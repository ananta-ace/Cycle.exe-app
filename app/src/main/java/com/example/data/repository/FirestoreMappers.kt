package com.example.data.repository

import com.example.data.model.AdminRecommendation
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.ContentReport
import com.example.data.model.FeaturedMember
import com.example.data.model.GalleryPhoto
import com.example.data.model.InvitationCode
import com.example.data.model.MemberRating
import com.example.data.model.MembershipApplication
import com.example.data.model.MissionReport
import com.example.data.model.MissionSegment
import com.example.data.model.NoticeReply
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SyncState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isFromCache: Boolean = false
)

object FirestoreMappers {

    private fun formatUtcIso(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    fun anyToString(value: Any?, default: String = ""): String {
        return when (value) {
            null -> default
            is String -> value
            is Timestamp -> formatUtcIso(value.toDate())
            is Date -> formatUtcIso(value)
            is Number, is Boolean -> value.toString()
            else -> default
        }
    }

    fun anyToNullableString(value: Any?): String? {
        val s = anyToString(value, "").trim()
        return s.ifEmpty { null }
    }

    fun anyToBool(value: Any?, default: Boolean = false): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> when (value.trim().lowercase(Locale.US)) {
                "true", "1", "yes" -> true
                "false", "0", "no" -> false
                else -> default
            }
            else -> default
        }
    }

    fun anyToLong(value: Any?, default: Long = 0L): Long {
        return when (value) {
            is Number -> value.toLong()
            is String -> value.trim().toDoubleOrNull()?.toLong() ?: default
            else -> default
        }
    }

    fun anyToInt(value: Any?, default: Int = 0): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.trim().toDoubleOrNull()?.toInt() ?: default
            else -> default
        }
    }

    fun anyToDouble(value: Any?, default: Double = 0.0): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.trim().toDoubleOrNull() ?: default
            else -> default
        }
    }

    fun anyToStringList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.mapNotNull { item ->
                anyToString(item).trim().ifBlank { null }
            }
            is String -> value.split(",").mapNotNull { it.trim().ifBlank { null } }
            else -> emptyList()
        }
    }

    fun DocumentSnapshot.toMissionReportSafe(): MissionReport? {
        val data = data ?: return null
        val rawSegments = data["segments"] as? List<*> ?: emptyList<Any?>()
        val parsedSegments = rawSegments.mapIndexedNotNull { idx, raw ->
            val m = raw as? Map<*, *> ?: return@mapIndexedNotNull null
            MissionSegment(
                num = anyToInt(m["num"], idx + 1).let { if (it > 0) it else idx + 1 },
                from = anyToString(m["from"]),
                to = anyToString(m["to"]),
                distance = anyToString(m["distance"]),
                segment = anyToString(m["segment"])
            )
        }
        return MissionReport(
            id = id,
            num = anyToString(data["num"]),
            tag = anyToString(data["tag"]),
            name = anyToString(data["name"]),
            title = anyToString(data["title"]),
            description = anyToString(data["description"]),
            date = anyToString(data["date"]),
            startTime = anyToString(data["startTime"]),
            endTime = anyToString(data["endTime"]),
            time = anyToString(data["time"]),
            fullDate = anyToString(data["fullDate"]),
            shortDate = anyToString(data["shortDate"]),
            duration = anyToString(data["duration"]),
            avgSpeed = anyToString(data["avgSpeed"]),
            totalDist = anyToString(data["totalDist"]),
            startLocation = anyToString(data["startLocation"]),
            destination = anyToString(data["destination"]),
            location = anyToString(data["location"]),
            segments = parsedSegments,
            crewIds = anyToStringList(data["crewIds"]),
            crewNote = anyToString(data["crewNote"]),
            notes = anyToString(data["notes"]),
            disclaimer = anyToString(data["disclaimer"]),
            icon = anyToString(data["icon"], "fa-map-signs"),
            variant = anyToString(data["variant"], "default"),
            customColor = anyToNullableString(data["customColor"]),
            isDuo = anyToBool(data["isDuo"], false),
            status = anyToString(data["status"], "PUBLISHED"),
            createdAt = anyToString(data["createdAt"]),
            updatedAt = anyToString(data["updatedAt"])
        )
    }

    fun DocumentSnapshot.toFeaturedMemberSafe(): FeaturedMember? {
        val data = data ?: return null
        val name = anyToString(data["name"]).ifBlank { "Featured Rider" }
        val completedRaces = anyToStringList(data["completedRaces"])
        val raceCount = anyToLong(data["completedRacesCount"], completedRaces.size.toLong())
        return FeaturedMember(
            id = id,
            userId = anyToString(data["userId"], id).ifBlank { id },
            name = name,
            title = anyToString(data["title"], "Cycle.exe Crew Leader").ifBlank { "Cycle.exe Crew Leader" },
            img = anyToString(data["img"]),
            initial = anyToString(data["initial"]).ifBlank { name.firstOrNull()?.uppercaseChar()?.toString() ?: "C" },
            active = if (data.containsKey("active")) anyToBool(data["active"], true) else true,
            attend = anyToString(data["attend"], "Active Rider").ifBlank { "Active Rider" },
            perfect = anyToBool(data["perfect"], false),
            bio = anyToString(data["bio"]),
            ratingAverage = anyToDouble(data["ratingAverage"], 0.0),
            ratingCount = anyToLong(data["ratingCount"], 0L),
            badges = anyToStringList(data["badges"]),
            completedRaces = completedRaces,
            completedRacesCount = raceCount,
            order = anyToLong(data["order"], 0L),
            createdAt = anyToString(data["createdAt"]),
            updatedAt = anyToString(data["updatedAt"])
        )
    }

    fun DocumentSnapshot.toGalleryPhotoSafe(): GalleryPhoto? {
        val data = data ?: return null
        val photoUrl = anyToString(data["photoURL"])
        if (photoUrl.isBlank()) return null
        return GalleryPhoto(
            id = id,
            title = anyToString(data["title"]),
            description = anyToNullableString(data["description"]),
            photoURL = photoUrl,
            uploadedBy = anyToString(data["uploadedBy"]),
            uploadedByName = anyToString(data["uploadedByName"]),
            uploadedAt = anyToString(data["uploadedAt"])
        )
    }

    fun DocumentSnapshot.toPostSafe(): Post? {
        val data = data ?: return null
        return Post(
            id = id,
            authorId = anyToString(data["authorId"]),
            authorName = anyToString(data["authorName"], "Cyclist").ifBlank { "Cyclist" },
            authorPhotoURL = anyToNullableString(data["authorPhotoURL"]),
            authorTitle = anyToString(data["authorTitle"]),
            content = anyToString(data["content"]),
            photoURLs = anyToStringList(data["photoURLs"]),
            likesCount = anyToLong(data["likesCount"], 0L),
            commentsCount = anyToLong(data["commentsCount"], 0L),
            isHidden = anyToBool(data["isHidden"], false),
            createdAt = anyToString(data["createdAt"]),
            updatedAt = anyToString(data["updatedAt"])
        )
    }

    fun DocumentSnapshot.toPostCommentSafe(): PostComment? {
        val data = data ?: return null
        return PostComment(
            id = id,
            postId = anyToString(data["postId"]),
            parentId = anyToNullableString(data["parentId"]),
            replyToAuthor = anyToNullableString(data["replyToAuthor"]),
            authorId = anyToString(data["authorId"]),
            authorName = anyToString(data["authorName"], "Rider").ifBlank { "Rider" },
            authorPhotoURL = anyToNullableString(data["authorPhotoURL"]),
            text = anyToString(data["text"]),
            replyCount = anyToLong(data["replyCount"], 0L),
            isReported = anyToBool(data["isReported"], false),
            isDeleted = anyToBool(data["isDeleted"], false),
            deletedReason = anyToNullableString(data["deletedReason"]),
            createdAt = anyToString(data["createdAt"]),
            updatedAt = anyToString(data["updatedAt"])
        )
    }

    fun DocumentSnapshot.toUserProfileSafe(): UserProfile? {
        val data = data ?: return null
        return UserProfile(
            id = id,
            email = anyToString(data["email"]),
            emailVerified = anyToBool(data["emailVerified"], false),
            displayName = anyToString(data["displayName"], "Cyclist").ifBlank { "Cyclist" },
            photoURL = anyToNullableString(data["photoURL"]),
            bio = anyToNullableString(data["bio"]),
            role = anyToString(data["role"], "PUBLIC").ifBlank { "PUBLIC" },
            customTitle = anyToNullableString(data["customTitle"]),
            adminMembershipStatus = anyToString(data["adminMembershipStatus"], "NONE").ifBlank { "NONE" },
            isAdminMember = anyToBool(data["isAdminMember"], false),
            isSuspended = anyToBool(data["isSuspended"], false),
            welcomeSeenForRole = anyToNullableString(data["welcomeSeenForRole"]),
            ratingAverage = anyToDouble(data["ratingAverage"], 0.0),
            ratingCount = anyToLong(data["ratingCount"], 0L),
            createdAt = anyToString(data["createdAt"]),
            updatedAt = anyToString(data["updatedAt"])
        )
    }

    fun DocumentSnapshot.toMemberRatingSafe(): MemberRating? {
        val data = data ?: return null
        return MemberRating(
            id = id,
            targetMemberId = anyToString(data["targetMemberId"]),
            raterId = anyToString(data["raterId"]),
            raterName = anyToString(data["raterName"]),
            rating = anyToInt(data["rating"], 5).coerceIn(1, 5),
            comment = anyToString(data["comment"]),
            createdAt = anyToString(data["createdAt"]),
            updatedAt = anyToString(data["updatedAt"])
        )
    }

    fun DocumentSnapshot.toMembershipApplicationSafe(): MembershipApplication? {
        val data = data ?: return null
        val recsRaw = data["adminRecommendations"] as? List<*> ?: emptyList<Any?>()
        val recs = recsRaw.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            AdminRecommendation(
                adminId = anyToString(m["adminId"]),
                adminName = anyToString(m["adminName"]),
                recommendation = anyToString(m["recommendation"]),
                comment = anyToString(m["comment"]),
                createdAt = anyToString(m["createdAt"])
            )
        }
        return MembershipApplication(
            id = id,
            userId = anyToString(data["userId"], id).ifBlank { id },
            applicantName = anyToString(data["applicantName"]),
            applicantEmail = anyToString(data["applicantEmail"]),
            applicantPhotoURL = anyToNullableString(data["applicantPhotoURL"]),
            reason = anyToString(data["reason"]),
            cyclingExperience = anyToString(data["cyclingExperience"]),
            requestedTitle = anyToString(data["requestedTitle"]),
            status = anyToString(data["status"], "PENDING").ifBlank { "PENDING" },
            adminRecommendations = recs,
            rejectionReason = anyToNullableString(data["rejectionReason"]),
            reviewedBy = anyToNullableString(data["reviewedBy"]),
            reviewedAt = anyToNullableString(data["reviewedAt"]),
            createdAt = anyToString(data["createdAt"]),
            updatedAt = anyToString(data["updatedAt"])
        )
    }

    fun DocumentSnapshot.toAppNotificationSafe(): AppNotification? {
        val data = data ?: return null
        val rawReplies = data["replies"] as? List<*> ?: emptyList<Any?>()
        val parsedReplies = rawReplies.mapNotNull { item ->
            val m = item as? Map<*, *> ?: return@mapNotNull null
            NoticeReply(
                id = anyToString(m["id"]),
                senderId = anyToString(m["senderId"]),
                senderName = anyToString(m["senderName"]),
                senderEmail = anyToString(m["senderEmail"]),
                senderRole = anyToString(m["senderRole"]),
                senderPhotoURL = anyToNullableString(m["senderPhotoURL"]),
                text = anyToString(m["text"]),
                createdAt = anyToString(m["createdAt"])
            )
        }
        return AppNotification(
            id = id,
            recipientId = anyToString(data["recipientId"]),
            recipientName = anyToString(data["recipientName"]),
            recipientEmail = anyToString(data["recipientEmail"]),
            senderId = anyToString(data["senderId"]),
            senderName = anyToString(data["senderName"]),
            senderEmail = anyToString(data["senderEmail"]),
            senderRole = anyToString(data["senderRole"]),
            type = anyToString(data["type"], "SYSTEM").ifBlank { "SYSTEM" },
            title = anyToString(data["title"]),
            message = anyToString(data["message"]),
            link = anyToString(data["link"]),
            isRead = anyToBool(data["isRead"], false),
            readAt = anyToNullableString(data["readAt"]),
            deliveryStatus = anyToString(data["deliveryStatus"], "DELIVERED"),
            deliveredAt = anyToNullableString(data["deliveredAt"]),
            isDirectNotice = anyToBool(data["isDirectNotice"], false),
            isUrgent = anyToBool(data["isUrgent"], false),
            replies = parsedReplies,
            replyCount = anyToLong(data["replyCount"], parsedReplies.size.toLong()),
            lastRepliedAt = anyToNullableString(data["lastRepliedAt"]),
            hasNewReply = anyToBool(data["hasNewReply"], false),
            applicantId = anyToNullableString(data["applicantId"]),
            applicantName = anyToNullableString(data["applicantName"]),
            applicantPhotoURL = anyToNullableString(data["applicantPhotoURL"]),
            createdAt = anyToString(data["createdAt"]),
            updatedAt = anyToString(data["updatedAt"])
        )
    }

    fun DocumentSnapshot.toContentReportSafe(): ContentReport? {
        val data = data ?: return null
        return ContentReport(
            id = id,
            targetType = anyToString(data["targetType"], "COMMENT"),
            targetId = anyToString(data["targetId"]),
            targetAuthorId = anyToString(data["targetAuthorId"]),
            targetExcerpt = anyToString(data["targetExcerpt"]),
            reporterId = anyToString(data["reporterId"]),
            reporterEmail = anyToString(data["reporterEmail"]),
            reason = anyToString(data["reason"]),
            status = anyToString(data["status"], "PENDING"),
            moderatorNotes = anyToNullableString(data["moderatorNotes"]),
            createdAt = anyToString(data["createdAt"])
        )
    }

    fun DocumentSnapshot.toInvitationCodeSafe(): InvitationCode? {
        val data = data ?: return null
        return InvitationCode(
            id = id,
            codeHash = anyToString(data["codeHash"]),
            displayCodeMasked = anyToString(data["displayCodeMasked"]),
            targetRole = anyToString(data["targetRole"], "MEMBER"),
            isRedeemed = anyToBool(data["isRedeemed"], false),
            redeemedByUserId = anyToString(data["redeemedByUserId"]),
            redeemedByEmail = anyToString(data["redeemedByEmail"]),
            expiresAt = anyToString(data["expiresAt"]),
            isRevoked = anyToBool(data["isRevoked"], false),
            createdBy = anyToString(data["createdBy"]),
            createdAt = anyToString(data["createdAt"])
        )
    }

    fun DocumentSnapshot.toAuditLogSafe(): AuditLog? {
        val data = data ?: return null
        return AuditLog(
            id = id,
            actorId = anyToString(data["actorId"]),
            actorEmail = anyToString(data["actorEmail"]),
            action = anyToString(data["action"]),
            targetType = anyToString(data["targetType"]),
            targetId = anyToString(data["targetId"]),
            details = anyToString(data["details"]),
            createdAt = anyToString(data["createdAt"])
        )
    }
}
