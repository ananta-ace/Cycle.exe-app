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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
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

    suspend fun getExistingRating(targetMemberId: String, raterId: String): MemberRating? {
        return try {
            val featSnap = db.collection("featuredMembers").document(targetMemberId)
                .collection("ratings").document(raterId).get().await()
            if (featSnap.exists()) {
                featSnap.toMemberRatingSafe()
            } else {
                val userSnap = db.collection("users").document(targetMemberId)
                    .collection("ratings").document(raterId).get().await()
                if (userSnap.exists()) userSnap.toMemberRatingSafe() else null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun submitRating(
        targetMember: FeaturedMember,
        raterId: String,
        raterName: String,
        rating: Int,
        comment: String,
        existingCreatedAt: String?
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        val payload = mapOf(
            "id" to raterId,
            "targetMemberId" to targetMember.id,
            "raterId" to raterId,
            "raterName" to raterName,
            "rating" to rating.coerceIn(1, 5),
            "comment" to comment.trim().take(300),
            "createdAt" to (existingCreatedAt ?: now),
            "updatedAt" to now
        )
        return try {
            db.collection("featuredMembers").document(targetMember.id)
                .collection("ratings").document(raterId).set(payload).await()
            try {
                db.collection("users").document(targetMember.id)
                    .collection("ratings").document(raterId).set(payload).await()
            } catch (_: Exception) {
            }
            recalculateRatingAggregate(targetMember)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "featuredMembers/${targetMember.id}/ratings/$raterId")
            Result.failure(e)
        }
    }

    suspend fun deleteRating(targetMember: FeaturedMember, raterId: String): Result<Unit> {
        return try {
            db.collection("featuredMembers").document(targetMember.id)
                .collection("ratings").document(raterId).delete().await()
            try {
                db.collection("users").document(targetMember.id)
                    .collection("ratings").document(raterId).delete().await()
            } catch (_: Exception) {
            }
            recalculateRatingAggregate(targetMember)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "featuredMembers/${targetMember.id}/ratings/$raterId")
            Result.failure(e)
        }
    }

    private suspend fun recalculateRatingAggregate(targetMember: FeaturedMember) {
        try {
            val snap = db.collection("featuredMembers").document(targetMember.id)
                .collection("ratings").get().await()
            var sum = 0
            var count = 0L
            for (d in snap.documents) {
                val r = FirestoreMappers.anyToInt(d.get("rating"), 0)
                if (r > 0) {
                    sum += r
                    count++
                }
            }
            val avg = if (count > 0) ((sum.toDouble() / count.toDouble()) * 10.0).roundToInt() / 10.0 else 0.0
            val now = DateUtils.nowIsoString()
            val aggMap = mapOf(
                "ratingAverage" to avg,
                "ratingCount" to count,
                "updatedAt" to now
            )
            try {
                db.collection("featuredMembers").document(targetMember.id)
                    .set(aggMap, SetOptions.merge()).await()
            } catch (_: Exception) {
            }
            val linkedUserId = targetMember.userId.ifBlank { targetMember.id }
            try {
                db.collection("users").document(linkedUserId)
                    .set(aggMap, SetOptions.merge()).await()
            } catch (_: Exception) {
            }
        } catch (_: Exception) {
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
