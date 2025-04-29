package com.umut.sp25v1

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Utils {
    /**
     * assets içindeki assetName dosyasını
     * context.filesDir altına kopyalar ve
     * gerçek dosya yolunu döner.
     */
    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String): String {
        val outFile = File(context.filesDir, assetName)
        if (outFile.exists() && outFile.length() > 0) {
            return outFile.absolutePath
        }
        context.assets.open(assetName).use { input ->
            FileOutputStream(outFile).use { output ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
        return outFile.absolutePath
    }
}
