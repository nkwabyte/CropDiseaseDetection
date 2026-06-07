package com.nkwabyte.cropdiseasedetection.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.get

fun saveBitmapToFile(context: Context, bitmap: Bitmap): File {
    val file = File(context.filesDir, "detection_annotated_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val stream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(stream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


fun preprocessBitmap(bitmap: Bitmap): FloatArray {
    val resized = bitmap.scale(640, 640)
    val floatArray = FloatArray(640 * 640 * 3)
    var index = 0
    for (y in 0 until 640) {
        for (x in 0 until 640) {
            val pixel = resized[x, y]
            floatArray[index++] = ((pixel shr 16 and 0xFF) / 255.0f - 0.485f) / 0.229f
            floatArray[index++] = ((pixel shr 8 and 0xFF) / 255.0f - 0.456f) / 0.224f
            floatArray[index++] = ((pixel and 0xFF) / 255.0f - 0.406f) / 0.225f
        }
    }
    return floatArray
}