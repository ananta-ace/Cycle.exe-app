package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CycleContainer
import com.example.data.model.AppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(private val container: CycleContainer) : ViewModel() {
    private val notifRepo = container.notificationsRepository

    val allNotifications: StateFlow<List<AppNotification>> = notifRepo.observeAllNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _busyId = MutableStateFlow<String?>(null)
    val busyId: StateFlow<String?> = _busyId.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun filterForUser(
        list: List<AppNotification>,
        uid: String,
        isSuperAdmin: Boolean,
        isAdmin: Boolean
    ): List<AppNotification> = notifRepo.filterForUser(list, uid, isSuperAdmin, isAdmin)

    fun markAsRead(notif: AppNotification) {
        viewModelScope.launch {
            notifRepo.markAsRead(notif)
        }
    }

    fun markAllRead(visibleList: List<AppNotification>) {
        viewModelScope.launch {
            notifRepo.markAllRead(visibleList)
        }
    }

    fun sendReply(
        notice: AppNotification,
        senderId: String,
        senderName: String,
        senderEmail: String,
        senderRole: String,
        senderPhotoURL: String,
        text: String,
        isSuperAdmin: Boolean,
        onSuccess: () -> Unit
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _busyId.value = notice.id
            _error.value = null
            notifRepo.addReplyToNotice(
                notice = notice,
                senderId = senderId,
                senderName = senderName,
                senderEmail = senderEmail,
                senderRole = senderRole,
                senderPhotoURL = senderPhotoURL,
                text = text,
                isSuperAdmin = isSuperAdmin
            ).onSuccess {
                onSuccess()
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to send reply."
            }
            _busyId.value = null
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun factory(container: CycleContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotificationsViewModel(container) as T
            }
        }
    }
}
