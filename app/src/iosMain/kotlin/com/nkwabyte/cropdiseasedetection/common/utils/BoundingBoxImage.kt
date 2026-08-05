package com.nkwabyte.cropdiseasedetection.common.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import org.jetbrains.skia.Image as SkiaImage
import kotlin.math.max

private val PALETTE = arrayOf(
    Color(0xFFE74C3C), Color(0xFFC0392B), Color(0xFF2ECC71), Color(0xFF27AE60),
    Color(0xFF3498DB), Color(0xFF2980B9), Color(0xFF9B59B6), Color(0xFF8E44AD),
    Color(0xFFF39C12), Color(0xFFE67E22), Color(0xFF1ABC9C), Color(0xFF16A085),
    Color(0xFF34495E), Color(0xFF2C3E50), Color(0xFF7F8C8D), Color(0xFF95A5A6),
    Color(0xFFD35400), Color(0xFFC0392B), Color(0xFF27AE60), Color(0xFF2980B9),
    Color(0xFF8E44AD), Color(0xFFF39C12), Color(0xFF16A085)
)

private val CROP_ICONS = mapOf(
    "Corn" to "🌽",
    "Pepper" to "🫑",
    "Tomato" to "🍅"
)

@Composable
actual fun BoundingBoxImage(
    imageBytes: ByteArray,
    results: List<DetectionResult>,
    modelWidth: Int,
    modelHeight: Int,
    contentDescription: String,
    modifier: Modifier
) {
    val imageBitmap: ImageBitmap? = remember(imageBytes) {
        runCatching { SkiaImage.makeFromEncoded(imageBytes).toComposeImageBitmap() }.getOrNull()
    }

    val textMeasurer = rememberTextMeasurer()

    if (imageBitmap != null) {
        Box(modifier = modifier) {
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (results.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawBoundingBoxes(
                        results = results,
                        imgW = imageBitmap.width.toFloat(),
                        imgH = imageBitmap.height.toFloat(),
                        modelWidth = modelWidth,
                        modelHeight = modelHeight,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBoundingBoxes(
    results: List<DetectionResult>,
    imgW: Float,
    imgH: Float,
    modelWidth: Int,
    modelHeight: Int,
    textMeasurer: TextMeasurer
) {
    val canvasW = size.width
    val canvasH = size.height

    // ContentScale.Crop: scale uniformly to fill both dimensions, center the result
    val scale = max(canvasW / imgW, canvasH / imgH)
    val displayedW = imgW * scale
    val displayedH = imgH * scale
    val ox = (canvasW - displayedW) / 2f   // ≤ 0 when image is wider than canvas
    val oy = (canvasH - displayedH) / 2f

    fun mx(v: Float) = ox + (v / modelWidth) * displayedW
    fun my(v: Float) = oy + (v / modelHeight) * displayedH

    val baseDim = minOf(canvasW, canvasH)
    val strokeW = max(2f, baseDim / 250f)
    val cornerLen = max(10f, baseDim * 0.04f)
    val cornerW = strokeW + 2f
    val fontSize = max(11f, baseDim / 45f)

    for (result in results) {
        val color = PALETTE.getOrElse(result.classIndex) { Color.Red }

        val x1 = mx(result.box[0])
        val y1 = my(result.box[1])
        val x2 = mx(result.box[2])
        val y2 = my(result.box[3])
        val bw = x2 - x1
        val bh = y2 - y1

        // Semi-transparent fill
        drawRect(color = color.copy(alpha = 0.06f), topLeft = Offset(x1, y1), size = Size(bw, bh))

        // Box border
        drawRect(
            color = color.copy(alpha = 0.7f),
            topLeft = Offset(x1, y1),
            size = Size(bw, bh),
            style = Stroke(width = strokeW)
        )

        // Corner L-brackets
        val lines = listOf(
            Offset(x1, y1) to Offset(x1 + cornerLen, y1),
            Offset(x1, y1) to Offset(x1, y1 + cornerLen),
            Offset(x2, y1) to Offset(x2 - cornerLen, y1),
            Offset(x2, y1) to Offset(x2, y1 + cornerLen),
            Offset(x1, y2) to Offset(x1 + cornerLen, y2),
            Offset(x1, y2) to Offset(x1, y2 - cornerLen),
            Offset(x2, y2) to Offset(x2 - cornerLen, y2),
            Offset(x2, y2) to Offset(x2, y2 - cornerLen)
        )
        lines.forEach { (s, e) ->
            drawLine(color = color, start = s, end = e, strokeWidth = cornerW, cap = StrokeCap.Round)
        }

        // Label pill
        val className = result.displayName.ifEmpty { "Unknown" }
        val crop = className.split(" ").firstOrNull().orEmpty()
        val icon = CROP_ICONS[crop] ?: "🌿"
        val pct = (result.score * 100).toInt()
        val label = " $icon $className  $pct% "

        val labelStyle = TextStyle(
            color = Color.White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold
        )
        val measured = textMeasurer.measure(label, labelStyle)
        val padX = max(6f, fontSize * 0.4f)
        val padY = max(4f, fontSize * 0.25f)
        val pillW = measured.size.width.toFloat() + 2 * padX
        val pillH = measured.size.height.toFloat() + 2 * padY
        val rx = max(3f, pillH / 4f)

        // Place pill above the box; fall back to inside the box if no room above
        var ty1 = y1 - pillH - 2f
        if (ty1 < 0f) ty1 = y1 + 2f
        var tx1 = x1
        if (tx1 + pillW > canvasW) tx1 = max(0f, canvasW - pillW)

        drawRoundRect(
            color = color.copy(alpha = 0.9f),
            topLeft = Offset(tx1, ty1),
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(rx, rx)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.4f),
            topLeft = Offset(tx1, ty1),
            size = Size(pillW, pillH),
            cornerRadius = CornerRadius(rx, rx),
            style = Stroke(width = 1f)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(tx1 + padX, ty1 + padY),
            style = labelStyle
        )
    }
}
