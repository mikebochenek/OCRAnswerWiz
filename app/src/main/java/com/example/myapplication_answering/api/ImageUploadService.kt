package com.example.myapplication_answering.api

import android.content.Context
import android.net.Uri
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ImageUploadService(
    // TODO: Make backend URL configurable (e.g., via Datastore, SharedPreferences, or Remote Config)
    private val baseUrl: String = DEFAULT_BASE_URL
) {
    companion object {
        // TODO: Update this default URL to the actual production backend server endpoint when available.
        // http://10.0.2.2:8088/ maps localhost:8088 on host machine from the Android Emulator.
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8088/"
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: ImageUploadApi by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ImageUploadApi::class.java)
    }

    suspend fun uploadImage(context: Context, uri: Uri): Result<ImageUploadResponse> {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Unable to open input stream for Uri: $uri"))

            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) {
                return Result.failure(Exception("Image file at $uri is empty"))
            }

            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("image", "uploaded_image.jpg", requestBody)

            Log.d("ImageUploadService", "Uploading image ($mimeType, ${bytes.size} bytes) to $baseUrl...")
            val response = api.uploadImage(filePart)

            if (response.isSuccessful && response.body() != null) {
                val resultBody = response.body()!!
                Log.d("ImageUploadService", "Image upload successful: $resultBody")
                Result.success(resultBody)
            } else {
                val errorMsg = "Upload failed with HTTP status ${response.code()}: ${response.errorBody()?.string()}"
                Log.e("ImageUploadService", errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ImageUploadService", "Image upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
