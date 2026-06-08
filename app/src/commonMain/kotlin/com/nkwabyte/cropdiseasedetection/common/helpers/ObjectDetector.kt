package com.nkwabyte.cropdiseasedetection.common.helpers

import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult

expect class ObjectDetector() {
    val isLoaded: Boolean
    val isClassifierLoaded: Boolean
    suspend fun loadModel()
    suspend fun loadClassifierModel()
    fun classify(imageBytes: ByteArray): ClassificationResult?
    fun detect(imageBytes: ByteArray): List<DetectionResult>
    fun release()
}
