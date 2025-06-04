package com.umut.sp25v1.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream

fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
    val filename = "processed_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
    Log.d("ImageSave", "Görsel kaydedildi: ${file.absolutePath}")
    return file.absolutePath
}