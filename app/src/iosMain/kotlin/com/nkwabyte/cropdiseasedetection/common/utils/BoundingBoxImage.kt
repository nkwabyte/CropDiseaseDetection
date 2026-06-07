package com.nkwabyte.cropdiseasedetection.common.utils

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.compose.rememberAsyncImagePainter
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import androidx.compose.ui.layout.ContentScale

@Composable
actual fun BoundingBoxImage(
    imageBytes: ByteArray,
    results: List<DetectionResult>,
    modelWidth: Int,
    modelHeight: Int,
    contentDescription: String,
    modifier: Modifier
) {
    // Basic image loading without boxes for iOS stub
    Image(
        painter = rememberAsyncImagePainter(imageBytes),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
