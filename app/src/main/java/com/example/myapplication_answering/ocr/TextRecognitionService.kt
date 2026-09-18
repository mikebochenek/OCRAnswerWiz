package com.example.myapplication_answering.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import android.util.Log
import kotlinx.coroutines.tasks.await

class TextRecognitionService(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun extractText(uri: Uri): Result<String> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            Log.d("TextRecognitionService", "Extracted text: ${result.text}")
            Result.success(result.text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractQuestionAndOptions(uri: Uri): Result<OcrResult> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            Log.d("TextRecognitionService", "Extracted text: ${result.text}")
            
            // Heuristic to split text into question and options
            val lines = result.text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) return Result.failure(Exception("No text found"))

            // Try to find where the question ends - either first line ending with '?' or first line before an option-like pattern
            val optionPattern = Regex("^[A-D][).]|\\d+[).]|[-•]")
            
            val questionLines = mutableListOf<String>()
            var foundOptions = false
            var questionLinesCount = 0
            
            for (line in lines) {
                if (optionPattern.containsMatchIn(line)) {
                    foundOptions = true
                    break
                }
                questionLines.add(line)
                questionLinesCount++
            }
            
            val question = if (questionLines.isNotEmpty()) questionLines.joinToString(" ") else lines.first()
            val options = if (foundOptions) {
                lines.subList(questionLinesCount, lines.size)
            } else {
                if (lines.size > 1) lines.drop(1) else emptyList()
            }

            Result.success(OcrResult(question, options, result.text))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class OcrResult(
    val question: String,
    val options: List<String>,
    val fullText: String
)
