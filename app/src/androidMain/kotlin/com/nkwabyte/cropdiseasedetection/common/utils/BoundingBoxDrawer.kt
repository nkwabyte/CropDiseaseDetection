package com.nkwabyte.cropdiseasedetection.common.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import kotlin.math.max

fun drawBoundingBoxesOnBitmap(
    originalBitmap: Bitmap,
    results: List<DetectionResult>,
    modelWidth: Int,
    modelHeight: Int
): Bitmap {
    val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    // Calculate scaling factors
    val scaleX = originalBitmap.width.toFloat() / modelWidth
    val scaleY = originalBitmap.height.toFloat() / modelHeight

    // Box paint
    val paint = Paint().apply {
        color = Color.RED
        strokeWidth = max(originalBitmap.width, originalBitmap.height) / 200f // Dynamic stroke width
        style = Paint.Style.STROKE
    }

    // Text background paint
    val textBgPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    // Text paint
    val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = max(originalBitmap.width, originalBitmap.height) / 40f // Dynamic text size
        style = Paint.Style.FILL
    }

    for (result in results) {
        // Scale the coordinates
        val scaledBox = floatArrayOf(
            result.box[0] * scaleX,
            result.box[1] * scaleY,
            result.box[2] * scaleX,
            result.box[3] * scaleY
        )

        // Draw the bounding box
        val rectF = RectF(scaledBox[0], scaledBox[1], scaledBox[2], scaledBox[3])
        canvas.drawRect(rectF, paint)

        // Prepare the label text
        val label = "${result.className}: ${(result.score * 100).toInt()}%"

        // Draw a background for the text for better visibility
        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)
        val textBgRect = RectF(
            rectF.left,
            rectF.top - textBounds.height() - 8f, // Position above the box
            rectF.left + textBounds.width() + 8f,
            rectF.top
        )
        canvas.drawRect(textBgRect, textBgPaint)

        // Draw the text
        canvas.drawText(label, textBgRect.left + 4f, textBgRect.bottom - 4f, textPaint)
    }

    return mutableBitmap
}