package com.umut.sp25v1.viewmodelfactory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.umut.sp25v1.data.repository.DetectionRepository
import com.umut.sp25v1.viewmodel.DetectionViewModel
import org.tensorflow.lite.Interpreter

class DetectionViewModelFactory(
    private val repository: DetectionRepository,
    private val tflite: Interpreter
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetectionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetectionViewModel(repository, tflite) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
