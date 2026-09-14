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

/** Raw bytes behind a content/file URI, or null if it cannot be read. */
fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Decodes a picked image UPRIGHT.
 *
 * The bytes are read first and decoded through [ImageOrientation] rather than
 * streamed straight into `BitmapFactory`, because BitmapFactory ignores EXIF:
 * the previous version of this function returned a sideways bitmap for every
 * portrait phone photo, and its caller then re-encoded that bitmap to JPEG,
 * destroying the orientation tag along with it.
 */
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    val bytes = readBytesFromUri(context, uri) ?: return null
    return ImageOrientation.decodeUpright(bytes)?.bitmap
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