@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.nkwabyte.cropdiseasedetection.common.helpers

import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.bridge.ExecuTorchBridge
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSData
import platform.Foundation.create
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

actual class ObjectDetector actual constructor() : KoinComponent {
    private val settingsManager: SettingsManager by inject()
    private val bridge = ExecuTorchBridge.shared()

    actual val isLoaded: Boolean
        get() = _isLoaded
    private var _isLoaded = false

    actual val isClassifierLoaded: Boolean
        get() = _isClassifierLoaded
    private var _isClassifierLoaded = false

    actual suspend fun loadModel() {
        val modelName = "crop_disease_yolo26"
        val path = NSBundle.mainBundle.pathForResource(modelName, ofType = "pte")
        if (path == null) {
            println("[iOS-ObjectDetector] ERROR: '$modelName.pte' not found in bundle")
            return
        }
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
        val sizeBytes = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
        _isLoaded = bridge.loadDetectionModelAtPath(path)
        if (_isLoaded) {
            println("[iOS-ObjectDetector] Detection model loaded")
            println("[iOS-ObjectDetector]   File         : $modelName.pte")
            println("[iOS-ObjectDetector]   Path         : $path")
            println("[iOS-ObjectDetector]   Size         : ${formatMb(sizeBytes)} MB ($sizeBytes bytes)")
            println("[iOS-ObjectDetector]   Architecture : YOLO26  |  Task: object_detection")
            println("[iOS-ObjectDetector]   Input        : 640×640 RGB  |  Normalize: pixel/255")
            println("[iOS-ObjectDetector]   Classes      : 23  |  conf_threshold=0.50  iou_threshold=0.10")
            println("[iOS-ObjectDetector]   Backend      : XNNPACK (ExecuTorch 0.6.0)")
        } else {
            println("[iOS-ObjectDetector] ERROR: ExecuTorch failed to load '$modelName.pte'")
        }
    }

    actual suspend fun loadClassifierModel() {
        val modelName = "crop_classifier"
        val path = NSBundle.mainBundle.pathForResource(modelName, ofType = "pte")
        if (path == null) {
            println("[iOS-ObjectDetector] ERROR: '$modelName.pte' not found in bundle")
            return
        }
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
        val sizeBytes = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
        _isClassifierLoaded = bridge.loadClassifierModelAtPath(path)
        if (_isClassifierLoaded) {
            println("[iOS-ObjectDetector] Classifier model loaded")
            println("[iOS-ObjectDetector]   File         : $modelName.pte")
            println("[iOS-ObjectDetector]   Path         : $path")
            println("[iOS-ObjectDetector]   Size         : ${formatMb(sizeBytes)} MB ($sizeBytes bytes)")
            println("[iOS-ObjectDetector]   Architecture : EfficientNet-B2  |  Task: image_classification")
            println("[iOS-ObjectDetector]   Input        : 260×260 RGB  |  Normalize: ImageNet")
            println("[iOS-ObjectDetector]   Classes      : 3 (Corn, Pepper, Tomato)  |  conf_threshold=0.55")
            println("[iOS-ObjectDetector]   Backend      : XNNPACK (ExecuTorch 0.6.0)")
        } else {
            println("[iOS-ObjectDetector] ERROR: ExecuTorch failed to load '$modelName.pte'")
        }
    }

    actual fun classify(imageBytes: ByteArray): ClassificationResult? {
        if (!_isClassifierLoaded) {
            println("[iOS-ObjectDetector] Classifier model not loaded")
            return null
        }

        val nsData = imageBytes.toNSData() ?: return null
        val rawOutput = bridge.runClassificationWithImageData(nsData)
        if (rawOutput.size < 3) {
            println("[iOS-ObjectDetector] Classification raw output size invalid: ${rawOutput.size}")
            return null
        }

        val logits = FloatArray(3) { (rawOutput[it] as NSNumber).floatValue }
        val maxLogit = logits.max()
        val exps = FloatArray(3) { kotlin.math.exp((logits[it] - maxLogit).toDouble()).toFloat() }
        val sum = exps.sum()
        val probs = FloatArray(3) { exps[it] / sum }

        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
        val maxProb = probs[maxIdx]
        val classes = arrayOf("Corn", "Pepper", "Tomato")

        val threshold = settingsManager.getClassifierThreshold()
        val label = if (maxProb >= threshold) classes[maxIdx] else "unknown"
        println("[iOS-ObjectDetector] Classifier probs: ${probs.joinToString()} -> Top: ${classes[maxIdx]} (${maxProb}), Threshold: $threshold, Label: $label")
        return ClassificationResult(
            label = label,
            confidence = maxProb,
            isAccepted = label != "unknown"
        )
    }

    actual fun detect(imageBytes: ByteArray): List<DetectionResult> {
        if (!_isLoaded) {
            println("[iOS-ObjectDetector] Detection model not loaded")
            return emptyList()
        }

        val nsData = imageBytes.toNSData() ?: return emptyList()
        val rawOutput = bridge.runDetectionWithImageData(nsData)
        if (rawOutput.isEmpty()) {
            println("[iOS-ObjectDetector] Detection raw output empty")
            return emptyList()
        }

        val numClasses = 23
        val rowStride = numClasses + 4
        val n = rawOutput.size / rowStride
        if (n <= 0) return emptyList()

        val detectionThreshold = settingsManager.getDetectionThreshold()
        val preliminary = mutableListOf<DetectionResult>()

        for (i in 0 until n) {
            var maxScore = 0f
            var classId = -1
            for (j in 0 until numClasses) {
                val score = (rawOutput[(j + 4) * n + i] as NSNumber).floatValue
                if (score > maxScore) { maxScore = score; classId = j }
            }
            if (maxScore > detectionThreshold && classId >= 0) {
                val cx = (rawOutput[0 * n + i] as NSNumber).floatValue
                val cy = (rawOutput[1 * n + i] as NSNumber).floatValue
                val w  = (rawOutput[2 * n + i] as NSNumber).floatValue
                val h  = (rawOutput[3 * n + i] as NSNumber).floatValue
                preliminary.add(DetectionResult(
                    classIndex = classId,
                    score = maxScore,
                    box = floatArrayOf(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f),
                    className = AppConstants.CLASS_LABELS.getOrElse(classId) { "Unknown" }
                ))
            }
        }
        val finalDetections = nonMaxSuppression(preliminary, settingsManager.getIouThreshold())
        println("[iOS-ObjectDetector] Detections count: ${finalDetections.size} (preliminary: ${preliminary.size}, threshold: $detectionThreshold)")
        return finalDetections
    }

    actual fun release() {
        bridge.releaseModels()
        _isLoaded = false
        _isClassifierLoaded = false
    }
}

// ---- helpers ----------------------------------------------------------------

private fun ByteArray.toNSData(): NSData? = this.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
}

private fun nonMaxSuppression(detections: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
    val sorted = detections.sortedByDescending { it.score }
    val result = mutableListOf<DetectionResult>()
    for (det in sorted) {
        val keep = result.none { kept ->
            det.classIndex == kept.classIndex && iou(det.box, kept.box) > iouThreshold
        }
        if (keep) result.add(det)
    }
    return result
}

private fun iou(a: FloatArray, b: FloatArray): Float {
    val x1 = maxOf(a[0], b[0]); val y1 = maxOf(a[1], b[1])
    val x2 = minOf(a[2], b[2]); val y2 = minOf(a[3], b[3])
    val inter = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
    val union = (a[2]-a[0])*(a[3]-a[1]) + (b[2]-b[0])*(b[3]-b[1]) - inter
    return if (union > 0f) inter / union else 0f
}

private fun formatMb(bytes: Long): String {
    val whole = bytes / (1024L * 1024L)
    val frac = (bytes % (1024L * 1024L)) * 100L / (1024L * 1024L)
    return "$whole.${frac.toString().padStart(2, '0')}"
}
