package com.example.data.repository

import android.util.Log
import com.example.data.model.GalleryPhoto
import com.example.data.repository.FirestoreMappers.toGalleryPhotoSafe
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

class GalleryRepository(private val firebase: FirebaseProvider) {
    private val db = firebase.db

    private fun sortPhotos(list: List<GalleryPhoto>): List<GalleryPhoto> {
        return list.sortedByDescending { it.uploadedAt }
    }

    fun observeGalleryPhotosState(): Flow<SyncState<GalleryPhoto>> = callbackFlow {
        val listener = db.collection("gallery")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "gallery")
                    close(error)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toGalleryPhotoSafe() } ?: emptyList()
                val sorted = sortPhotos(list)
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
            Log.w("GalleryRepository", "Gallery listener error: ${cause.message}", cause)
            emit(
                SyncState(
                    items = emptyList(),
                    isLoading = false,
                    error = cause.localizedMessage ?: "Unable to sync gallery from server."
                )
            )
        } else {
            throw e
        }
    }

    suspend fun fetchGalleryFromServer(): Result<List<GalleryPhoto>> {
        return try {
            val snap = db.collection("gallery").get(Source.SERVER).await()
            val list = snap.documents.mapNotNull { it.toGalleryPhotoSafe() }
            Result.success(sortPhotos(list))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "gallery")
            Result.failure(e)
        }
    }

    suspend fun addGalleryPhoto(
        title: String,
        description: String,
        photoURL: String,
        uploadedBy: String,
        uploadedByName: String
    ): Result<Unit> {
        val payload = mapOf(
            "title" to title.trim(),
            "description" to description.trim(),
            "photoURL" to photoURL,
            "uploadedBy" to uploadedBy,
            "uploadedByName" to uploadedByName,
            "uploadedAt" to DateUtils.nowIsoString()
        )
        return try {
            db.collection("gallery").add(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "gallery")
            Result.failure(e)
        }
    }

    suspend fun deleteGalleryPhoto(photoId: String): Result<Unit> {
        return try {
            db.collection("gallery").document(photoId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "gallery/$photoId")
            Result.failure(e)
        }
    }
}
