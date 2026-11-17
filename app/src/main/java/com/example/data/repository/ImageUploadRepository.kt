package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlin.math.max

class ImageUploadRepository(
    private val context: Context,
    private val firebase: FirebaseProvider
) {
    suspend fun uploadImageUri(
        uri: Uri,
        folder: String,
        maxBytes: Int = 450_000,
        onProgress: (Int) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        onProgress(10)
        val compressedBytes = compressUriToJpeg(uri, maxDimension = 1280, maxBytes = maxBytes)
            ?: throw IllegalStateException("Could not read or decode selected image.")

        onProgress(25)
        try {
            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val ref = firebase.storage.reference.child("$folder/$fileName")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            val uploadTask = ref.putBytes(compressedBytes, metadata)
            uploadTask.addOnProgressListener { snap ->
                val total = snap.totalByteCount
                if (total > 0) {
                    val pct = 25 + ((snap.bytesTransferred.toFloat() / total.toFloat()) * 65f).toInt()
                    onProgress(pct.coerceIn(25, 92))
                }
            }
            uploadTask.await()
            val url = ref.downloadUrl.await().toString()
            onProgress(100)
            url
        } catch (_: Exception) {
            // Graceful Base64 data URI fallback if Storage upload fails or offline
            onProgress(70)
            val fallbackBytes = if (compressedBytes.size > 320_000) {
                compressUriToJpeg(uri, maxDimension = 800, maxBytes = 280_000) ?: compressedBytes
            } else {
                compressedBytes
            }
            val encoded = Base64.encodeToString(fallbackBytes, Base64.NO_WRAP)
            onProgress(100)
            "data:image/jpeg;base64,$encoded"
        }
    }

    private fun compressUriToJpeg(
        uri: Uri,
        maxDimension: Int,
        maxBytes: Int
    ): ByteArray? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        val largest = max(bounds.outWidth, bounds.outHeight)
        while (largest / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        val scaledBitmap = if (max(bitmap.width, bitmap.height) > maxDimension) {
            val ratio = maxDimension.toFloat() / max(bitmap.width, bitmap.height).toFloat()
            val targetW = max(1, (bitmap.width * ratio).toInt())
            val targetH = max(1, (bitmap.height * ratio).toInt())
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true).also {
                if (it != bitmap) bitmap.recycle()
            }
        } else {
            bitmap
        }

        return try {
            var quality = 86
            var result: ByteArray
            val out = ByteArrayOutputStream()
            do {
                out.reset()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                result = out.toByteArray()
                quality -= 10
            } while (result.size > maxBytes && quality >= 30)
            result
        } finally {
            scaledBitmap.recycle()
        }
    }
}
