package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CycleContainer
import com.example.data.model.GalleryPhoto
import com.example.data.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(private val container: CycleContainer) : ViewModel() {
    private val galleryRepo = container.galleryRepository
    private val uploadRepo = container.imageUploadRepository

    private val _isRefreshing = MutableStateFlow(false)
    private val _manualError = MutableStateFlow<String?>(null)
    private val _serverOverride = MutableStateFlow<List<GalleryPhoto>?>(null)

    val photosState: StateFlow<SyncState<GalleryPhoto>> = combine(
        galleryRepo.observeGalleryPhotosState(),
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

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refreshGallery() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _manualError.value = null
            val res = galleryRepo.fetchGalleryFromServer()
            res.onSuccess { list ->
                _serverOverride.value = list
            }.onFailure { e ->
                _manualError.value = e.localizedMessage ?: "Failed to sync gallery from server."
            }
            _isRefreshing.value = false
        }
    }

    fun uploadPhoto(
        uri: Uri,
        title: String,
        description: String,
        uploadedBy: String,
        uploadedByName: String,
        onSuccess: () -> Unit
    ) {
        if (title.isBlank()) {
            _error.value = "Please provide a photo title."
            return
        }
        viewModelScope.launch {
            _uploading.value = true
            _error.value = null
            _uploadProgress.value = 10
            try {
                val url = uploadRepo.uploadImageUri(
                    uri = uri,
                    folder = "gallery",
                    maxBytes = 450_000,
                    onProgress = { pct -> _uploadProgress.value = pct }
                )
                galleryRepo.addGalleryPhoto(
                    title = title,
                    description = description,
                    photoURL = url,
                    uploadedBy = uploadedBy,
                    uploadedByName = uploadedByName
                ).onSuccess {
                    onSuccess()
                }.onFailure { e ->
                    _error.value = e.message ?: "Failed to save gallery photo."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to upload photo."
            } finally {
                _uploadProgress.value = null
                _uploading.value = false
            }
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            galleryRepo.deleteGalleryPhoto(photoId)
                .onFailure { e -> _error.value = e.message ?: "Failed to delete photo." }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun factory(container: CycleContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GalleryViewModel(container) as T
            }
        }
    }
}
