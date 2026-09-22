package com.example.myapplication_answering.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication_answering.api.GeminiService
import com.example.myapplication_answering.ocr.OcrResult
import com.example.myapplication_answering.ocr.TextRecognitionService
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val textRecognitionService = TextRecognitionService(application)
    private val geminiService = GeminiService()
    private val sharedPreferences = application.getSharedPreferences("ocr_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

    private val _answerCount = MutableStateFlow(sharedPreferences.getInt("successful_answers_count", 0))
    val answerCount: StateFlow<Int> = _answerCount.asStateFlow()

    private fun incrementAnswerCount() {
        val newCount = _answerCount.value + 1
        _answerCount.value = newCount
        sharedPreferences.edit().putInt("successful_answers_count", newCount).apply()
    }

    fun onImageSelected(uri: Uri?) {
        _imageUri.value = uri
        if (uri != null) {
            Log.d("MainViewModel", "Image selected/captured: $uri at timestamp: ${System.currentTimeMillis()}")
            processImage(uri)
        }
    }

    private fun processImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val ocrResult = textRecognitionService.extractQuestionAndOptions(uri)
            ocrResult.onSuccess { result ->
                _uiState.value = UiState.Success(result, null)
                fetchGeminiAnswer(result)
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "OCR error")
            }
        }
    }

    fun retry() {
        val currentState = _uiState.value
        if (currentState is UiState.Error) {
            val result = currentState.result
            if (result != null) {
                _uiState.value = UiState.Success(result, null)
                viewModelScope.launch {
                    fetchGeminiAnswer(result)
                }
            } else {
                _imageUri.value?.let { processImage(it) }
            }
        } else if (currentState is UiState.Success) {
            val result = currentState.result
            _uiState.value = UiState.Success(result, null)
            viewModelScope.launch {
                fetchGeminiAnswer(result)
            }
        }
    }

    private suspend fun fetchGeminiAnswer(result: OcrResult) {
        val geminiResult = geminiService.getAnswer(result.question, result.options)
        geminiResult.onSuccess { answer ->
            _uiState.value = UiState.Success(result, answer)
            incrementAnswerCount()
        }.onFailure { error ->
            _uiState.value = UiState.Error(error.message ?: "Gemini error", result)
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val result: OcrResult, val geminiAnswer: String?) : UiState()
        data class Error(val message: String, val result: OcrResult? = null) : UiState()
    }
}
