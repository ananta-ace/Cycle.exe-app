package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CycleContainer
import com.example.data.model.FeaturedMember
import com.example.data.model.MissionReport
import com.example.data.model.MissionSegment
import com.example.data.model.UserProfile
import com.example.data.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CandidateRider(
    val name: String,
    val title: String,
    val photoUrl: String
)

class MissionsViewModel(private val container: CycleContainer) : ViewModel() {
    private val missionsRepo = container.missionsRepository
    private val crewRepo = container.crewRepository

    private val _isRefreshing = MutableStateFlow(false)
    private val _manualError = MutableStateFlow<String?>(null)
    private val _serverOverride = MutableStateFlow<List<MissionReport>?>(null)

    val missionsState: StateFlow<SyncState<MissionReport>> = combine(
        missionsRepo.observeMissionsState(),
        _isRefreshing,
        _manualError,
        _serverOverride
    ) { liveState, refreshing, manualErr, overrideItems ->
        val resolvedItems = if (liveState.items.isNotEmpty() || liveState.error == null) {
            liveState.items
        } else {
            overrideItems ?: liveState.items
        }
        liveState.copy(
            items = resolvedItems,
            isRefreshing = refreshing,
            error = manualErr ?: liveState.error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SyncState(isLoading = true))

    val candidateRiders: StateFlow<List<CandidateRider>> = combine(
        crewRepo.observeFeaturedMembers(),
        crewRepo.observeAllUsers()
    ) { featured: List<FeaturedMember>, users: List<UserProfile> ->
        val map = linkedMapOf<String, CandidateRider>()
        for (fm in featured) {
            if (fm.name.isNotBlank()) {
                map[fm.name.lowercase()] = CandidateRider(
                    name = fm.name,
                    title = fm.title,
                    photoUrl = fm.img
                )
            }
        }
        for (u in users) {
            val name = u.displayName.trim()
            if (name.isNotBlank() && !map.containsKey(name.lowercase())) {
                map[name.lowercase()] = CandidateRider(
                    name = name,
                    title = u.customTitle?.ifBlank { null } ?: u.role,
                    photoUrl = u.photoURL.orEmpty()
                )
            }
        }
        map.values.toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refreshMissions() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _manualError.value = null
            val res = missionsRepo.fetchMissionsFromServer()
            res.onSuccess { list ->
                _serverOverride.value = list
            }.onFailure { e ->
                _manualError.value = e.localizedMessage ?: "Failed to sync missions from server."
            }
            _isRefreshing.value = false
        }
    }

    fun saveMission(
        editingId: String?,
        tag: String,
        title: String,
        date: String,
        startTime: String,
        endTime: String,
        duration: String,
        avgSpeed: String,
        totalDist: String,
        segments: List<MissionSegment>,
        crewIds: List<String>,
        notes: String,
        variant: String,
        customColor: String?,
        isDuo: Boolean,
        createdBy: String,
        onSuccess: () -> Unit
    ) {
        if (tag.isBlank() || title.isBlank()) {
            _error.value = "Mission tag and title are required."
            return
        }
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            val res = missionsRepo.saveMission(
                editingId = editingId,
                tag = tag,
                title = title,
                date = date,
                startTime = startTime,
                endTime = endTime,
                duration = duration,
                avgSpeed = avgSpeed,
                totalDist = totalDist,
                segments = segments,
                crewIds = crewIds,
                notes = notes,
                variant = variant,
                customColor = customColor,
                isDuo = isDuo,
                createdBy = createdBy
            )
            res.onSuccess { onSuccess() }
                .onFailure { e -> _error.value = e.message ?: "Failed to save mission." }
            _saving.value = false
        }
    }

    fun deleteMission(mission: MissionReport) {
        viewModelScope.launch {
            missionsRepo.deleteMission(mission)
                .onFailure { e -> _error.value = e.message ?: "Failed to delete mission." }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun factory(container: CycleContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MissionsViewModel(container) as T
            }
        }
    }
}
