package com.example.data.repository

import android.util.Log
import com.example.data.model.AdminRecommendation
import com.example.data.model.AuditLog
import com.example.data.model.ContentReport
import com.example.data.model.InvitationCode
import com.example.data.model.MembershipApplication
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreMappers.toAuditLogSafe
import com.example.data.repository.FirestoreMappers.toContentReportSafe
import com.example.data.repository.FirestoreMappers.toInvitationCodeSafe
import com.example.data.repository.FirestoreMappers.toMembershipApplicationSafe
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Calendar
import java.util.Date

class AdminRepository(private val firebase: FirebaseProvider) {
    private val db = firebase.db

    fun observeApplications(): Flow<List<MembershipApplication>> = callbackFlow {
        val listener = db.collection("applications")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "applications")
                    close(error)
                    return@addSnapshotListener
                }
                val list = (snap?.documents?.mapNotNull { it.toMembershipApplicationSafe() } ?: emptyList())
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) emit(emptyList()) else throw e
    }

    fun observeReports(): Flow<List<ContentReport>> = callbackFlow {
        val listener = db.collection("reports")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "reports")
                    close(error)
                    return@addSnapshotListener
                }
                val list = (snap?.documents?.mapNotNull { it.toContentReportSafe() } ?: emptyList())
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) emit(emptyList()) else throw e
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> = callbackFlow {
        val listener = db.collection("auditLogs")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "auditLogs")
                    close(error)
                    return@addSnapshotListener
                }
                val list = (snap?.documents?.mapNotNull { it.toAuditLogSafe() } ?: emptyList())
                    .sortedByDescending { it.createdAt }
                    .take(100)
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) {
            Log.w("AdminRepository", "AuditLogs listener error: ${cause.message}")
            emit(emptyList())
        } else {
            throw e
        }
    }

    fun observeInvitationCodes(): Flow<List<InvitationCode>> = callbackFlow {
        val listener = db.collection("invitationCodes")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "invitationCodes")
                    close(error)
                    return@addSnapshotListener
                }
                val list = (snap?.documents?.mapNotNull { it.toInvitationCodeSafe() } ?: emptyList())
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) emit(emptyList()) else throw e
    }

    suspend fun updateReportStatus(
        reportId: String,
        newStatus: String,
        notes: String
    ): Result<Unit> {
        return try {
            db.collection("reports").document(reportId).update(
                mapOf(
                    "status" to newStatus,
                    "moderatorNotes" to notes.ifBlank { "Reviewed by moderator (${DateUtils.formatShortDate(DateUtils.nowIsoString())})" }
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
            Result.failure(e)
        }
    }

    suspend fun addAdminRecommendation(
        app: MembershipApplication,
        adminId: String,
        adminName: String,
        recommendation: String,
        comment: String
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        val cleanComment = comment.trim().ifBlank {
            if (recommendation == "APPROVE") "Rider looks experienced and friendly!" else "More active ride logs recommended."
        }
        val newRec = AdminRecommendation(
            adminId = adminId,
            adminName = adminName,
            recommendation = recommendation,
            comment = cleanComment,
            createdAt = now
        )
        val merged = (app.adminRecommendations.filter { it.adminId != adminId } + newRec).map { r ->
            mapOf(
                "adminId" to r.adminId,
                "adminName" to r.adminName,
                "recommendation" to r.recommendation,
                "comment" to r.comment,
                "createdAt" to r.createdAt
            )
        }
        return try {
            db.collection("applications").document(app.id).update(
                mapOf(
                    "adminRecommendations" to merged,
                    "updatedAt" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "applications/${app.id}")
            Result.failure(e)
        }
    }

    suspend fun approveApplication(
        app: MembershipApplication,
        reviewerUid: String,
        reviewerEmail: String
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        return try {
            db.collection("applications").document(app.id).update(
                mapOf(
                    "status" to "APPROVED",
                    "reviewedBy" to reviewerUid,
                    "reviewedAt" to now,
                    "updatedAt" to now
                )
            ).await()

            db.collection("users").document(app.userId).update(
                mapOf(
                    "isAdminMember" to true,
                    "role" to "ADMIN",
                    "adminMembershipStatus" to "APPROVED",
                    "customTitle" to app.requestedTitle.ifBlank { "Admin Member" },
                    "isSuspended" to false,
                    "updatedAt" to now
                )
            ).await()

            db.collection("notifications").add(
                mapOf(
                    "recipientId" to app.userId,
                    "type" to "APPLICATION_APPROVED",
                    "title" to "Admin Membership Approved!",
                    "message" to "Congratulations! Your Admin Membership application was approved by the Super Admin. You are now an official Cycle.exe Admin Member.",
                    "isRead" to false,
                    "createdAt" to now
                )
            ).await()

            recordAuditLog(
                actorId = reviewerUid,
                actorEmail = reviewerEmail,
                action = "APPROVE_APPLICATION",
                targetType = "APPLICATION",
                targetId = app.id,
                details = "Approved Admin Membership for ${app.applicantName} (${app.applicantEmail})"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "applications/${app.id}")
            Result.failure(e)
        }
    }

    suspend fun rejectApplication(
        app: MembershipApplication,
        reason: String,
        reviewerUid: String,
        reviewerEmail: String
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        val finalReason = reason.trim().ifBlank { "Requirements not met at this time." }
        return try {
            db.collection("applications").document(app.id).update(
                mapOf(
                    "status" to "REJECTED",
                    "rejectionReason" to finalReason,
                    "reviewedBy" to reviewerUid,
                    "reviewedAt" to now,
                    "updatedAt" to now
                )
            ).await()

            db.collection("users").document(app.userId).update(
                mapOf(
                    "adminMembershipStatus" to "REJECTED",
                    "updatedAt" to now
                )
            ).await()

            db.collection("notifications").add(
                mapOf(
                    "recipientId" to app.userId,
                    "type" to "APPLICATION_REJECTED",
                    "title" to "Application Status Update",
                    "message" to "Your application for Admin Membership was not approved. Note: \"$finalReason\"",
                    "isRead" to false,
                    "createdAt" to now
                )
            ).await()

            recordAuditLog(
                actorId = reviewerUid,
                actorEmail = reviewerEmail,
                action = "REJECT_APPLICATION",
                targetType = "APPLICATION",
                targetId = app.id,
                details = "Rejected ${app.applicantName}: $finalReason"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "applications/${app.id}")
            Result.failure(e)
        }
    }

    suspend fun changeUserRole(
        targetUser: UserProfile,
        newRole: String,
        actorUid: String,
        actorEmail: String
    ): Result<Unit> {
        if (SuperAdminConfig.isSuperAdminEmail(targetUser.email)) {
            return Result.failure(IllegalStateException("A designated Super Admin account role cannot be changed."))
        }
        val now = DateUtils.nowIsoString()
        val becomingAdmin = newRole == "ADMIN"
        val leavingAdmin = !becomingAdmin && (
            targetUser.role == "ADMIN" ||
                targetUser.isAdminMember ||
                targetUser.adminMembershipStatus == "APPROVED"
            )

        val updates = mutableMapOf<String, Any>(
            "role" to newRole,
            "updatedAt" to now
        )
        if (becomingAdmin) {
            updates["isAdminMember"] = true
            updates["adminMembershipStatus"] = "APPROVED"
            updates["isSuspended"] = false
        } else if (leavingAdmin) {
            updates["isAdminMember"] = false
            updates["adminMembershipStatus"] = "NONE"
        }

        return try {
            db.collection("users").document(targetUser.id).update(updates).await()
            if (becomingAdmin || leavingAdmin) {
                try {
                    val appUpdates = mutableMapOf<String, Any>(
                        "status" to if (becomingAdmin) "APPROVED" else "REJECTED",
                        "updatedAt" to now
                    )
                    if (leavingAdmin) {
                        appUpdates["rejectionReason"] = "Admin membership removed by Super Admin. You may re-apply."
                    }
                    db.collection("applications").document(targetUser.id).update(appUpdates).await()
                } catch (_: Exception) {
                }
            }
            recordAuditLog(
                actorId = actorUid,
                actorEmail = actorEmail,
                action = "SET_ROLE_TO_$newRole",
                targetType = "USER",
                targetId = targetUser.id,
                details = "Changed role for ${targetUser.email} to $newRole"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${targetUser.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleUserSuspension(
        targetUser: UserProfile,
        actorUid: String,
        actorEmail: String
    ): Result<Unit> {
        if (SuperAdminConfig.isSuperAdminEmail(targetUser.email)) {
            return Result.failure(IllegalStateException("A designated Super Admin account cannot be suspended."))
        }
        val nextState = !targetUser.isSuspended
        val now = DateUtils.nowIsoString()
        return try {
            db.collection("users").document(targetUser.id).update(
                mapOf(
                    "isSuspended" to nextState,
                    "updatedAt" to now
                )
            ).await()
            recordAuditLog(
                actorId = actorUid,
                actorEmail = actorEmail,
                action = if (nextState) "SUSPEND_USER" else "REACTIVATE_USER",
                targetType = "USER",
                targetId = targetUser.id,
                details = "${if (nextState) "Suspended" else "Reactivated"} ${targetUser.email}"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${targetUser.id}")
            Result.failure(e)
        }
    }

    suspend fun removeFromAdminPanel(
        targetUserId: String,
        targetName: String,
        actorUid: String,
        actorEmail: String
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        return try {
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "isAdminMember" to false,
                    "adminMembershipStatus" to "NONE",
                    "role" to "PUBLIC",
                    "isSuspended" to false,
                    "updatedAt" to now
                )
            ).await()
            try {
                db.collection("applications").document(targetUserId).update(
                    mapOf(
                        "status" to "REJECTED",
                        "rejectionReason" to "Admin membership removed by Super Admin. You may re-apply.",
                        "updatedAt" to now
                    )
                ).await()
            } catch (_: Exception) {
            }
            try {
                db.collection("notifications").add(
                    mapOf(
                        "recipientId" to targetUserId,
                        "type" to "STATUS_CHANGED",
                        "title" to "Admin Membership Removed",
                        "message" to "Your Admin Member role has been removed by the Super Admin. You may re-apply whenever ready.",
                        "isRead" to false,
                        "createdAt" to now
                    )
                ).await()
            } catch (_: Exception) {
            }
            recordAuditLog(
                actorId = actorUid,
                actorEmail = actorEmail,
                action = "REMOVE_ADMIN_MEMBER",
                targetType = "USER",
                targetId = targetUserId,
                details = "Removed Admin privileges from $targetName"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUserId")
            Result.failure(e)
        }
    }

    suspend fun updateCustomTitle(
        targetUserId: String,
        newTitle: String,
        actorUid: String,
        actorEmail: String
    ): Result<Unit> {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Title cannot be empty"))
        return try {
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "customTitle" to clean,
                    "updatedAt" to DateUtils.nowIsoString()
                )
            ).await()
            recordAuditLog(
                actorId = actorUid,
                actorEmail = actorEmail,
                action = "UPDATE_CUSTOM_TITLE",
                targetType = "USER",
                targetId = targetUserId,
                details = "Updated title to \"$clean\""
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUserId")
            Result.failure(e)
        }
    }

    suspend fun generateInvitationCode(
        targetRole: String,
        actorUid: String,
        actorEmail: String
    ): Result<String> {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rnd = SecureRandom()
        val seg1 = (1..4).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        val seg2 = (1..4).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        val rawCode = "CYCLE-$targetRole-$seg1-$seg2"
        val masked = "CYCLE-$targetRole-****-$seg2"

        val digest = MessageDigest.getInstance("SHA-256").digest(rawCode.toByteArray())
        val hashHex = digest.joinToString("") { "%02x".format(it) }

        val cal = Calendar.getInstance().apply {
            time = Date()
            add(Calendar.DAY_OF_YEAR, 30)
        }
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val expiresAt = sdf.format(cal.time)
        val now = DateUtils.nowIsoString()

        val payload = mapOf(
            "codeHash" to hashHex,
            "displayCodeMasked" to masked,
            "targetRole" to targetRole,
            "isRedeemed" to false,
            "redeemedByUserId" to "",
            "redeemedByEmail" to "",
            "expiresAt" to expiresAt,
            "isRevoked" to false,
            "createdBy" to actorUid,
            "createdAt" to now
        )
        return try {
            db.collection("invitationCodes").add(payload).await()
            recordAuditLog(
                actorId = actorUid,
                actorEmail = actorEmail,
                action = "GENERATE_INVITATION_CODE",
                targetType = "INVITATION",
                targetId = masked,
                details = "Generated $targetRole invitation code ($masked)"
            )
            Result.success(rawCode)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "invitationCodes")
            Result.failure(e)
        }
    }

    suspend fun revokeInvitationCode(
        codeId: String,
        actorUid: String,
        actorEmail: String
    ): Result<Unit> {
        return try {
            db.collection("invitationCodes").document(codeId).update(
                mapOf("isRevoked" to true)
            ).await()
            recordAuditLog(
                actorId = actorUid,
                actorEmail = actorEmail,
                action = "REVOKE_INVITATION_CODE",
                targetType = "INVITATION",
                targetId = codeId,
                details = "Revoked invitation code $codeId"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "invitationCodes/$codeId")
            Result.failure(e)
        }
    }

    suspend fun recordAuditLog(
        actorId: String,
        actorEmail: String,
        action: String,
        targetType: String,
        targetId: String,
        details: String
    ) {
        try {
            db.collection("auditLogs").add(
                mapOf(
                    "actorId" to actorId,
                    "actorEmail" to actorEmail,
                    "action" to action,
                    "targetType" to targetType,
                    "targetId" to targetId,
                    "details" to details,
                    "createdAt" to DateUtils.nowIsoString()
                )
            ).await()
        } catch (_: Exception) {
        }
    }
}
