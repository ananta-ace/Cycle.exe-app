package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CycleContainer
import com.example.data.model.AppNotification
import com.example.data.model.AuditLog
import com.example.data.model.ContentReport
import com.example.data.model.FeaturedMember
import com.example.data.model.InvitationCode
import com.example.data.model.MembershipApplication
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val container: CycleContainer,
    private val isSuperAdmin: Boolean
) : ViewModel() {
    private val adminRepo = container.adminRepository
    private val crewRepo = container.crewRepository
    private val notifRepo = container.notificationsRepository
    private val uploadRepo = container.imageUploadRepository

    val applications: StateFlow<List<MembershipApplication>> = adminRepo.observeApplications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ContentReport>> = adminRepo.observeReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<UserProfile>> = crewRepo.observeAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val featuredMembers: StateFlow<List<FeaturedMember>> = crewRepo.observeFeaturedMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotifications: StateFlow<List<AppNotification>> = notifRepo.observeAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = if (isSuperAdmin) {
        adminRepo.observeAuditLogs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow<List<AuditLog>>(emptyList()).asStateFlow()
    }

    val invitationCodes: StateFlow<List<InvitationCode>> = if (isSuperAdmin) {
        adminRepo.observeInvitationCodes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow<List<InvitationCode>>(emptyList()).asStateFlow()
    }

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _latestGeneratedCode = MutableStateFlow<Pair<String, String>?>(null)
    val latestGeneratedCode: StateFlow<Pair<String, String>?> = _latestGeneratedCode.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _featUploadProgress = MutableStateFlow<Int?>(null)
    val featUploadProgress: StateFlow<Int?> = _featUploadProgress.asStateFlow()

    // Moderator actions
    fun updateReportStatus(reportId: String, status: String, notes: String = "") {
        viewModelScope.launch {
            adminRepo.updateReportStatus(reportId, status, notes)
                .onSuccess { _statusMessage.value = "Report marked as $status." }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to update report." }
        }
    }

    fun recommendApplication(
        app: MembershipApplication,
        adminId: String,
        adminName: String,
        recommendation: String,
        comment: String
    ) {
        viewModelScope.launch {
            adminRepo.addAdminRecommendation(app, adminId, adminName, recommendation, comment)
                .onSuccess { _statusMessage.value = "Recommendation ($recommendation) submitted!" }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to submit recommendation." }
        }
    }

    // Super Admin Application decisions
    fun approveApplication(app: MembershipApplication, reviewerUid: String, reviewerEmail: String) {
        viewModelScope.launch {
            _busy.value = true
            adminRepo.approveApplication(app, reviewerUid, reviewerEmail)
                .onSuccess { _statusMessage.value = "Approved ${app.applicantName} as Admin Member!" }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to approve application." }
            _busy.value = false
        }
    }

    fun rejectApplication(app: MembershipApplication, reason: String, reviewerUid: String, reviewerEmail: String) {
        viewModelScope.launch {
            _busy.value = true
            adminRepo.rejectApplication(app, reason, reviewerUid, reviewerEmail)
                .onSuccess { _statusMessage.value = "Rejected application for ${app.applicantName}." }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to reject application." }
            _busy.value = false
        }
    }

    // Super Admin User & Role Management
    fun changeUserRole(targetUser: UserProfile, newRole: String, actorUid: String, actorEmail: String) {
        viewModelScope.launch {
            adminRepo.changeUserRole(targetUser, newRole, actorUid, actorEmail)
                .onSuccess { _statusMessage.value = "Updated ${targetUser.displayName}'s role to $newRole." }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to change role." }
        }
    }

    fun toggleUserSuspension(targetUser: UserProfile, actorUid: String, actorEmail: String) {
        viewModelScope.launch {
            adminRepo.toggleUserSuspension(targetUser, actorUid, actorEmail)
                .onSuccess {
                    val label = if (!targetUser.isSuspended) "Suspended" else "Reactivated"
                    _statusMessage.value = "$label ${targetUser.displayName}."
                }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to toggle suspension." }
        }
    }

    fun removeFromAdminPanel(targetUserId: String, name: String, actorUid: String, actorEmail: String) {
        viewModelScope.launch {
            adminRepo.removeFromAdminPanel(targetUserId, name, actorUid, actorEmail)
                .onSuccess { _statusMessage.value = "Removed $name from Admin panel." }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to remove admin." }
        }
    }

    fun updateCustomTitle(targetUserId: String, newTitle: String, actorUid: String, actorEmail: String, onDone: () -> Unit) {
        viewModelScope.launch {
            adminRepo.updateCustomTitle(targetUserId, newTitle, actorUid, actorEmail)
                .onSuccess {
                    _statusMessage.value = "Custom community title updated!"
                    onDone()
                }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to update title." }
        }
    }

    // Featured Members Management
    fun featureRegisteredUser(user: UserProfile, onCreated: (FeaturedMember) -> Unit) {
        viewModelScope.launch {
            val nextOrder = (featuredMembers.value.size + 1).toLong()
            crewRepo.featureRegisteredUser(user, nextOrder)
                .onSuccess { created ->
                    _statusMessage.value = "${user.displayName} added to Home Screen crew!"
                    onCreated(created)
                }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to feature user." }
        }
    }

    fun uploadFeaturedPhoto(uri: Uri, onUrlReady: (String) -> Unit) {
        viewModelScope.launch {
            _featUploadProgress.value = 10
            try {
                val url = uploadRepo.uploadImageUri(
                    uri = uri,
                    folder = "featured",
                    maxBytes = 400_000,
                    onProgress = { pct -> _featUploadProgress.value = pct }
                )
                onUrlReady(url)
            } catch (e: Exception) {
                _statusMessage.value = e.message ?: "Failed to upload featured photo."
            } finally {
                _featUploadProgress.value = null
            }
        }
    }

    fun saveFeaturedMember(
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
        order: Long,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank()) {
            _statusMessage.value = "Rider name cannot be empty."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            crewRepo.saveFeaturedMember(
                existingId = existingId,
                userId = userId,
                name = name,
                title = title,
                img = img,
                active = active,
                attend = attend,
                perfect = perfect,
                bio = bio,
                badges = badges,
                completedRaces = completedRaces,
                order = order
            ).onSuccess {
                _statusMessage.value = "Featured crew profile saved!"
                onSuccess()
            }.onFailure { e ->
                _statusMessage.value = e.message ?: "Failed to save featured member."
            }
            _busy.value = false
        }
    }

    fun deleteFeaturedMember(memberId: String, name: String) {
        viewModelScope.launch {
            crewRepo.deleteFeaturedMember(memberId)
                .onSuccess { _statusMessage.value = "Removed $name from featured crew." }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to remove member." }
        }
    }

    // Direct Notices
    fun sendDirectNotice(
        recipient: UserProfile,
        senderId: String,
        senderName: String,
        senderEmail: String,
        title: String,
        message: String,
        isUrgent: Boolean,
        onSuccess: () -> Unit
    ) {
        if (message.isBlank()) {
            _statusMessage.value = "Notice message cannot be empty."
            return
        }
        viewModelScope.launch {
            _busy.value = true
            notifRepo.sendDirectNotice(
                recipient = recipient,
                senderId = senderId,
                senderName = senderName,
                senderEmail = senderEmail,
                title = title,
                message = message,
                isUrgent = isUrgent
            ).onSuccess {
                _statusMessage.value = "Direct notice sent to ${recipient.displayName}!"
                onSuccess()
            }.onFailure { e ->
                _statusMessage.value = e.message ?: "Failed to send direct notice."
            }
            _busy.value = false
        }
    }

    fun sendAdminReplyToNotice(
        notice: AppNotification,
        senderId: String,
        senderName: String,
        senderEmail: String,
        text: String,
        onSuccess: () -> Unit
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            notifRepo.addReplyToNotice(
                notice = notice,
                senderId = senderId,
                senderName = senderName,
                senderEmail = senderEmail,
                senderRole = "SUPER_ADMIN",
                senderPhotoURL = "",
                text = text,
                isSuperAdmin = true
            ).onSuccess { onSuccess() }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to reply." }
        }
    }

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch {
            notifRepo.deleteNotification(noticeId)
                .onSuccess { _statusMessage.value = "Notice deleted." }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to delete notice." }
        }
    }

    // Invitation Codes
    fun generateInvitationCode(targetRole: String, actorUid: String, actorEmail: String) {
        viewModelScope.launch {
            _busy.value = true
            adminRepo.generateInvitationCode(targetRole, actorUid, actorEmail)
                .onSuccess { code ->
                    _latestGeneratedCode.value = code to targetRole
                    _statusMessage.value = "Generated $targetRole invitation code."
                }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to generate code." }
            _busy.value = false
        }
    }

    fun revokeInvitationCode(codeId: String, actorUid: String, actorEmail: String) {
        viewModelScope.launch {
            adminRepo.revokeInvitationCode(codeId, actorUid, actorEmail)
                .onSuccess { _statusMessage.value = "Invitation code revoked." }
                .onFailure { e -> _statusMessage.value = e.message ?: "Failed to revoke code." }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    companion object {
        fun factory(container: CycleContainer, isSuperAdmin: Boolean): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminViewModel(container, isSuperAdmin) as T
                }
            }
    }
}
