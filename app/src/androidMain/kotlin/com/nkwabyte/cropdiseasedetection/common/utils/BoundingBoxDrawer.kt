package com.nkwabyte.cropdiseasedetection.common.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import kotlin.math.max

val PALETTE = arrayOf(
    Color.rgb(231, 76, 60),   Color.rgb(192, 57, 43),   Color.rgb(46, 204, 113),  Color.rgb(39, 174, 96),
    Color.rgb(52, 152, 219),  Color.rgb(41, 128, 185),  Color.rgb(155, 89, 182),  Color.rgb(142, 68, 173),
    Color.rgb(243, 156, 18),  Color.rgb(230, 126, 34),  Color.rgb(26, 188, 156),  Color.rgb(22, 160, 133),
    Color.rgb(52, 73, 94),    Color.rgb(44, 62, 80),    Color.rgb(127, 140, 141), Color.rgb(149, 165, 166),
    Color.rgb(211, 84, 0),    Color.rgb(192, 57, 43),   Color.rgb(39, 174, 96),   Color.rgb(41, 128, 185),
    Color.rgb(142, 68, 173),  Color.rgb(243, 156, 18),  Color.rgb(22, 160, 133)
)

val CROP_ICONS = mapOf(
    "Corn" to "🌽",
    "Pepper" to "🫑",
    "Tomato" to "🍅"
)

fun drawBoundingBoxesOnBitmap(
    originalBitmap: Bitmap,
    results: List<DetectionResult>,
    modelWidth: Int,
    modelHeight: Int
): Bitmap {
    val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    val scaleX = originalBitmap.width.toFloat() / modelWidth
    val scaleY = originalBitmap.height.toFloat() / modelHeight

    val baseDim = minOf(originalBitmap.width, originalBitmap.height).toFloat()
    val scaledThickness = maxOf(2f, baseDim / 250f)
    val cornerLen = maxOf(10f, baseDim * 0.04f)
    val cornerThickness = scaledThickness + 2f
    val fontSize = maxOf(11f, baseDim / 45f)

    for (result in results) {
        val color = PALETTE.getOrElse(result.classIndex) { Color.RED }

        // Scale bounding box coordinates
        val scaledBox = floatArrayOf(
            result.box[0] * scaleX,
            result.box[1] * scaleY,
            result.box[2] * scaleX,
            result.box[3] * scaleY
        )
        val rectF = RectF(scaledBox[0], scaledBox[1], scaledBox[2], scaledBox[3])

        // 1. Subtle, clean semi-transparent box region fill
        val fillPaint = Paint().apply {
            this.color = color
            this.alpha = 15
            this.style = Paint.Style.FILL
        }
        canvas.drawRect(rectF, fillPaint)

        // 2. Thin bounding box border (semi-transparent)
        val borderPaint = Paint().apply {
            this.color = color
            this.alpha = 180
            this.strokeWidth = scaledThickness
            this.style = Paint.Style.STROKE
        }
        canvas.drawRect(rectF, borderPaint)

        // 3. Styled "Camera Focus / Target" Corner Brackets (L-shapes)
        val cornerPaint = Paint().apply {
            this.color = color
            this.alpha = 255
            this.strokeWidth = cornerThickness
            this.style = Paint.Style.STROKE
            this.strokeCap = Paint.Cap.ROUND
        }
        val (x1, y1, x2, y2) = floatArrayOf(rectF.left, rectF.top, rectF.right, rectF.bottom)
        // Top-left
        canvas.drawLine(x1, y1, x1 + cornerLen, y1, cornerPaint)
        canvas.drawLine(x1, y1, x1, y1 + cornerLen, cornerPaint)
        // Top-right
        canvas.drawLine(x2, y1, x2 - cornerLen, y1, cornerPaint)
        canvas.drawLine(x2, y1, x2, y1 + cornerLen, cornerPaint)
        // Bottom-left
        canvas.drawLine(x1, y2, x1 + cornerLen, y2, cornerPaint)
        canvas.drawLine(x1, y2, x1, y2 - cornerLen, cornerPaint)
        // Bottom-right
        canvas.drawLine(x2, y2, x2 - cornerLen, y2, cornerPaint)
        canvas.drawLine(x2, y2, x2, y2 - cornerLen, cornerPaint)

        // 4. Premium rounded pill text badge
        val className = result.className ?: "Unknown"
        val crop = className.split(" ").firstOrNull() ?: ""
        val icon = CROP_ICONS[crop] ?: "🌿"
        val confidencePercent = (result.score * 100).toInt()
        val label = " $icon $className  $confidencePercent% "

        val textPaint = Paint().apply {
            this.color = Color.WHITE
            this.textSize = fontSize
            this.style = Paint.Style.FILL
            this.isAntiAlias = true
        }

        val textBounds = android.graphics.Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)
        val textW = textBounds.width().toFloat()
        val textH = textBounds.height().toFloat()

        val padX = maxOf(6f, fontSize * 0.4f)
        val padY = maxOf(4f, fontSize * 0.25f)

        val pillW = textW + 2 * padX
        val pillH = textH + 2 * padY

        // Dynamic label placement (above the box if there's room, otherwise draw inside the box)
        var ty1 = y1 - pillH - 2f
        var ty2 = y1 - 2f
        if (y1 - pillH - 2f < 0) {
            ty1 = y1 + 2f
            ty2 = y1 + pillH + 2f
        }

        var tx1 = x1
        var tx2 = x1 + pillW
        if (tx2 > originalBitmap.width) {
            tx1 = maxOf(0f, originalBitmap.width - pillW)
            tx2 = originalBitmap.width.toFloat()
        }

        val rx = maxOf(3f, pillH / 4f)
        val pillRect = RectF(tx1, ty1, tx2, ty2)

        // Draw pill background
        val pillPaint = Paint().apply {
            this.color = color
            this.alpha = 230
            this.style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pillRect, rx, rx, pillPaint)

        // Draw pill white border highlight
        val pillBorderPaint = Paint().apply {
            this.color = Color.WHITE
            this.alpha = 100
            this.strokeWidth = 1f
            this.style = Paint.Style.STROKE
        }
        canvas.drawRoundRect(pillRect, rx, rx, pillBorderPaint)

        // Render centered text inside the pill
        val textY = ty1 + padY + textH - textBounds.bottom
        canvas.drawText(label, tx1 + padX - textBounds.left, textY, textPaint)
    }

    return mutableBitmap
}