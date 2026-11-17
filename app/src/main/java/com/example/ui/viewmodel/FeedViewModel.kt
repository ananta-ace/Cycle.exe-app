package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CycleContainer
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.UserProfile
import com.example.data.repository.SyncState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FeedViewModel(private val container: CycleContainer) : ViewModel() {
    private val communityRepo = container.communityRepository
    private val uploadRepo = container.imageUploadRepository

    private val _isRefreshing = MutableStateFlow(false)
    private val _manualError = MutableStateFlow<String?>(null)
    private val _serverOverride = MutableStateFlow<List<Post>?>(null)

    val postsState: StateFlow<SyncState<Post>> = combine(
        communityRepo.observePostsState(),
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

    private val _userLikes = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val userLikes: StateFlow<Map<String, Boolean>> = _userLikes.asStateFlow()

    private val _commentsMap = MutableStateFlow<Map<String, List<PostComment>>>(emptyMap())
    val commentsMap: StateFlow<Map<String, List<PostComment>>> = _commentsMap.asStateFlow()

    private val _expandedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedPostIds: StateFlow<Set<String>> = _expandedPostIds.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Int?>(null)
    val uploadProgress: StateFlow<Int?> = _uploadProgress.asStateFlow()

    private val _feedMessage = MutableStateFlow<String?>(null)
    val feedMessage: StateFlow<String?> = _feedMessage.asStateFlow()

    private val likeJobs = mutableMapOf<String, Job>()
    private val commentJobs = mutableMapOf<String, Job>()

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _manualError.value = null
            val res = communityRepo.fetchPostsFromServer()
            res.onSuccess { list ->
                _serverOverride.value = list
            }.onFailure { e ->
                _manualError.value = e.localizedMessage ?: "Failed to sync community feed from server."
            }
            _isRefreshing.value = false
        }
    }

    fun observeLikesForPosts(postIds: List<String>, uid: String) {
        for (postId in postIds) {
            if (!likeJobs.containsKey(postId)) {
                likeJobs[postId] = viewModelScope.launch {
                    communityRepo.observePostLikedByUser(postId, uid).collect { liked ->
                        _userLikes.update { it + (postId to liked) }
                    }
                }
            }
        }
    }

    fun toggleComments(postId: String) {
        val currentlyOpen = _expandedPostIds.value.contains(postId)
        _expandedPostIds.update { if (currentlyOpen) it - postId else it + postId }
        if (!currentlyOpen && !commentJobs.containsKey(postId)) {
            commentJobs[postId] = viewModelScope.launch {
                communityRepo.observeComments(postId).collect { list ->
                    _commentsMap.update { it + (postId to list) }
                }
            }
        }
    }

    fun createPost(
        uid: String,
        profile: UserProfile?,
        fallbackName: String,
        fallbackPhoto: String,
        isSuperAdmin: Boolean,
        isAdmin: Boolean,
        content: String,
        photoUris: List<Uri>,
        onSuccess: () -> Unit
    ) {
        if (content.isBlank() && photoUris.isEmpty()) {
            _feedMessage.value = "Please enter text or attach at least one image."
            return
        }
        viewModelScope.launch {
            _isPosting.value = true
            _feedMessage.value = null
            try {
                val uploadedUrls = mutableListOf<String>()
                if (photoUris.isNotEmpty()) {
                    val perPhotoMax = 450_000 / photoUris.size
                    photoUris.forEachIndexed { index, uri ->
                        val url = uploadRepo.uploadImageUri(
                            uri = uri,
                            folder = "posts",
                            maxBytes = perPhotoMax,
                            onProgress = { pct ->
                                val totalPct = (((index + pct / 100f) / photoUris.size) * 95f).toInt()
                                _uploadProgress.value = totalPct.coerceIn(5, 98)
                            }
                        )
                        uploadedUrls.add(url)
                    }
                }
                val authorTitle = profile?.customTitle?.ifBlank { null } ?: when {
                    isSuperAdmin -> "Super Admin"
                    isAdmin -> "Admin"
                    else -> "Admin Member"
                }
                val res = communityRepo.createPost(
                    authorId = uid,
                    authorName = profile?.displayName?.ifBlank { null } ?: fallbackName,
                    authorPhotoURL = profile?.photoURL?.ifBlank { null } ?: fallbackPhoto,
                    authorTitle = authorTitle,
                    content = content,
                    photoURLs = uploadedUrls
                )
                res.onSuccess {
                    onSuccess()
                }.onFailure { e ->
                    _feedMessage.value = e.message ?: "Failed to publish post."
                }
            } catch (e: Exception) {
                _feedMessage.value = e.message ?: "Image upload failed."
            } finally {
                _uploadProgress.value = null
                _isPosting.value = false
            }
        }
    }

    fun updatePost(postId: String, content: String, onDone: () -> Unit) {
        viewModelScope.launch {
            communityRepo.updatePost(postId, content)
                .onSuccess { onDone() }
                .onFailure { e -> _feedMessage.value = e.message ?: "Failed to update post." }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            communityRepo.deletePost(postId)
                .onFailure { e -> _feedMessage.value = e.message ?: "Failed to delete post." }
        }
    }

    fun toggleHiddenPost(post: Post) {
        viewModelScope.launch {
            communityRepo.toggleHiddenPost(post.id, !post.isHidden)
                .onFailure { e -> _feedMessage.value = e.message ?: "Failed to update visibility." }
        }
    }

    fun toggleLike(post: Post, uid: String) {
        val liked = _userLikes.value[post.id] == true
        viewModelScope.launch {
            communityRepo.toggleLike(post, uid, liked)
                .onFailure { e -> _feedMessage.value = e.message ?: "Could not update like." }
        }
    }

    fun addComment(
        post: Post,
        uid: String,
        authorName: String,
        authorPhotoURL: String,
        text: String,
        parentComment: PostComment? = null,
        onDone: () -> Unit
    ) {
        if (text.isBlank()) return
        viewModelScope.launch {
            communityRepo.addComment(post, uid, authorName, authorPhotoURL, text, parentComment)
                .onSuccess { onDone() }
                .onFailure { e -> _feedMessage.value = e.message ?: "Failed to add comment." }
        }
    }

    fun editComment(postId: String, commentId: String, text: String, onDone: () -> Unit) {
        if (text.isBlank()) return
        viewModelScope.launch {
            communityRepo.editComment(postId, commentId, text)
                .onSuccess { onDone() }
                .onFailure { e -> _feedMessage.value = e.message ?: "Failed to edit comment." }
        }
    }

    fun deleteComment(post: Post, comment: PostComment) {
        viewModelScope.launch {
            communityRepo.deleteComment(post, comment)
                .onFailure { e -> _feedMessage.value = e.message ?: "Failed to delete comment." }
        }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        targetAuthorId: String,
        targetExcerpt: String,
        reporterId: String,
        reporterEmail: String,
        reason: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            communityRepo.submitReport(
                targetType = targetType,
                targetId = targetId,
                targetAuthorId = targetAuthorId,
                targetExcerpt = targetExcerpt,
                reporterId = reporterId,
                reporterEmail = reporterEmail,
                reason = reason
            ).onSuccess {
                _feedMessage.value = "Report submitted to moderators."
                onSuccess()
            }.onFailure { e ->
                _feedMessage.value = e.message ?: "Failed to submit report."
            }
        }
    }

    fun clearMessage() {
        _feedMessage.value = null
    }

    companion object {
        fun factory(container: CycleContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FeedViewModel(container) as T
            }
        }
    }
}
