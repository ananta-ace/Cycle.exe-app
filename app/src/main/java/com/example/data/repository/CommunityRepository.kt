package com.example.data.repository

import android.util.Log
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.repository.FirestoreMappers.toPostCommentSafe
import com.example.data.repository.FirestoreMappers.toPostSafe
import com.example.util.DateUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class CommunityRepository(private val firebase: FirebaseProvider) {
    private val db = firebase.db

    private fun sortPosts(items: List<Post>): List<Post> =
        items.sortedByDescending { it.createdAt }

    fun observePostsState(): Flow<SyncState<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "posts")
                    close(error)
                    return@addSnapshotListener
                }
                val items = snap?.documents?.mapNotNull { it.toPostSafe() } ?: emptyList()
                val sorted = sortPosts(items)
                val fromCache = snap?.metadata?.isFromCache == true
                val stillWaitingServer = fromCache && sorted.isEmpty()
                trySend(
                    SyncState(
                        items = sorted,
                        isLoading = stillWaitingServer,
                        error = null,
                        isFromCache = fromCache
                    )
                )
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) {
            Log.w("CommunityRepository", "Posts listener error: ${cause.message}", cause)
            emit(
                SyncState(
                    items = emptyList(),
                    isLoading = false,
                    error = cause.localizedMessage ?: "Unable to sync feed posts from server."
                )
            )
        } else {
            throw e
        }
    }

    suspend fun fetchPostsFromServer(): Result<List<Post>> {
        return try {
            val snap = db.collection("posts").get(Source.SERVER).await()
            val items = snap.documents.mapNotNull { it.toPostSafe() }
            Result.success(sortPosts(items))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "posts")
            Result.failure(e)
        }
    }

    fun observePostLikedByUser(postId: String, uid: String): Flow<Boolean> = callbackFlow {
        val listener = db.collection("posts").document(postId)
            .collection("likes").document(uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }
                trySend(snap?.exists() == true)
            }
        awaitClose { listener.remove() }
    }

    fun observeComments(postId: String): Flow<List<PostComment>> = callbackFlow {
        val listener = db.collection("posts").document(postId)
            .collection("comments")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "posts/$postId/comments")
                    close(error)
                    return@addSnapshotListener
                }
                val items = (snap?.documents?.mapNotNull { it.toPostCommentSafe() } ?: emptyList())
                    .sortedBy { it.createdAt }
                trySend(items)
            }
        awaitClose { listener.remove() }
    }.catch { e ->
        val cause = (e as? CancellationException)?.cause ?: e
        if (cause is FirebaseFirestoreException) {
            emit(emptyList())
        } else {
            throw e
        }
    }

    suspend fun createPost(
        authorId: String,
        authorName: String,
        authorPhotoURL: String,
        authorTitle: String,
        content: String,
        photoURLs: List<String>
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        val payload = mapOf(
            "authorId" to authorId,
            "authorName" to authorName,
            "authorPhotoURL" to authorPhotoURL,
            "authorTitle" to authorTitle,
            "content" to content.trim().take(2000),
            "photoURLs" to photoURLs,
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "isHidden" to false,
            "createdAt" to now,
            "updatedAt" to now
        )
        return try {
            db.collection("posts").add(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts")
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, content: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId).update(
                mapOf(
                    "content" to content.trim().take(2000),
                    "updatedAt" to DateUtils.nowIsoString()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun toggleHiddenPost(postId: String, hidden: Boolean): Result<Unit> {
        return try {
            db.collection("posts").document(postId).update(
                mapOf("isHidden" to hidden)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun toggleLike(post: Post, uid: String, currentlyLiked: Boolean): Result<Unit> {
        val likeRef = db.collection("posts").document(post.id).collection("likes").document(uid)
        val postRef = db.collection("posts").document(post.id)
        return try {
            if (currentlyLiked) {
                likeRef.delete().await()
                postRef.update(mapOf("likesCount" to maxOf(0L, post.likesCount - 1))).await()
            } else {
                likeRef.set(
                    mapOf(
                        "postId" to post.id,
                        "userId" to uid,
                        "createdAt" to DateUtils.nowIsoString()
                    )
                ).await()
                postRef.update(mapOf("likesCount" to post.likesCount + 1)).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "posts/${post.id}/likes/$uid")
            Result.failure(e)
        }
    }

    suspend fun addComment(
        post: Post,
        authorId: String,
        authorName: String,
        authorPhotoURL: String,
        text: String,
        parentComment: PostComment? = null
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        val payload = hashMapOf<String, Any?>(
            "postId" to post.id,
            "parentId" to parentComment?.id,
            "replyToAuthor" to parentComment?.authorName,
            "authorId" to authorId,
            "authorName" to authorName,
            "authorPhotoURL" to authorPhotoURL,
            "text" to text.trim().take(1000),
            "replyCount" to 0L,
            "isReported" to false,
            "isDeleted" to false,
            "createdAt" to now,
            "updatedAt" to now
        )
        return try {
            db.collection("posts").document(post.id).collection("comments").add(payload).await()
            if (parentComment != null) {
                db.collection("posts").document(post.id).collection("comments")
                    .document(parentComment.id)
                    .update(
                        mapOf(
                            "replyCount" to parentComment.replyCount + 1,
                            "updatedAt" to now
                        )
                    ).await()
            }
            db.collection("posts").document(post.id).update(
                mapOf("commentsCount" to post.commentsCount + 1)
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments")
            Result.failure(e)
        }
    }

    suspend fun editComment(postId: String, commentId: String, text: String): Result<Unit> {
        return try {
            db.collection("posts").document(postId).collection("comments").document(commentId)
                .update(
                    mapOf(
                        "text" to text.trim().take(1000),
                        "updatedAt" to DateUtils.nowIsoString()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId/comments/$commentId")
            Result.failure(e)
        }
    }

    suspend fun deleteComment(post: Post, comment: PostComment): Result<Unit> {
        return try {
            db.collection("posts").document(post.id).collection("comments").document(comment.id)
                .delete().await()
            if (post.commentsCount > 0) {
                db.collection("posts").document(post.id).update(
                    mapOf("commentsCount" to maxOf(0L, post.commentsCount - 1))
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/${post.id}/comments/${comment.id}")
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        targetAuthorId: String,
        targetExcerpt: String,
        reporterId: String,
        reporterEmail: String,
        reason: String
    ): Result<Unit> {
        val payload = mapOf(
            "targetType" to targetType,
            "targetId" to targetId,
            "targetAuthorId" to targetAuthorId,
            "targetExcerpt" to targetExcerpt.take(300),
            "reporterId" to reporterId,
            "reporterEmail" to reporterEmail,
            "reason" to reason,
            "status" to "PENDING",
            "createdAt" to DateUtils.nowIsoString()
        )
        return try {
            db.collection("reports").add(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reports")
            Result.failure(e)
        }
    }
}
