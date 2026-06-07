package com.nkwabyte.cropdiseasedetection.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult

@Composable
expect fun BoundingBoxImage(
    imageBytes: ByteArray,
    results: List<DetectionResult>,
    modelWidth: Int,
    modelHeight: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
)
