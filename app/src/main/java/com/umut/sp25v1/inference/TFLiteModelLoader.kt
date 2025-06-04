package com.umut.sp25v1.inference

import com.umut.sp25v1.MyApp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

fun loadTFLiteModel(): MappedByteBuffer {
    val assetManager = MyApp.instance.assets
    val fileDescriptor = assetManager.openFd("tflite_model/final_float32.tflite")
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}