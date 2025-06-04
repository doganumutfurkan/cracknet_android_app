package com.umut.sp25v1.inference

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun preprocessTFLite(bitmap: Bitmap, size: Int): ByteBuffer {
    val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
    val inputBuffer = ByteBuffer.allocateDirect(1 * size * size * 3 * 4)
    inputBuffer.order(ByteOrder.nativeOrder())
    val intValues = IntArray(size * size)
    resized.getPixels(intValues, 0, size, 0, 0, size, size)
    for (pixel in intValues) {
        val r = (pixel shr 16 and 0xFF).toFloat() / 255f
        val g = (pixel shr 8 and 0xFF).toFloat() / 255f
        val b = (pixel and 0xFF).toFloat() / 255f

        inputBuffer.putFloat(r)
        inputBuffer.putFloat(g)
        inputBuffer.putFloat(b)
    }
    return inputBuffer
}