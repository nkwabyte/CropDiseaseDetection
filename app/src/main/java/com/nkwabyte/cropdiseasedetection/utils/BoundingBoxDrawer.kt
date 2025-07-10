package com.nkwabyte.cropdiseasedetection.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.nkwabyte.cropdiseasedetection.model.DetectionResult


fun drawBoundingBoxesOnBitmap(
    original: Bitmap,
    results: List<DetectionResult>,
    labels: List<String>
): Bitmap {
    val mutableBitmap = original.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)
    val paint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    val textPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 32f
        style = Paint.Style.FILL
    }

    for (result in results) {
        val (left, top, right, bottom) = result.box
        canvas.drawRect(left, top, right, bottom, paint)
        canvas.drawText("${labels[result.classIndex]}: ${(result.score * 100).toInt()}%", left, top - 10, textPaint)
    }

    return mutableBitmap
}
