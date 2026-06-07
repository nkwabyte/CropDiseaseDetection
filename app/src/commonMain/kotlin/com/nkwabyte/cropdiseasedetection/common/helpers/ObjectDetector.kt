package com.nkwabyte.cropdiseasedetection.common.helpers

import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult

expect class ObjectDetector() {
    val isLoaded: Boolean
    suspend fun loadModel()
    fun detect(imageBytes: ByteArray): List<DetectionResult>
    fun release()
}
