package com.example.myapplication_answering.worker

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication_answering.api.ImageUploadService

class ImageUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uriString = inputData.getString(KEY_IMAGE_URI)
        if (uriString.isNullOrEmpty()) {
            Log.e("ImageUploadWorker", "No image URI provided for upload worker.")
            return Result.failure()
        }

        val uri = uriString.toUri()
        Log.d("ImageUploadWorker", "Starting background image upload task for Uri: $uri")

        val uploadService = ImageUploadService()
        val uploadResult = uploadService.uploadImage(applicationContext, uri)

        return if (uploadResult.isSuccess) {
            Log.d("ImageUploadWorker", "Background image upload completed successfully for $uri")
            Result.success()
        } else {
            Log.w("ImageUploadWorker", "Background image upload failed for $uri (attempt $runAttemptCount)")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val KEY_IMAGE_URI = "key_image_uri"
    }
}
