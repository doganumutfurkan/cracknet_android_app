package com.umut.sp25v1.data.repository

import com.umut.sp25v1.data.dao.DetectionResultDao
import com.umut.sp25v1.data.model.DetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DetectionRepository(private val dao: DetectionResultDao) {

    suspend fun insertDetection(detection: DetectionResult) {
        withContext(Dispatchers.IO) {
            dao.insert(detection)
        }
    }

    suspend fun updateDetection(detection: DetectionResult) {
        withContext(Dispatchers.IO) {
            dao.update(detection)
        }
    }

    suspend fun deleteDetection(detection: DetectionResult) {
        withContext(Dispatchers.IO) {
            dao.deleteDetection(detection)
        }
    }

    suspend fun getDetectionById(id: Long): DetectionResult? {
        return withContext(Dispatchers.IO) {
            dao.getById(id)
        }
    }

    suspend fun getAllDetections(): List<DetectionResult> {
        return withContext(Dispatchers.IO) {
            dao.getAllDetections()
        }
    }
}
