package com.umut.sp25v1.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umut.sp25v1.data.model.DetectionResult
import com.umut.sp25v1.data.model.TempDetection
import com.umut.sp25v1.data.repository.DetectionRepository
import com.umut.sp25v1.inference.drawTFLiteDetectionsWithInfo
import com.umut.sp25v1.inference.preprocessTFLite
import com.umut.sp25v1.ui.screens.detection.DetectionUiState
import com.umut.sp25v1.utils.saveBitmapToInternalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter

class DetectionViewModel(
    private val repository: DetectionRepository,
    private val tflite: Interpreter
) : ViewModel() {

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri

    private val _uiState = MutableStateFlow(DetectionUiState())
    val uiState: StateFlow<DetectionUiState> = _uiState

    private val _selectedDetection = MutableStateFlow<DetectionResult?>(null)
    val selectedDetection: StateFlow<DetectionResult?> = _selectedDetection

    private val _editDetection = MutableStateFlow<DetectionResult?>(null)
    val editDetection: StateFlow<DetectionResult?> = _editDetection

    private val _detectionList = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detectionList: StateFlow<List<DetectionResult>> = _detectionList

    init {
        refreshDetections()
    }

    fun refreshDetections() {
        viewModelScope.launch {
            _detectionList.value = repository.getAllDetections()
        }
    }


    fun setSelectedImageUri(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun runDetection(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

                val inputStream = context.contentResolver.openInputStream(uri)
                val sourceBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (sourceBitmap == null) {
                    _uiState.update { it.copy(errorMessage = "Görsel okunamadı", isProcessing = false) }
                    return@launch
                }

                val input = preprocessTFLite(sourceBitmap, 640)
                val output = Array(1) { Array(6) { FloatArray(8400) } }
                tflite.run(input, output)

                val flatOutput = FloatArray(6 * 8400)
                for (i in 0 until 8400) {
                    for (j in 0 until 6) {
                        flatOutput[i * 6 + j] = output[0][j][i]
                    }
                }

                val result = drawTFLiteDetectionsWithInfo(sourceBitmap, flatOutput)
                val savedPath = saveBitmapToInternalStorage(context, result.bitmap)

                val tempDetections = result.classes.zip(result.confidences).map { (label, conf) ->
                    TempDetection(label, conf)
                }

                _uiState.update {
                    it.copy(
                        resultImagePath = savedPath,
                        tempDetections = tempDetections,
                        isProcessing = false,
                        shouldNavigate = true
                    )
                }

            } catch (e: Exception) {
                Log.e("DetectionViewModel", "Detection error", e)
                _uiState.update {
                    it.copy(errorMessage = e.message, isProcessing = false)
                }
            }
        }
    }

    fun loadDetectionById(id: Long) {
        viewModelScope.launch {
            val detection = repository.getDetectionById(id)
            _selectedDetection.value = detection
        }
    }

    fun deleteDetection(detection: DetectionResult, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDetection(detection)
            refreshDetections()
            onComplete()
        }
    }

    fun loadDetectionForEdit(id: Long) {
        viewModelScope.launch {
            _editDetection.value = repository.getDetectionById(id)
        }
    }

    fun updateDetection(updated: DetectionResult, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateDetection(updated)
            refreshDetections()
            onComplete()
        }
    }

    fun insertDetection(detection: DetectionResult, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                repository.insertDetection(detection)
                refreshDetections()
                onSuccess()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun resetNavigation() {
        _uiState.update { it.copy(shouldNavigate = false) }
    }
}
