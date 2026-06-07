package com.nkwabyte.cropdiseasedetection.common.helpers

import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult

actual class ObjectDetector actual constructor() {
    private var _isLoaded = false
    actual val isLoaded: Boolean
        get() = _isLoaded

    actual suspend fun loadModel() {
        println("Mocking ExecuTorch model loading for iOS...")
        kotlinx.coroutines.delay(1000)
        _isLoaded = true
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
    }
}
