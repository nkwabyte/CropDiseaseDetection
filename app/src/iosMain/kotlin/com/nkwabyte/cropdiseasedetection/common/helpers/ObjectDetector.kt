package com.nkwabyte.cropdiseasedetection.common.helpers

import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult

actual class ObjectDetector actual constructor() {
    private var _isLoaded = false
    private var _isClassifierLoaded = false

    actual val isLoaded: Boolean
        get() = _isLoaded

    actual val isClassifierLoaded: Boolean
        get() = _isClassifierLoaded

    actual suspend fun loadModel() {
        println("Mocking ExecuTorch model loading for iOS...")
        kotlinx.coroutines.delay(1000)
        _isLoaded = true
    }

    actual suspend fun loadClassifierModel() {
        println("Mocking ExecuTorch classifier loading for iOS...")
        kotlinx.coroutines.delay(500)
        _isClassifierLoaded = true
    }

    actual fun classify(imageBytes: ByteArray): ClassificationResult? {
        println("Mocking ExecuTorch classifier inference for iOS...")
        if (!_isClassifierLoaded) return null
        return ClassificationResult(
            label = "Corn",
            confidence = 0.92f,
            isAccepted = true
        )
    }

    actual fun detect(imageBytes: ByteArray): List<DetectionResult> {
        println("Mocking ExecuTorch inference for iOS...")
        if (!_isLoaded) return emptyList()
        // Mock a single dummy detection for testing UI flow
        return listOf(
            DetectionResult(
                classIndex = 0,
                score = 0.95f,
                box = floatArrayOf(0.1f, 0.1f, 0.9f, 0.9f),
                className = "Healthy Crop (Mock)"
            )
        )
    }

    actual fun release() {
        println("Mocking iOS model release")
        _isLoaded = false
        _isClassifierLoaded = false
    }
}
