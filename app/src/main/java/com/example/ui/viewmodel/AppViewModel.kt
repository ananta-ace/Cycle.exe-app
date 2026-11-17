package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CycleContainer
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.repository.SignInCancelledException
import com.example.data.repository.SuperAdminConfig
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppSessionState(
    val user: FirebaseUser? = null,
    val profile: UserProfile? = null,
    val role: UserRole = UserRole.PUBLIC,
    val isSuperAdmin: Boolean = false,
    val firestoreSuperAdminSynced: Boolean = false,
    val isAdmin: Boolean = false,
    val isMember: Boolean = false,
    val isSuspended: Boolean = false,
    val unreadNotificationsCount: Int = 0,
    val isDarkTheme: Boolean = false,
    val showSplash: Boolean = true,
    val authBusy: Boolean = false,
    val signInError: String? = null,
    val signInNotice: String? = null,
    val profileMessage: String? = null,
    val profileError: String? = null,
    val avatarProgress: Int? = null,
    val dismissedWelcomeKeys: Set<String> = emptySet()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val container: CycleContainer) : ViewModel() {
    private val authRepo = container.authRepository
    private val notifRepo = container.notificationsRepository
    private val themeRepo = container.themePreferencesRepository
    private val uploadRepo = container.imageUploadRepository

    private val _showSplash = MutableStateFlow(true)
    private val _authBusy = MutableStateFlow(false)
    private val _signInError = MutableStateFlow<String?>(null)
    private val _signInNotice = MutableStateFlow<String?>(null)
    private val _profileMessage = MutableStateFlow<String?>(null)
    private val _profileError = MutableStateFlow<String?>(null)
    private val _avatarProgress = MutableStateFlow<Int?>(null)
    private val _sessionDismissedWelcomeKeys = MutableStateFlow<Set<String>>(emptySet())

    private val authUserFlow = authRepo.observeAuthUser()
        .stateIn(viewModelScope, SharingStarted.Eagerly, container.firebaseProvider.auth.currentUser)

    private val profileFlow: StateFlow<UserProfile?> = authUserFlow
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(null)
            } else {
                authRepo.ensureUserProfile(user)
                authRepo.observeUserProfile(user.uid)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val notificationsFlow = authUserFlow
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList()) else notifRepo.observeAllNotifications()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val darkThemeAndWelcomeFlow = combine(
        themeRepo.isDarkTheme,
        themeRepo.dismissedWelcomeKeys,
        _sessionDismissedWelcomeKeys
    ) { isDark, storedKeys, sessionKeys ->
        isDark to (storedKeys + sessionKeys)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false to emptySet())

    val sessionState: StateFlow<AppSessionState> = combine(
        authUserFlow,
        profileFlow,
        notificationsFlow,
        darkThemeAndWelcomeFlow,
        combine(
            _showSplash,
            _authBusy,
            combine(_signInError, _signInNotice) { err, notice -> err to notice },
            _profileMessage,
            combine(_profileError, _avatarProgress) { e, p -> e to p }
        ) { splash, busy, signPair, msg, errPair ->
            QuadState(splash, busy, signPair.first, signPair.second, msg, errPair.first, errPair.second)
        }
    ) { user, profile, allNotifs, themeAndDismissed, ui ->
        val isDark = themeAndDismissed.first
        val dismissedWelcomeKeys = themeAndDismissed.second
        val isSuper = SuperAdminConfig.isSuperAdminEmail(user?.email)
        val firestoreSynced = isSuper && profile?.role == "SUPER_ADMIN"
        val rawRole = UserRole.fromString(profile?.role)
        val effectiveRole = when {
            isSuper -> UserRole.SUPER_ADMIN
            rawRole == UserRole.SUPER_ADMIN -> UserRole.PUBLIC
            else -> rawRole
        }
        val suspended = profile?.isSuspended == true
        val isAdmin = isSuper || (effectiveRole == UserRole.ADMIN && !suspended)
        val isMember = (
            effectiveRole == UserRole.MEMBER ||
                profile?.isAdminMember == true ||
                profile?.adminMembershipStatus == "APPROVED" ||
                isAdmin
            ) && !suspended

        val unreadCount = if (user == null) {
            0
        } else {
            notifRepo.filterForUser(allNotifs, user.uid, isSuper, isAdmin).count { !it.isRead }
        }

        AppSessionState(
            user = user,
            profile = profile,
            role = effectiveRole,
            isSuperAdmin = isSuper,
            firestoreSuperAdminSynced = firestoreSynced,
            isAdmin = isAdmin,
            isMember = isMember,
            isSuspended = suspended,
            unreadNotificationsCount = unreadCount,
            isDarkTheme = isDark,
            showSplash = ui.showSplash,
            authBusy = ui.authBusy,
            signInError = ui.signInError,
            signInNotice = ui.signInNotice,
            profileMessage = ui.profileMessage,
            profileError = ui.profileError,
            avatarProgress = ui.avatarProgress,
            dismissedWelcomeKeys = dismissedWelcomeKeys
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSessionState())

    private var silentSignInAttempted = false

    init {
        viewModelScope.launch {
            delay(1300)
            _showSplash.value = false
        }
    }

    fun attemptSilentSignInIfNeeded(activityContext: Context) {
        if (silentSignInAttempted || authUserFlow.value != null) return
        silentSignInAttempted = true
        viewModelScope.launch {
            authRepo.trySilentGoogleSignIn(activityContext)
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _authBusy.value = true
            _signInError.value = null
            _signInNotice.value = null
            val res = authRepo.signInWithGoogleInteractive(activityContext)
            res.onFailure { err ->
                if (err is SignInCancelledException) {
                    _signInNotice.value = err.message
                } else {
                    _signInError.value = err.message ?: "Google Sign-In failed. Try Browser Google Sign-In below."
                }
            }
            _authBusy.value = false
        }
    }

    fun signInWithGoogleBrowser(activity: Activity) {
        viewModelScope.launch {
            _authBusy.value = true
            _signInError.value = null
            _signInNotice.value = null
            val res = authRepo.signInWithGoogleBrowser(activity)
            res.onFailure { err ->
                if (err is SignInCancelledException) {
                    _signInNotice.value = err.message
                } else {
                    _signInError.value = err.message ?: "Browser Google Sign-In failed. Please try again."
                }
            }
            _authBusy.value = false
        }
    }

    fun signOut(activityContext: Context) {
        viewModelScope.launch {
            _authBusy.value = true
            authRepo.signOut(activityContext)
            _signInError.value = null
            _signInNotice.value = null
            _profileMessage.value = null
            _profileError.value = null
            _authBusy.value = false
        }
    }

    fun clearSignInError() {
        _signInError.value = null
        _signInNotice.value = null
    }

    fun toggleDarkTheme() {
        viewModelScope.launch {
            themeRepo.setDarkTheme(!darkThemeAndWelcomeFlow.value.first)
        }
    }

    suspend fun syncSuperAdminAccess(): Pair<Boolean, String> {
        return authRepo.syncSuperAdminAccess()
    }

    fun updateProfile(displayName: String, bio: String, photoUrl: String? = null, customTitle: String? = null) {
        viewModelScope.launch {
            _profileError.value = null
            _profileMessage.value = null
            val res = authRepo.updateProfileDetails(displayName, bio, photoUrl, customTitle)
            res.onSuccess {
                _profileMessage.value = "Profile saved successfully!"
            }.onFailure { e ->
                _profileError.value = e.message ?: "Failed to update profile."
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        val currentProfile = sessionState.value.profile
        val user = sessionState.value.user ?: return
        viewModelScope.launch {
            _profileError.value = null
            _profileMessage.value = null
            _avatarProgress.value = 10
            try {
                val uploadedUrl = uploadRepo.uploadImageUri(
                    uri = uri,
                    folder = "avatars",
                    maxBytes = 400_000,
                    onProgress = { pct -> _avatarProgress.value = pct }
                )
                val name = currentProfile?.displayName?.ifBlank { null } ?: user.displayName ?: "Cyclist"
                val bio = currentProfile?.bio.orEmpty()
                val res = authRepo.updateProfileDetails(name, bio, photoURL = uploadedUrl)
                res.onSuccess {
                    _profileMessage.value = "Profile photo updated and saved!"
                }.onFailure { e ->
                    _profileError.value = e.message ?: "Photo uploaded, but saving profile failed."
                }
            } catch (e: Exception) {
                _profileError.value = e.message ?: "Failed to upload avatar."
            } finally {
                _avatarProgress.value = null
            }
        }
    }

    fun clearProfileMessages() {
        _profileMessage.value = null
        _profileError.value = null
    }

    fun dismissWelcomeModal(roleToken: String) {
        val uid = authUserFlow.value?.uid ?: return
        val key = "$uid:$roleToken"
        _sessionDismissedWelcomeKeys.update { it + key }
        viewModelScope.launch {
            themeRepo.addDismissedWelcomeKey(key)
            authRepo.dismissWelcomeModal(roleToken)
        }
    }

    private data class QuadState(
        val showSplash: Boolean,
        val authBusy: Boolean,
        val signInError: String?,
        val signInNotice: String?,
        val profileMessage: String?,
        val profileError: String?,
        val avatarProgress: Int?
    )

    companion object {
        fun factory(container: CycleContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppViewModel(container) as T
            }
        }
    }
}
