package com.example.data.repository

import android.util.Log
import com.example.data.model.MissionReport
import com.example.data.model.MissionSegment
import com.example.data.repository.FirestoreMappers.toMissionReportSafe
import com.example.util.DateUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class MissionsRepository(private val firebase: FirebaseProvider) {
    private val db = firebase.db

    private fun sortMissions(items: List<MissionReport>): List<MissionReport> {
        return items.sortedWith(
            compareBy<MissionReport>(
                { it.createdAt.ifBlank { "~" } },
                { it.resolvedTag().toIntOrNull() ?: Int.MAX_VALUE },
                { it.resolvedTag() }
            )
        )
    }

    fun observeMissionsState(): Flow<SyncState<MissionReport>> = callbackFlow {
        val listener = db.collection("missions")
            .addSnapshotListener(MetadataChanges.INCLUDE) { snap, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "missions")
                    close(error)
                    return@addSnapshotListener
                }
                val items = snap?.documents?.mapNotNull { it.toMissionReportSafe() } ?: emptyList()
                val sorted = sortMissions(items)
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
            Log.w("MissionsRepository", "Missions listener error: ${cause.message}", cause)
            emit(
                SyncState(
                    items = emptyList(),
                    isLoading = false,
                    error = cause.localizedMessage ?: "Unable to sync missions from server."
                )
            )
        } else {
            throw e
        }
    }

    suspend fun fetchMissionsFromServer(): Result<List<MissionReport>> {
        return try {
            val snap = db.collection("missions").get(Source.SERVER).await()
            val items = snap.documents.mapNotNull { it.toMissionReportSafe() }
            Result.success(sortMissions(items))
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "missions")
            Result.failure(e)
        }
    }

    suspend fun saveMission(
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
        createdBy: String
    ): Result<Unit> {
        val now = DateUtils.nowIsoString()
        val formattedSegments = segments.mapIndexed { idx, seg ->
            val f = seg.from.trim()
            val t = seg.to.trim()
            val label = when {
                f.isNotBlank() && t.isNotBlank() -> "$f → $t"
                seg.segment.isNotBlank() -> seg.segment.trim()
                f.isNotBlank() -> f
                else -> "Leg ${idx + 1}"
            }
            mapOf(
                "num" to (idx + 1),
                "from" to f,
                "to" to t,
                "distance" to seg.distance.trim(),
                "segment" to label
            )
        }
        val derivedStart = segments.firstOrNull()?.from?.trim().orEmpty()
        val derivedDest = segments.lastOrNull()?.to?.trim().orEmpty()
        val timeDisplay = when {
            startTime.isNotBlank() && endTime.isNotBlank() -> "$startTime · Finished $endTime"
            startTime.isNotBlank() -> startTime
            else -> ""
        }
        val fullDate = DateUtils.formatLongDate(date).ifBlank { date }
        val shortDate = DateUtils.formatShortDate(date).ifBlank { date }

        val payload = hashMapOf<String, Any>(
            "num" to tag.trim(),
            "tag" to tag.trim(),
            "name" to title.trim(),
            "title" to title.trim(),
            "date" to date.trim(),
            "startTime" to startTime.trim(),
            "endTime" to endTime.trim(),
            "time" to timeDisplay,
            "fullDate" to fullDate,
            "shortDate" to shortDate,
            "duration" to duration.trim(),
            "avgSpeed" to avgSpeed.trim(),
            "totalDist" to totalDist.trim(),
            "startLocation" to derivedStart,
            "destination" to derivedDest,
            "location" to if (derivedStart.isNotBlank() && derivedDest.isNotBlank()) "$derivedStart to $derivedDest" else (derivedStart.ifBlank { derivedDest }),
            "segments" to formattedSegments,
            "crewIds" to crewIds,
            "crewNote" to notes.trim(),
            "notes" to notes.trim(),
            "disclaimer" to notes.trim(),
            "icon" to "fa-map-signs",
            "variant" to variant,
            "isDuo" to isDuo,
            "status" to "PUBLISHED",
            "createdBy" to createdBy,
            "updatedAt" to now
        )

        if (variant == "custom" && !customColor.isNullOrBlank()) {
            payload["customColor"] = customColor
        } else if (!editingId.isNullOrBlank()) {
            payload["customColor"] = FieldValue.delete()
        }

        return try {
            if (!editingId.isNullOrBlank()) {
                db.collection("missions").document(editingId).update(payload).await()
            } else {
                payload["createdAt"] = now
                db.collection("missions").add(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "missions")
            Result.failure(e)
        }
    }

    suspend fun deleteMission(mission: MissionReport): Result<Unit> {
        return try {
            val snap = db.collection("missions").get().await()
            val foundId = snap.documents.firstOrNull { d ->
                d.id == mission.id ||
                    d.getString("num") == mission.num ||
                    d.getString("tag") == mission.tag ||
                    d.getString("name") == mission.name
            }?.id ?: mission.id
            db.collection("missions").document(foundId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "missions/${mission.id}")
            Result.failure(e)
        }
    }
}
