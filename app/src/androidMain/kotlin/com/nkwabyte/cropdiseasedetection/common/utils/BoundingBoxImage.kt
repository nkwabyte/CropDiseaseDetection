package com.nkwabyte.cropdiseasedetection.common.utils

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult

@Composable
actual fun BoundingBoxImage(
    imageBytes: ByteArray,
    results: List<DetectionResult>,
    modelWidth: Int,
    modelHeight: Int,
    contentDescription: String,
    modifier: Modifier
) {
    val bitmap = remember(imageBytes, results) {
        val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        if (originalBitmap != null) {
            if (results.isNotEmpty()) {
                drawBoundingBoxesOnBitmap(
                    originalBitmap = originalBitmap,
                    results = results,
                    modelWidth = modelWidth,
                    modelHeight = modelHeight
                )
            } else {
                originalBitmap
            }
        } else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}
