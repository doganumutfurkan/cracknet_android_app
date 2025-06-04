package com.umut.sp25v1.ui.screens.detection
import com.umut.sp25v1.data.model.TempDetection

data class DetectionUiState(
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val resultImagePath: String? = null,
    val tempDetections: List<TempDetection> = emptyList(),
    val shouldNavigate: Boolean = false
)
