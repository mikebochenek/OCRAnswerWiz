package com.example.myapplication_answering.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ImageUploadResponse(
    @Json(name = "filename") val filename: String? = null,
    @Json(name = "contentType") val contentType: String? = null,
    @Json(name = "sizeBytes") val sizeBytes: Long? = null
)
