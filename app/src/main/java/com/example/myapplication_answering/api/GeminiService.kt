package com.example.myapplication_answering.api

import android.util.Log
import com.example.myapplication_answering.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class GeminiService {
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.6-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun getAnswer(question: String, options: List<String>): Result<String> {
        return try {
            val prompt = if (options.isNotEmpty()) {
                "Question: $question\nOptions:\n${options.joinToString("\n")}\n\nPlease provide the correct answer and a brief explanation."
            } else {
                "Question: $question\n\nPlease provide an answer and a brief explanation."
            }

            Log.d("GeminiService", "Prompt query sent to Gemini API:\n$prompt")

            val response = generativeModel.generateContent(prompt)
            Log.d("GeminiService", "Full response from Gemini API: $response")
            val answerText = response.text ?: "No response from Gemini"
            Log.d("GeminiService", "Answer text: $answerText")
            Result.success(answerText)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error calling Gemini API. Message: ${e.message}", e)
            Result.failure(handleGeminiException(e))
        }
    }

    private fun handleGeminiException(e: Exception): Exception {
        val originalMessage = e.message ?: return e

        // The SDK error message often contains the raw JSON response when it fails to parse it.
        // We try to extract the user-friendly "message" field from it if it exists.
        val messageRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()
        val match = messageRegex.find(originalMessage)

        return if (match != null) {
            val extractedMessage = match.groupValues[1]
            Exception(extractedMessage, e)
        } else if (originalMessage.contains("503") || originalMessage.contains("UNAVAILABLE")) {
            Exception("Gemini is currently experiencing high demand (503). Please try again in a few moments.", e)
        } else if (originalMessage.contains("MissingFieldException")) {
            Exception("Gemini returned an unexpected response format. Please try again.", e)
        } else {
            e
        }
    }
}
