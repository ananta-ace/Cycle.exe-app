package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

enum class UserRole(val key: String) {
    PUBLIC("PUBLIC"),
    MEMBER("MEMBER"),
    ADMIN("ADMIN"),
    SUPER_ADMIN("SUPER_ADMIN");

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.firstOrNull { it.key.equals(value?.trim(), ignoreCase = true) } ?: PUBLIC
        }
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val email: String = "",
    val emailVerified: Boolean = false,
    val displayName: String = "",
    val photoURL: String? = null,
    val bio: String? = null,
    val role: String = "PUBLIC",
    val customTitle: String? = null,
    val adminMembershipStatus: String = "NONE",
    @get:PropertyName("isAdminMember")
    @set:PropertyName("isAdminMember")
    var isAdminMember: Boolean = false,
    @get:PropertyName("isSuspended")
    @set:PropertyName("isSuspended")
    var isSuspended: Boolean = false,
    val welcomeSeenForRole: String? = null,
    val ratingAverage: Double = 0.0,
    val ratingCount: Long = 0L,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@IgnoreExtraProperties
data class FeaturedMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val title: String = "Cycle.exe Crew Leader",
    val img: String = "",
    val initial: String = "",
    val active: Boolean = true,
    val attend: String = "Active Rider",
    val perfect: Boolean = false,
    val bio: String = "",
    val ratingAverage: Double = 0.0,
    val ratingCount: Long = 0L,
    val badges: List<String> = emptyList(),
    val completedRaces: List<String> = emptyList(),
    val completedRacesCount: Long = 0L,
    val order: Long = 0L,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun resolvedInitial(): String {
        return initial.ifBlank { name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "C" }
    }

    fun resolvedRaceCount(): Int {
        return if (completedRaces.isNotEmpty()) completedRaces.size else completedRacesCount.toInt()
    }
}

@IgnoreExtraProperties
data class MemberRating(
    @DocumentId val id: String = "",
    val targetMemberId: String = "",
    val raterId: String = "",
    val raterName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

@IgnoreExtraProperties
data class AdminRecommendation(
    val adminId: String = "",
    val adminName: String = "",
    val recommendation: String = "",
    val comment: String = "",
    val createdAt: String = ""
)

@IgnoreExtraProperties
data class MembershipApplication(
    @DocumentId val id: String = "",
    val userId: String = "",
    val applicantName: String = "",
    val applicantEmail: String = "",
    val applicantPhotoURL: String? = null,
    val reason: String = "",
    val cyclingExperience: String = "",
    val requestedTitle: String = "",
    val status: String = "PENDING",
    val adminRecommendations: List<AdminRecommendation> = emptyList(),
    val rejectionReason: String? = null,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@IgnoreExtraProperties
data class GalleryPhoto(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String? = null,
    val photoURL: String = "",
    val uploadedBy: String = "",
    val uploadedByName: String = "",
    val uploadedAt: String = ""
)

@IgnoreExtraProperties
data class NoticeReply(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderRole: String = "",
    val senderPhotoURL: String? = null,
    val text: String = "",
    val createdAt: String = ""
)

@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val recipientEmail: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val senderRole: String = "",
    val type: String = "SYSTEM",
    val title: String = "",
    val message: String = "",
    val link: String = "",
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val readAt: String? = null,
    val deliveryStatus: String = "DELIVERED",
    val deliveredAt: String? = null,
    @get:PropertyName("isDirectNotice")
    @set:PropertyName("isDirectNotice")
    var isDirectNotice: Boolean = false,
    @get:PropertyName("isUrgent")
    @set:PropertyName("isUrgent")
    var isUrgent: Boolean = false,
    val replies: List<NoticeReply> = emptyList(),
    val replyCount: Long = 0L,
    val lastRepliedAt: String? = null,
    val hasNewReply: Boolean = false,
    val applicantId: String? = null,
    val applicantName: String? = null,
    val applicantPhotoURL: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoURL: String? = null,
    val authorTitle: String = "",
    val content: String = "",
    val photoURLs: List<String> = emptyList(),
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @get:PropertyName("isHidden")
    @set:PropertyName("isHidden")
    var isHidden: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val parentId: String? = null,
    val replyToAuthor: String? = null,
    val authorId: String = "",
    val authorName: String = "",
    val authorPhotoURL: String? = null,
    val text: String = "",
    val replyCount: Long = 0L,
    @get:PropertyName("isReported")
    @set:PropertyName("isReported")
    var isReported: Boolean = false,
    @get:PropertyName("isDeleted")
    @set:PropertyName("isDeleted")
    var isDeleted: Boolean = false,
    val deletedReason: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@IgnoreExtraProperties
data class ContentReport(
    @DocumentId val id: String = "",
    val targetType: String = "COMMENT",
    val targetId: String = "",
    val targetAuthorId: String = "",
    val targetExcerpt: String = "",
    val reporterId: String = "",
    val reporterEmail: String = "",
    val reason: String = "",
    val status: String = "PENDING",
    val moderatorNotes: String? = null,
    val createdAt: String = ""
)

@IgnoreExtraProperties
data class MissionSegment(
    val num: Int = 1,
    val from: String = "",
    val to: String = "",
    val distance: String = "",
    val segment: String = ""
) {
    fun resolvedLabel(): String {
        return when {
            from.isNotBlank() && to.isNotBlank() -> "$from → $to"
            segment.isNotBlank() -> segment
            from.isNotBlank() -> from
            else -> "Leg $num"
        }
    }
}

@IgnoreExtraProperties
data class MissionReport(
    @DocumentId val id: String = "",
    val num: String = "",
    val tag: String = "",
    val name: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val time: String = "",
    val fullDate: String = "",
    val shortDate: String = "",
    val duration: String = "",
    val avgSpeed: String = "",
    val totalDist: String = "",
    val startLocation: String = "",
    val destination: String = "",
    val location: String = "",
    val segments: List<MissionSegment> = emptyList(),
    val crewIds: List<String> = emptyList(),
    val crewNote: String = "",
    val notes: String = "",
    val disclaimer: String = "",
    val icon: String = "fa-map-signs",
    val variant: String = "default",
    val customColor: String? = null,
    @get:PropertyName("isDuo")
    @set:PropertyName("isDuo")
    var isDuo: Boolean = false,
    val status: String = "PUBLISHED",
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun resolvedTitle(): String = name.ifBlank { title.ifBlank { "Untitled Mission" } }
    fun resolvedTag(): String = tag.ifBlank { num.ifBlank { "001" } }
    fun resolvedDate(): String = fullDate.ifBlank { shortDate.ifBlank { date } }
    fun resolvedNotes(): String = notes.ifBlank { crewNote.ifBlank { disclaimer } }
}

@IgnoreExtraProperties
data class InvitationCode(
    @DocumentId val id: String = "",
    val codeHash: String = "",
    val displayCodeMasked: String = "",
    val targetRole: String = "MEMBER",
    @get:PropertyName("isRedeemed")
    @set:PropertyName("isRedeemed")
    var isRedeemed: Boolean = false,
    val redeemedByUserId: String = "",
    val redeemedByEmail: String = "",
    val expiresAt: String = "",
    @get:PropertyName("isRevoked")
    @set:PropertyName("isRevoked")
    var isRevoked: Boolean = false,
    val createdBy: String = "",
    val createdAt: String = ""
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val actorEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    val createdAt: String = ""
)

