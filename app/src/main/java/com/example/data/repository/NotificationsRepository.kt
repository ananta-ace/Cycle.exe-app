package com.example.data.repository

import android.util.Log
import com.example.data.model.AppNotification
import com.example.data.model.NoticeReply
import com.example.data.model.UserProfile
import com.example.data.repository.FirestoreMappers.toAppNotificationSafe
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NotificationsRepository(private val firebase: FirebaseProvider) {
    private val db = firebase.db

    fun observeAllNotifications(): Flow<List<AppNotification>> = callbackFlow {
        val listener = db.collection("notifications")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "notifications")
                    close(error)
                    return@addSnapshotListener
                }
                val list = (snap?.documents?.mapNotNull { it.toAppNotificationSafe() } ?: emptyList())
                    .sortedByDescending { it.createdAt }
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) {
            Log.w("NotificationsRepo", "Notifications listener error: ${cause.message}")
            emit(emptyList())
        } else {
            throw e
        }
    }

    fun filterForUser(
        all: List<AppNotification>,
        uid: String,
        isSuperAdmin: Boolean,
        isAdmin: Boolean
    ): List<AppNotification> {
        return all.filter { n ->
            n.recipientId == uid ||
                (isSuperAdmin && (n.recipientId == "SUPER_ADMIN" || n.recipientId == "ALL_ADMINS" || n.recipientId == "ADMINS")) ||
                (isAdmin && (n.recipientId == "ALL_ADMINS" || n.recipientId == "ADMINS"))
        }
    }

    suspend fun markAsRead(notif: AppNotification): Result<Unit> {
        if (notif.isRead) return Result.success(Unit)
        val now = DateUtils.nowIsoString()
        return try {
            db.collection("notifications").document(notif.id).update(
                mapOf(
                    "isRead" to true,
                    "readAt" to now,
                    "deliveryStatus" to "READ",
                    "updatedAt" to now
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/${notif.id}")
            Result.failure(e)
        }
    }

    suspend fun markAllRead(notifications: List<AppNotification>): Result<Unit> {
        val unread = notifications.filter { !it.isRead }
        if (unread.isEmpty()) return Result.success(Unit)
        val now = DateUtils.nowIsoString()
        return try {
            for (n in unread) {
                db.collection("notifications").document(n.id).update(
                    mapOf(
                        "isRead" to true,
                        "readAt" to now,
                        "deliveryStatus" to "READ",
                        "updatedAt" to now
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notifId: String): Result<Unit> {
        return try {
            db.collection("notifications").document(notifId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "notifications/$notifId")
            Result.failure(e)
        }
    }

    suspend fun sendDirectNotice(
        recipient: UserProfile,
        senderId: String,
        senderName: String,
        senderEmail: String,
        title: String,
        message: String,
        isUrgent: Boolean
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        val noticeId = "notice_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}"
        val payload = hashMapOf<String, Any?>(
            "id" to noticeId,
            "recipientId" to recipient.id,
            "recipientName" to recipient.displayName.ifBlank { "Rider" },
            "recipientEmail" to recipient.email,
            "senderId" to senderId,
            "senderName" to senderName.ifBlank { "Super Admin (Owner)" },
            "senderEmail" to senderEmail.ifBlank { SuperAdminConfig.DESIGNATED_EMAIL },
            "senderRole" to "SUPER_ADMIN",
            "type" to "DIRECT_NOTICE",
            "isDirectNotice" to true,
            "title" to title.trim().ifBlank { "Official Notice from Super Admin" }.take(120),
            "message" to message.trim().take(2000),
            "isUrgent" to isUrgent,
            "deliveryStatus" to "DELIVERED",
            "deliveredAt" to now,
            "isRead" to false,
            "readAt" to null,
            "replies" to emptyList<Map<String, Any>>(),
            "replyCount" to 0L,
            "createdAt" to now,
            "updatedAt" to now
        )
        return try {
            db.collection("notifications").document(noticeId).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "notifications/$noticeId")
            Result.failure(e)
        }
    }

    suspend fun addReplyToNotice(
        notice: AppNotification,
        senderId: String,
        senderName: String,
        senderEmail: String,
        senderRole: String,
        senderPhotoURL: String,
        text: String,
        isSuperAdmin: Boolean
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Reply cannot be empty"))

        val replyItem = NoticeReply(
            id = "rep_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
            senderId = senderId,
            senderName = senderName,
            senderEmail = senderEmail,
            senderRole = if (isSuperAdmin) "SUPER_ADMIN" else senderRole,
            senderPhotoURL = senderPhotoURL,
            text = cleanText,
            createdAt = now
        )
        val updatedReplies = (notice.replies + replyItem).map { r ->
            mapOf(
                "id" to r.id,
                "senderId" to r.senderId,
                "senderName" to r.senderName,
                "senderEmail" to r.senderEmail,
                "senderRole" to r.senderRole,
                "senderPhotoURL" to (r.senderPhotoURL ?: ""),
                "text" to r.text,
                "createdAt" to r.createdAt
            )
        }

        return try {
            db.collection("notifications").document(notice.id).update(
                mapOf(
                    "replies" to updatedReplies,
                    "replyCount" to updatedReplies.size.toLong(),
                    "lastRepliedAt" to now,
                    "hasNewReply" to !isSuperAdmin,
                    "updatedAt" to now
                )
            ).await()

            // Ping recipient or Super Admin
            if (isSuperAdmin && notice.recipientId.isNotBlank() && notice.recipientId != senderId) {
                val pingId = "notif_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
                db.collection("notifications").document(pingId).set(
                    mapOf(
                        "id" to pingId,
                        "recipientId" to notice.recipientId,
                        "type" to "DIRECT_NOTICE",
                        "isDirectNotice" to true,
                        "title" to "Reply from Super Admin: ${notice.title}",
                        "message" to cleanText,
                        "isRead" to false,
                        "createdAt" to now
                    )
                ).await()
            } else if (!isSuperAdmin) {
                db.collection("notifications").add(
                    mapOf(
                        "recipientId" to "SUPER_ADMIN",
                        "type" to "SYSTEM",
                        "title" to "Reply from $senderName",
                        "message" to "Re: \"${notice.title}\": ${cleanText.take(140)}",
                        "link" to notice.id,
                        "isRead" to false,
                        "createdAt" to now
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/${notice.id}")
            Result.failure(e)
        }
    }
}
