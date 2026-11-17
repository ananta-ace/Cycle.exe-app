package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CycleContainer
import com.example.data.model.FeaturedMember
import com.example.data.model.MemberRating
import com.example.data.model.MembershipApplication
import com.example.data.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CrewViewModel(private val container: CycleContainer) : ViewModel() {
    private val crewRepo = container.crewRepository

    private val _isRefreshing = MutableStateFlow(false)
    private val _manualError = MutableStateFlow<String?>(null)
    private val _serverOverride = MutableStateFlow<List<FeaturedMember>?>(null)

    val membersState: StateFlow<SyncState<FeaturedMember>> = combine(
        crewRepo.observeFeaturedMembersState(),
        crewRepo.observeAllUsers(),
        _isRefreshing,
        _manualError,
        _serverOverride
    ) { liveState, users, refreshing, manualErr, overrideItems ->
        val photoMap = users.mapNotNull { u ->
            val p = u.photoURL
            if (!p.isNullOrBlank()) u.id to p else null
        }.toMap()
        val rawItems = if (liveState.items.isNotEmpty() || liveState.error == null) {
            liveState.items
        } else {
            overrideItems ?: liveState.items
        }
        val enriched = rawItems.map { member ->
            if (member.img.isBlank() && member.userId.isNotBlank()) {
                val fallback = photoMap[member.userId].orEmpty()
                if (fallback.isNotBlank()) member.copy(img = fallback) else member
            } else {
                member
            }
        }
        liveState.copy(
            items = enriched,
            isRefreshing = refreshing,
            error = manualErr ?: liveState.error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SyncState(isLoading = true))

    fun refreshMembers() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _manualError.value = null
            val res = crewRepo.fetchFeaturedMembersFromServer()
            res.onSuccess { list ->
                _serverOverride.value = list
            }.onFailure { e ->
                _manualError.value = e.localizedMessage ?: "Failed to sync crew directory from server."
            }
            _isRefreshing.value = false
        }
    }

    private val _existingRating = MutableStateFlow<MemberRating?>(null)
    val existingRating: StateFlow<MemberRating?> = _existingRating.asStateFlow()

    private val _ratingLoading = MutableStateFlow(false)
    val ratingLoading: StateFlow<Boolean> = _ratingLoading.asStateFlow()

    private val _ratingError = MutableStateFlow<String?>(null)
    val ratingError: StateFlow<String?> = _ratingError.asStateFlow()

    private val _myApplication = MutableStateFlow<MembershipApplication?>(null)
    val myApplication: StateFlow<MembershipApplication?> = _myApplication.asStateFlow()

    private val _appBusy = MutableStateFlow(false)
    val appBusy: StateFlow<Boolean> = _appBusy.asStateFlow()

    private val _appError = MutableStateFlow<String?>(null)
    val appError: StateFlow<String?> = _appError.asStateFlow()

    private val _appSuccess = MutableStateFlow(false)
    val appSuccess: StateFlow<Boolean> = _appSuccess.asStateFlow()

    fun loadExistingRating(targetMemberId: String, raterId: String) {
        viewModelScope.launch {
            _ratingLoading.value = true
            _ratingError.value = null
            _existingRating.value = crewRepo.getExistingRating(targetMemberId, raterId)
            _ratingLoading.value = false
        }
    }

    fun submitRating(
        targetMember: FeaturedMember,
        raterId: String,
        raterName: String,
        rating: Int,
        comment: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _ratingLoading.value = true
            _ratingError.value = null
            val res = crewRepo.submitRating(
                targetMember = targetMember,
                raterId = raterId,
                raterName = raterName,
                rating = rating,
                comment = comment,
                existingCreatedAt = _existingRating.value?.createdAt
            )
            res.onSuccess { onSuccess() }
                .onFailure { e -> _ratingError.value = e.message ?: "Failed to submit rating." }
            _ratingLoading.value = false
        }
    }

    fun deleteRating(targetMember: FeaturedMember, raterId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _ratingLoading.value = true
            _ratingError.value = null
            crewRepo.deleteRating(targetMember, raterId)
                .onSuccess { onSuccess() }
                .onFailure { e -> _ratingError.value = e.message ?: "Failed to delete rating." }
            _ratingLoading.value = false
        }
    }

    fun loadMyApplication(uid: String) {
        viewModelScope.launch {
            _appBusy.value = true
            _appError.value = null
            _appSuccess.value = false
            _myApplication.value = crewRepo.getMyApplication(uid)
            _appBusy.value = false
        }
    }

    fun submitApplication(
        uid: String,
        applicantName: String,
        applicantEmail: String,
        applicantPhotoURL: String,
        reason: String,
        cyclingExperience: String,
        requestedTitle: String
    ) {
        if (reason.isBlank()) {
            _appError.value = "Please provide a reason for applying."
            return
        }
        if (cyclingExperience.isBlank()) {
            _appError.value = "Please share your cycling background."
            return
        }
        viewModelScope.launch {
            _appBusy.value = true
            _appError.value = null
            val res = crewRepo.submitApplication(
                uid = uid,
                applicantName = applicantName,
                applicantEmail = applicantEmail,
                applicantPhotoURL = applicantPhotoURL,
                reason = reason,
                cyclingExperience = cyclingExperience,
                requestedTitle = requestedTitle,
                previousApp = _myApplication.value
            )
            res.onSuccess { saved ->
                _myApplication.value = saved
                _appSuccess.value = true
            }.onFailure { e ->
                _appError.value = e.message ?: "Failed to submit application."
            }
            _appBusy.value = false
        }
    }

    companion object {
        fun factory(container: CycleContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CrewViewModel(container) as T
            }
        }
    }
}
