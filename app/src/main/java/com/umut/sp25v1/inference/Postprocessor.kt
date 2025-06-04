package com.umut.sp25v1.inference

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun drawTFLiteDetectionsWithInfo(original: Bitmap, outputs: FloatArray): DetectionInfo {
    val boxed = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(boxed)
    val numBoxes = outputs.size / 6

    val detections = mutableListOf<Detection>()

    val classThresholds = mapOf(
        0 to 0.5f,
        1 to 0.01f
    )


    for (i in 0 until numBoxes) {
        val base = i * 6

        if (base + 6 >= outputs.size) continue // ✅ Güvenlik kontrolü

        Log.d("TFLiteOutput", "Class raw value: ${outputs[base + 5]}")

        val cx = outputs[base + 0] * original.width
        val cy = outputs[base + 1] * original.height
        val w = outputs[base + 2] * original.width
        val h = outputs[base + 3] * original.height
        val objConf = outputs[base + 4]
        val cls = outputs[base + 5].toInt()

        if (cls !in CLASS_NAMES.indices) continue

        val threshold = classThresholds[cls] ?: 0.1f
        if (objConf < threshold) {
            Log.d("TFLiteSkip", "Conf $objConf too low for class $cls")
            continue
        }
        Log.d("TFLiteOutput", "Raw outputs: ${outputs.slice(base until base+6)}")

        val x1 = (cx - w / 2f).coerceIn(0f, original.width.toFloat())
        val y1 = (cy - h / 2f).coerceIn(0f, original.height.toFloat())
        val x2 = (cx + w / 2f).coerceIn(0f, original.width.toFloat())
        val y2 = (cy + h / 2f).coerceIn(0f, original.height.toFloat())

        val minBoxSize = 40f
        if ((x2 - x1) < minBoxSize || (y2 - y1) < minBoxSize) continue

        detections.add(Detection(x1, y1, x2, y2, objConf, cls))


        Log.d("TFLiteDebug", "outputs.size = ${outputs.size}")


    }

    val filtered = nonMaxSuppression(detections, iouThreshold = 0.5f)
        .sortedByDescending { it.score }
        .take(10)

    val classList = filtered.map { CLASS_NAMES[it.classId] }
    val confList = filtered.map { it.score }

    for (det in filtered) {
        val classColor = when (det.classId) {
            0 -> Color(0xFFFF0000)
            1 -> Color(0xFFFFFF00)
            else -> Color(0xFFFF00FF)
        }

        val paint = Paint().apply {
            color = classColor.toArgb()
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 48f
            isAntiAlias = true
        }

        val label = "${CLASS_NAMES[det.classId]}: %.2f".format(det.score)
        val textWidth = textPaint.measureText(label)
        val textHeight = textPaint.textSize

        val backgroundPaint = Paint().apply {
            color = android.graphics.Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        }

        canvas.drawRect(RectF(det.x1, det.y1, det.x2, det.y2), paint)
        val bgRect = RectF(det.x1, det.y1, det.x1 + textWidth + 16f, det.y1 + textHeight + 16f)
        canvas.drawRect(bgRect, backgroundPaint)
        canvas.drawText(label, det.x1 + 8f, det.y1 + textHeight, textPaint)


    }



    return DetectionInfo(
        bitmap = boxed,
        classes = classList,
        confidences = confList,
        objectCount = filtered.size
    )
}

fun iou(a: Detection, b: Detection): Float {
    val areaA = (a.x2 - a.x1).coerceAtLeast(0f) * (a.y2 - a.y1).coerceAtLeast(0f)
    val areaB = (b.x2 - b.x1).coerceAtLeast(0f) * (b.y2 - b.y1).coerceAtLeast(0f)

    val interX1 = maxOf(a.x1, b.x1)
    val interY1 = maxOf(a.y1, b.y1)
    val interX2 = minOf(a.x2, b.x2)
    val interY2 = minOf(a.y2, b.y2)

    val interArea = (interX2 - interX1).coerceAtLeast(0f) * (interY2 - interY1).coerceAtLeast(0f)
    return interArea / (areaA + areaB - interArea + 1e-6f)
}

fun nonMaxSuppression(detections: List<Detection>, iouThreshold: Float): List<Detection> {
    val output = mutableListOf<Detection>()
    val sorted = detections.sortedByDescending { it.score }.toMutableList()

    while (sorted.isNotEmpty()) {
        val best = sorted.removeAt(0)
        output.add(best)

        sorted.removeAll { other ->
            val overlap = iou(best, other)
            overlap > iouThreshold && best.classId == other.classId
        }
    }

    return output
}