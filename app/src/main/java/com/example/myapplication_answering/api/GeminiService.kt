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
            val answerText = response.text ?: "No response from Gemini"
            Log.d("GeminiService", "Answer/explanation returned from Gemini API:\n$answerText")
            Result.success(answerText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
