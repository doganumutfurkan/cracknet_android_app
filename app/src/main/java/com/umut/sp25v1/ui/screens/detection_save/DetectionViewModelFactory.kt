package com.umut.sp25v1.ui.screens.detection_save

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.umut.sp25v1.data.dao.DetectionResultDao

class DetectionViewModelFactory(
    private val application: Application,
    private val dao: DetectionResultDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetectionViewModel::class.java)) {
            return DetectionViewModel(application, dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
