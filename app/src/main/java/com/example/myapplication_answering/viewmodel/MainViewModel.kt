package com.example.myapplication_answering.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication_answering.api.GeminiService
import com.example.myapplication_answering.ocr.OcrResult
import com.example.myapplication_answering.ocr.TextRecognitionService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val textRecognitionService = TextRecognitionService(application)
    private val geminiService = GeminiService()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

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
                
                // Trigger Gemini answer
                val geminiResult = geminiService.getAnswer(result.question, result.options)
                geminiResult.onSuccess { answer ->
                    _uiState.value = UiState.Success(result, answer)
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Gemini error", result)
                }
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "OCR error")
            }
        }
    }

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val result: OcrResult, val geminiAnswer: String?) : UiState()
        data class Error(val message: String, val result: OcrResult? = null) : UiState()
    }
}
