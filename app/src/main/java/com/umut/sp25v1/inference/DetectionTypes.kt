package com.umut.sp25v1.inference

import android.graphics.Bitmap

val CLASS_NAMES = listOf("crack", "patch")


data class Detection(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val score: Float,
    val classId: Int
)


data class DetectionInfo(
    val bitmap: Bitmap,
    val classes: List<String>,
    val confidences: List<Float>,
    val objectCount: Int
)