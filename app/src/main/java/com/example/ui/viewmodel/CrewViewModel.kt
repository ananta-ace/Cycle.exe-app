package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CycleContainer
import com.example.data.model.FeaturedMember
import com.example.data.model.MemberRating
import com.example.data.model.MembershipApplication
import com.example.data.repository.CrewRepository
import com.example.data.repository.SyncState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RatingUiState(
    val targetMemberId: String = "",
    val fetchGeneration: Long = 0L,
    val existingRating: MemberRating? = null,
    val isFetching: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CrewViewModel(private val container: CycleContainer) : ViewModel() {
    private val crewRepo = container.crewRepository

    private val _isRefreshing = MutableStateFlow(false)
    private val _manualError = MutableStateFlow<String?>(null)
    private val _serverOverride = MutableStateFlow<List<FeaturedMember>?>(null)
    private val _ratingOverrides = MutableStateFlow<Map<String, CrewRepository.RatingSummary>>(emptyMap())

    val membersState: StateFlow<SyncState<FeaturedMember>> = combine(
        combine(
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
        },
        _ratingOverrides
    ) { baseState, overrides ->
        if (overrides.isEmpty()) {
            baseState
        } else {
            val patched = baseState.items.map { member ->
                val summary = overrides[member.id]
                if (summary != null) {
                    member.copy(
                        ratingAverage = summary.ratingAverage,
                        ratingCount = summary.ratingCount
                    )
                } else {
                    member
                }
            }
            baseState.copy(items = patched)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SyncState(isLoading = true))

    fun refreshMembers(clearRatingOverrides: Boolean = true) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _manualError.value = null
            val res = crewRepo.fetchFeaturedMembersFromServer()
            res.onSuccess { list ->
                _serverOverride.value = list
                if (clearRatingOverrides) {
                    _ratingOverrides.value = emptyMap()
                } else {
                    _ratingOverrides.update { current ->
                        current.filter { (memberId, overrideSummary) ->
                            val serverMember = list.firstOrNull { it.id == memberId }
                            serverMember == null ||
                                serverMember.ratingCount != overrideSummary.ratingCount ||
                                serverMember.ratingAverage != overrideSummary.ratingAverage
                        }
                    }
                }
            }.onFailure { e ->
                _manualError.value = e.localizedMessage ?: "Failed to sync crew directory from server."
            }
            _isRefreshing.value = false
        }
    }

    private val _selectedMemberIdForReviews = MutableStateFlow<String?>(null)
    private var nextFetchGeneration = 0L
    private val _ratingUiState = MutableStateFlow(RatingUiState())
    val ratingUiState: StateFlow<RatingUiState> = _ratingUiState.asStateFlow()

    val selectedMemberRatings: StateFlow<List<MemberRating>> = combine(
        _selectedMemberIdForReviews.flatMapLatest { memberId ->
            if (memberId.isNullOrBlank()) flowOf(emptyList()) else crewRepo.observeMemberRatings(memberId)
        },
        _selectedMemberIdForReviews,
        _ratingUiState
    ) { liveRatings, selectedId, ratingUi ->
        val own = ratingUi.existingRating
        if (!selectedId.isNullOrBlank() &&
            ratingUi.targetMemberId == selectedId &&
            own != null &&
            liveRatings.none { it.id == own.id || it.raterId == own.raterId }
        ) {
            listOf(own) + liveRatings
        } else {
            liveRatings
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun observeRatingsForMember(memberId: String?) {
        _selectedMemberIdForReviews.value = memberId
    }

    private val _ratingNotice = MutableStateFlow<String?>(null)
    val ratingNotice: StateFlow<String?> = _ratingNotice.asStateFlow()

    private val _myApplication = MutableStateFlow<MembershipApplication?>(null)
    val myApplication: StateFlow<MembershipApplication?> = _myApplication.asStateFlow()

    private val _appBusy = MutableStateFlow(false)
    val appBusy: StateFlow<Boolean> = _appBusy.asStateFlow()

    private val _appError = MutableStateFlow<String?>(null)
    val appError: StateFlow<String?> = _appError.asStateFlow()

    private val _appSuccess = MutableStateFlow(false)
    val appSuccess: StateFlow<Boolean> = _appSuccess.asStateFlow()

    fun clearRatingNotice() {
        _ratingNotice.value = null
    }

    fun loadExistingRating(targetMember: FeaturedMember, raterId: String) {
        val gen = ++nextFetchGeneration
        _ratingUiState.value = RatingUiState(
            targetMemberId = targetMember.id,
            fetchGeneration = gen,
            existingRating = null,
            isFetching = true,
            isSubmitting = false,
            error = null
        )
        viewModelScope.launch {
            val found = crewRepo.getExistingRating(targetMember, raterId)
            _ratingUiState.update { current ->
                if (current.targetMemberId == targetMember.id && current.fetchGeneration == gen) {
                    current.copy(
                        existingRating = found,
                        isFetching = false
                    )
                } else {
                    current
                }
            }
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
        val currentState = _ratingUiState.value
        val existing = if (currentState.targetMemberId == targetMember.id) {
            currentState.existingRating
        } else {
            null
        }
        val wasUpdate = existing != null

        _ratingUiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val res = crewRepo.submitRating(
                targetMember = targetMember,
                raterId = raterId,
                raterName = raterName,
                rating = rating,
                comment = comment,
                existingRating = existing
            )
            res.onSuccess { writeResult ->
                _ratingOverrides.update { it + (targetMember.id to writeResult.summary) }
                _ratingUiState.update {
                    it.copy(
                        targetMemberId = targetMember.id,
                        existingRating = writeResult.savedRating,
                        isFetching = false,
                        isSubmitting = false,
                        error = null
                    )
                }
                _ratingNotice.value = if (wasUpdate) {
                    "Updated your rating for ${targetMember.name}."
                } else {
                    "Submitted your rating for ${targetMember.name}!"
                }
                refreshMembers(clearRatingOverrides = false)
                onSuccess()
            }.onFailure { e ->
                _ratingUiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = e.message ?: "Failed to submit rating. Please try again."
                    )
                }
            }
        }
    }

    fun deleteRating(targetMember: FeaturedMember, raterId: String, onSuccess: () -> Unit) {
        val currentState = _ratingUiState.value
        val prevRating = if (currentState.targetMemberId == targetMember.id) {
            currentState.existingRating?.rating
        } else {
            null
        }

        _ratingUiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            crewRepo.deleteRating(
                targetMember = targetMember,
                raterId = raterId,
                previousRating = prevRating
            ).onSuccess { summary ->
                _ratingOverrides.update { it + (targetMember.id to summary) }
                _ratingUiState.update {
                    it.copy(
                        targetMemberId = targetMember.id,
                        existingRating = null,
                        isFetching = false,
                        isSubmitting = false,
                        error = null
                    )
                }
                _ratingNotice.value = "Removed your rating for ${targetMember.name}."
                refreshMembers(clearRatingOverrides = false)
                onSuccess()
            }.onFailure { e ->
                _ratingUiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = e.message ?: "Failed to delete rating."
                    )
                }
            }
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
