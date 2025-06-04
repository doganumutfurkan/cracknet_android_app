package com.umut.sp25v1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detection_results")
data class DetectionResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val imagePath: String,
    val classes: List<String>,
    val confidences: List<Float>,
    val objectCount: Int,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long,
    val userRating: Int,
    val userNote: String,
    val damageType: String //= "observation" // veya "repair"
)

data class TempDetection(
    val label: String,
    val confidence: Float
)
