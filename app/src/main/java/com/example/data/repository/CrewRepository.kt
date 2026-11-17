package com.example.data.repository

import android.util.Log
import com.example.data.model.FeaturedMember
import com.example.data.model.MemberRating
import com.example.data.model.MembershipApplication
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreMappers.toFeaturedMemberSafe
import com.example.data.repository.FirestoreMappers.toMemberRatingSafe
import com.example.data.repository.FirestoreMappers.toMembershipApplicationSafe
import com.example.data.repository.FirestoreMappers.toUserProfileSafe
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

class CrewRepository(private val firebase: FirebaseProvider) {
    private val db = firebase.db

    private fun sortFeatured(items: List<FeaturedMember>): List<FeaturedMember> {
        return items.sortedWith(
            compareBy<FeaturedMember>(
                { it.order },
                { it.createdAt.ifBlank { "~" } },
                { it.name.lowercase() }
            )
        )
    }

    private fun sortUsers(items: List<UserProfile>): List<UserProfile> {
        return items.sortedByDescending { it.createdAt }
    }

    fun observeFeaturedMembersState(): Flow<SyncState<FeaturedMember>> = callbackFlow {
        // Do NOT use .orderBy("order") on Firestore query: documents without an `order` field would be excluded silently!
        val listener = db.collection("featuredMembers")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "featuredMembers")
                    close(error)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toFeaturedMemberSafe() } ?: emptyList()
                val sorted = sortFeatured(list)
                val fromCache = snap?.metadata?.isFromCache == true
                val stillWaitingServer = fromCache && sorted.isEmpty()
                trySend(
                    SyncState(
                        items = sorted,
                        isLoading = stillWaitingServer,
                        error = null,
                        isFromCache = fromCache
                    )
                )
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) {
            Log.w("CrewRepository", "FeaturedMembers listener error: ${cause.message}", cause)
            emit(
                SyncState(
                    items = emptyList(),
                    isLoading = false,
                    error = cause.localizedMessage ?: "Unable to sync crew directory from server."
                )
            )
        } else {
            throw e
        }
    }

    fun observeFeaturedMembers(): Flow<List<FeaturedMember>> =
        observeFeaturedMembersState().map { it.items }

    suspend fun fetchFeaturedMembersFromServer(): Result<List<FeaturedMember>> {
        return try {
            val snap = db.collection("featuredMembers").get(Source.SERVER).await()
            val list = snap.documents.mapNotNull { it.toFeaturedMemberSafe() }
            Result.success(sortFeatured(list))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "featuredMembers")
            Result.failure(e)
        }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = callbackFlow {
        val listener = db.collection("users")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "users")
                    close(error)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toUserProfileSafe() } ?: emptyList()
                trySend(sortUsers(list))
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) {
            emit(emptyList())
        } else {
            throw e
        }
    }

    data class RatingSummary(
        val ratingAverage: Double,
        val ratingCount: Long
    )

    data class RatingWriteResult(
        val summary: RatingSummary,
        val savedRating: MemberRating
    )

    fun observeMemberRatings(targetMemberId: String): Flow<List<MemberRating>> = callbackFlow {
        if (targetMemberId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("featuredMembers")
            .document(targetMemberId)
            .collection("ratings")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "featuredMembers/$targetMemberId/ratings")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents
                    ?.mapNotNull { it.toMemberRatingSafe() }
                    ?.sortedByDescending { it.updatedAt.ifBlank { it.createdAt } }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) {
            emit(emptyList())
        } else {
            throw e
        }
    }

    suspend fun getExistingRating(targetMember: FeaturedMember, raterId: String): MemberRating? {
        if (targetMember.id.isBlank() || raterId.isBlank()) return null

        suspend fun readDocFast(collection: String, docId: String, timeoutMs: Long): MemberRating? {
            if (docId.isBlank()) return null
            val ref = db.collection(collection).document(docId)
                .collection("ratings").document(raterId)
            val serverSnap = try {
                withTimeoutOrNull(timeoutMs) { ref.get(Source.SERVER).await() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (serverSnap != null) {
                return if (serverSnap.exists()) serverSnap.toMemberRatingSafe() else null
            }
            val defaultSnap = try {
                withTimeoutOrNull(1200L) { ref.get().await() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            return if (defaultSnap != null && defaultSnap.exists()) defaultSnap.toMemberRatingSafe() else null
        }

        // Check featuredMembers/{id}/ratings/{raterId} first
        readDocFast("featuredMembers", targetMember.id, 2500L)?.let { return it }

        // Check fallback paths in parallel with a tight bound
        return coroutineScope {
            val userByIdDeferred = async { readDocFast("users", targetMember.id, 1800L) }
            val userByUidDeferred = if (targetMember.userId.isNotBlank() && targetMember.userId != targetMember.id) {
                async { readDocFast("users", targetMember.userId, 1800L) }
            } else {
                null
            }
            userByIdDeferred.await() ?: userByUidDeferred?.await()
        }
    }

    suspend fun submitRating(
        targetMember: FeaturedMember,
        raterId: String,
        raterName: String,
        rating: Int,
        comment: String,
        existingRating: MemberRating?
    ): Result<RatingWriteResult> {
        if (raterId.isBlank()) {
            return Result.failure(IllegalStateException("You must be signed in to rate a crew leader."))
        }
        if (raterId == targetMember.id || (targetMember.userId.isNotBlank() && raterId == targetMember.userId)) {
            return Result.failure(IllegalArgumentException("You cannot rate yourself."))
        }
        val safeRating = rating.coerceIn(1, 5)
        val cleanComment = comment.trim().take(300)
        val cleanRaterName = raterName.trim().ifBlank { "Rider" }
        val now = DateUtils.nowIsoString()
        val createdAt = existingRating?.createdAt?.ifBlank { null } ?: now
        val payload = mapOf(
            "id" to raterId,
            "targetMemberId" to targetMember.id,
            "raterId" to raterId,
            "raterName" to cleanRaterName,
            "rating" to safeRating,
            "comment" to cleanComment,
            "createdAt" to createdAt,
            "updatedAt" to now
        )
        return try {
            db.collection("featuredMembers").document(targetMember.id)
                .collection("ratings").document(raterId).set(payload).await()

            val userTargetIds = linkedSetOf(targetMember.id).apply {
                if (targetMember.userId.isNotBlank()) add(targetMember.userId)
            }
            for (uidTarget in userTargetIds) {
                if (uidTarget != raterId) {
                    try {
                        db.collection("users").document(uidTarget)
                            .collection("ratings").document(raterId).set(payload).await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    }
                }
            }

            val summary = recalculateRatingAggregate(
                targetMember = targetMember,
                overrideRaterId = raterId,
                overrideRating = safeRating,
                previousRating = existingRating?.rating
            )
            val saved = MemberRating(
                id = raterId,
                targetMemberId = targetMember.id,
                raterId = raterId,
                raterName = cleanRaterName,
                rating = safeRating,
                comment = cleanComment,
                createdAt = createdAt,
                updatedAt = now
            )
            Result.success(RatingWriteResult(summary = summary, savedRating = saved))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "featuredMembers/${targetMember.id}/ratings/$raterId")
            Result.failure(e)
        }
    }

    suspend fun deleteRating(
        targetMember: FeaturedMember,
        raterId: String,
        previousRating: Int? = null
    ): Result<RatingSummary> {
        if (raterId.isBlank()) {
            return Result.failure(IllegalStateException("You must be signed in to remove your rating."))
        }
        return try {
            db.collection("featuredMembers").document(targetMember.id)
                .collection("ratings").document(raterId).delete().await()

            val userTargetIds = linkedSetOf(targetMember.id).apply {
                if (targetMember.userId.isNotBlank()) add(targetMember.userId)
            }
            for (uidTarget in userTargetIds) {
                if (uidTarget != raterId) {
                    try {
                        db.collection("users").document(uidTarget)
                            .collection("ratings").document(raterId).delete().await()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                    }
                }
            }

            val summary = recalculateRatingAggregate(
                targetMember = targetMember,
                overrideRaterId = raterId,
                overrideRating = null,
                previousRating = previousRating
            )
            Result.success(summary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "featuredMembers/${targetMember.id}/ratings/$raterId")
            Result.failure(e)
        }
    }

    private suspend fun recalculateRatingAggregate(
        targetMember: FeaturedMember,
        overrideRaterId: String,
        overrideRating: Int?,
        previousRating: Int?
    ): RatingSummary {
        val ratingsRef = db.collection("featuredMembers").document(targetMember.id)
            .collection("ratings")

        val snap = try {
            withTimeoutOrNull(3500L) { ratingsRef.get(Source.SERVER).await() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: try {
            withTimeoutOrNull(1500L) { ratingsRef.get().await() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        val summary = if (snap != null) {
            val ratingsByRater = linkedMapOf<String, Int>()
            for (d in snap.documents) {
                val r = FirestoreMappers.anyToInt(d.get("rating"), 0)
                if (r in 1..5) {
                    ratingsByRater[d.id] = r
                }
            }
            if (overrideRating != null) {
                ratingsByRater[overrideRaterId] = overrideRating.coerceIn(1, 5)
            } else {
                ratingsByRater.remove(overrideRaterId)
            }
            computeSummaryFromRatings(ratingsByRater.values)
        } else {
            computeIncrementalSummary(
                currentAverage = targetMember.ratingAverage,
                currentCount = targetMember.ratingCount,
                previousRating = previousRating,
                newRating = overrideRating
            )
        }

        val now = DateUtils.nowIsoString()
        val aggMap = mapOf(
            "ratingAverage" to summary.ratingAverage,
            "ratingCount" to summary.ratingCount,
            "updatedAt" to now
        )
        val featRef = db.collection("featuredMembers").document(targetMember.id)
        try {
            featRef.update(aggMap).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if ((e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                try {
                    featRef.set(aggMap, SetOptions.merge()).await()
                } catch (mergeEx: Exception) {
                    handleFirestoreError(mergeEx, OperationType.UPDATE, "featuredMembers/${targetMember.id}")
                }
            } else {
                handleFirestoreError(e, OperationType.UPDATE, "featuredMembers/${targetMember.id}")
                Log.w("CrewRepository", "Update featured member ratings warning: ${e.message}")
            }
        }

        val linkedUserId = targetMember.userId.ifBlank { targetMember.id }
        val userRef = db.collection("users").document(linkedUserId)
        try {
            userRef.update(aggMap).await()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        return summary
    }

    companion object {
        internal fun computeSummaryFromRatings(ratings: Collection<Int>): RatingSummary {
            var sum = 0
            var count = 0L
            for (r in ratings) {
                if (r in 1..5) {
                    sum += r
                    count++
                }
            }
            val avg = if (count > 0L) {
                ((sum.toDouble() / count.toDouble()) * 10.0).roundToInt() / 10.0
            } else {
                0.0
            }
            return RatingSummary(ratingAverage = avg, ratingCount = count)
        }

        internal fun computeIncrementalSummary(
            currentAverage: Double,
            currentCount: Long,
            previousRating: Int?,
            newRating: Int?
        ): RatingSummary {
            val safeAvg = if (currentAverage > 0.0) currentAverage else 0.0
            val safeCount = currentCount.coerceAtLeast(0L)
            val baseSum = safeAvg * safeCount.toDouble()

            return if (newRating != null) {
                val clampedNew = newRating.coerceIn(1, 5)
                if (previousRating != null && previousRating in 1..5 && safeCount > 0L) {
                    val newSum = (baseSum - previousRating + clampedNew).coerceAtLeast(clampedNew.toDouble())
                    val avg = ((newSum / safeCount.toDouble()) * 10.0).roundToInt() / 10.0
                    RatingSummary(ratingAverage = avg, ratingCount = safeCount)
                } else {
                    val nextCount = safeCount + 1L
                    val newSum = baseSum + clampedNew
                    val avg = ((newSum / nextCount.toDouble()) * 10.0).roundToInt() / 10.0
                    RatingSummary(ratingAverage = avg, ratingCount = nextCount)
                }
            } else {
                if (safeCount <= 1L) {
                    RatingSummary(ratingAverage = 0.0, ratingCount = 0L)
                } else {
                    val removed = (previousRating ?: safeAvg.roundToInt()).coerceIn(1, 5)
                    val nextCount = (safeCount - 1L).coerceAtLeast(0L)
                    val newSum = (baseSum - removed).coerceAtLeast(0.0)
                    val avg = if (nextCount > 0L) {
                        ((newSum / nextCount.toDouble()) * 10.0).roundToInt() / 10.0
                    } else {
                        0.0
                    }
                    RatingSummary(ratingAverage = avg, ratingCount = nextCount)
                }
            }
        }
    }

    suspend fun getMyApplication(uid: String): MembershipApplication? {
        return try {
            val snap = db.collection("applications").document(uid).get().await()
            if (snap.exists()) snap.toMembershipApplicationSafe() else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun submitApplication(
        uid: String,
        applicantName: String,
        applicantEmail: String,
        applicantPhotoURL: String,
        reason: String,
        cyclingExperience: String,
        requestedTitle: String,
        previousApp: MembershipApplication?
    ): Result<MembershipApplication> {
        val now = DateUtils.nowIsoString()
        val createdAt = previousApp?.createdAt?.ifBlank { now } ?: now
        val payload = mapOf(
            "id" to uid,
            "userId" to uid,
            "applicantName" to applicantName,
            "applicantEmail" to applicantEmail,
            "applicantPhotoURL" to applicantPhotoURL,
            "reason" to reason.trim(),
            "cyclingExperience" to cyclingExperience.trim(),
            "requestedTitle" to requestedTitle.trim().ifBlank { "Cycling Enthusiast" },
            "status" to "PENDING",
            "createdAt" to createdAt,
            "updatedAt" to now
        )
        return try {
            db.collection("applications").document(uid).set(payload).await()
            try {
                val isResubmit = previousApp?.status == "REJECTED"
                db.collection("notifications").add(
                    mapOf(
                        "recipientId" to "SUPER_ADMIN",
                        "type" to "APPLICATION_SUBMITTED",
                        "title" to if (isResubmit) "Admin Membership Application Re-Submitted" else "New Admin Member Application",
                        "message" to "$applicantName ${if (isResubmit) "re-submitted" else "submitted"} an application for Admin Membership.",
                        "applicantId" to uid,
                        "applicantName" to applicantName,
                        "applicantPhotoURL" to applicantPhotoURL,
                        "isRead" to false,
                        "createdAt" to now
                    )
                ).await()
            } catch (_: Exception) {
            }
            try {
                db.collection("users").document(uid)
                    .set(mapOf("adminMembershipStatus" to "PENDING", "updatedAt" to now), SetOptions.merge())
                    .await()
            } catch (_: Exception) {
            }
            Result.success(
                MembershipApplication(
                    id = uid,
                    userId = uid,
                    applicantName = applicantName,
                    applicantEmail = applicantEmail,
                    applicantPhotoURL = applicantPhotoURL,
                    reason = reason.trim(),
                    cyclingExperience = cyclingExperience.trim(),
                    requestedTitle = requestedTitle.trim().ifBlank { "Cycling Enthusiast" },
                    status = "PENDING",
                    createdAt = createdAt,
                    updatedAt = now
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "applications/$uid")
            Result.failure(e)
        }
    }

    suspend fun saveFeaturedMember(
        existingId: String?,
        userId: String,
        name: String,
        title: String,
        img: String,
        active: Boolean,
        attend: String,
        perfect: Boolean,
        bio: String,
        badges: List<String>,
        completedRaces: List<String>,
        order: Long
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        val cleanName = name.trim()
        val cleanBadges = badges.map { it.trim() }.filter { it.isNotEmpty() }
        val cleanRaces = completedRaces.map { it.trim() }.filter { it.isNotEmpty() }

        val payload = hashMapOf<String, Any>(
            "userId" to userId,
            "name" to cleanName,
            "title" to title.trim().ifBlank { "Cycle.exe Crew Leader" },
            "img" to img.trim(),
            "initial" to (cleanName.firstOrNull()?.uppercaseChar()?.toString() ?: "C"),
            "active" to active,
            "attend" to attend.trim().ifBlank { "Active Rider" },
            "perfect" to perfect,
            "bio" to bio.trim(),
            "badges" to cleanBadges,
            "completedRaces" to cleanRaces,
            "completedRacesCount" to cleanRaces.size.toLong(),
            "order" to order,
            "isCustomMember" to true,
            "updatedAt" to now
        )
        return try {
            if (!existingId.isNullOrBlank()) {
                db.collection("featuredMembers").document(existingId)
                    .set(payload, SetOptions.merge()).await()
            } else {
                payload["ratingAverage"] = 0.0
                payload["ratingCount"] = 0L
                payload["createdAt"] = now
                db.collection("featuredMembers").add(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "featuredMembers")
            Result.failure(e)
        }
    }

    suspend fun featureRegisteredUser(user: UserProfile, order: Long): Result<FeaturedMember> {
        val now = DateUtils.nowIsoString()
        val defaultTitle = user.customTitle?.ifBlank { null } ?: when (user.role) {
            "SUPER_ADMIN" -> "Executive Lead"
            "ADMIN" -> "Admin Crew Leader"
            else -> "Admin Member"
        }
        val initial = user.displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "R"
        val badges = listOf("Verified Rider", "Cycle.exe Crew")
        val races = listOf("Mission 001: Midnight Sprint")
        val payload = mapOf(
            "userId" to user.id,
            "name" to user.displayName.ifBlank { "Rider" },
            "title" to defaultTitle,
            "img" to (user.photoURL ?: ""),
            "initial" to initial,
            "active" to !user.isSuspended,
            "attend" to "Active Rider",
            "perfect" to false,
            "bio" to (user.bio ?: ""),
            "ratingAverage" to user.ratingAverage,
            "ratingCount" to user.ratingCount,
            "badges" to badges,
            "completedRaces" to races,
            "completedRacesCount" to 1L,
            "order" to order,
            "isCustomMember" to true,
            "createdAt" to now,
            "updatedAt" to now
        )
        return try {
            db.collection("featuredMembers").document(user.id).set(payload).await()
            Result.success(
                FeaturedMember(
                    id = user.id,
                    userId = user.id,
                    name = user.displayName.ifBlank { "Rider" },
                    title = defaultTitle,
                    img = user.photoURL.orEmpty(),
                    initial = initial,
                    active = !user.isSuspended,
                    attend = "Active Rider",
                    perfect = false,
                    bio = user.bio.orEmpty(),
                    ratingAverage = user.ratingAverage,
                    ratingCount = user.ratingCount,
                    badges = badges,
                    completedRaces = races,
                    completedRacesCount = 1L,
                    order = order,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "featuredMembers/${user.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteFeaturedMember(memberId: String): Result<Unit> {
        return try {
            db.collection("featuredMembers").document(memberId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "featuredMembers/$memberId")
            Result.failure(e)
        }
    }
}
