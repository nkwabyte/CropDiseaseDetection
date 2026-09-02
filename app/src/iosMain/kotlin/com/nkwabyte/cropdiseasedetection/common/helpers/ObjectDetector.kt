@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.nkwabyte.cropdiseasedetection.common.helpers

import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelCatalog
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelSpec
import com.nkwabyte.cropdiseasedetection.common.model.DetectionOutputLayout
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

    /** The spec the currently loaded model was built from — detect() decodes
     *  against this, and a settings change is detected by comparing ids. */
    private var _loadedSpec: DetectionModelSpec? = null

    actual val isClassifierLoaded: Boolean
        get() = _isClassifierLoaded
    private var _isClassifierLoaded = false

    /**
     * Loads the detector the user selected, replacing the loaded one if the
     * selection changed. Safe and cheap to call before every detection: it only
     * reloads when the model actually differs.
     */
    actual suspend fun loadModel() {
        val spec = DetectionModelCatalog.byId(settingsManager.getDetectionModel())
        if (_isLoaded && _loadedSpec?.id == spec.id) return

        val modelName = spec.assetName.removeSuffix(".pte")
        val path = NSBundle.mainBundle.pathForResource(modelName, ofType = "pte")
        if (path == null) {
            println("[iOS-ObjectDetector] ERROR: '$modelName.pte' not found in bundle")
            _isLoaded = false
            _loadedSpec = null
            return
        }
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
        val sizeBytes = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
        _isLoaded = bridge.loadDetectionModelAtPath(path)
        _loadedSpec = if (_isLoaded) spec else null
        if (_isLoaded) {
            println("[iOS-ObjectDetector] Detection model loaded")
            println("[iOS-ObjectDetector]   File         : $modelName.pte  (${spec.displayName})")
            println("[iOS-ObjectDetector]   Path         : $path")
            println("[iOS-ObjectDetector]   Size         : ${formatMb(sizeBytes)} MB ($sizeBytes bytes)")
            println("[iOS-ObjectDetector]   Task         : object_detection  |  Layout: ${spec.layout}")
            println("[iOS-ObjectDetector]   Input        : ${spec.inputSize}×${spec.inputSize} RGB  |  Normalize: pixel/255  |  ${if (spec.letterbox) "letterboxed" else "stretched"}")
            println("[iOS-ObjectDetector]   Classes      : ${spec.numClasses}  |  NMS: ${spec.applyNms}")
            println("[iOS-ObjectDetector]   Backend      : XNNPACK (ExecuTorch 1.3.1)")
        } else {
            println("[iOS-ObjectDetector] ERROR: ExecuTorch failed to load '$modelName.pte'")
        }
    }

    actual suspend fun loadClassifierModel() {
        val modelName = CLASSIFIER_RESOURCE
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
            println("[iOS-ObjectDetector]   Classes      : ${CROP_CLASSES.size} (${CROP_CLASSES.joinToString()})  |  rejection: argmax=='Other' or below the confidence floor")
            println("[iOS-ObjectDetector]   Backend      : XNNPACK (ExecuTorch 1.3.1)")
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
        if (rawOutput.size < CROP_CLASSES.size) {
            println("[iOS-ObjectDetector] Classification raw output size invalid: ${rawOutput.size}")
            return null
        }

        val n = CROP_CLASSES.size
        val logits = FloatArray(n) { (rawOutput[it] as NSNumber).floatValue }
        val maxLogit = logits.max()
        val exps = FloatArray(n) { kotlin.math.exp((logits[it] - maxLogit).toDouble()).toFloat() }
        val sum = exps.sum()
        val probs = FloatArray(n) { exps[it] / sum }

        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
        val maxProb = probs[maxIdx]

        // Two rejection mechanisms that fail differently: the learned "Other" class
        // catches the non-crop species it was trained on, the confidence floor still
        // catches confidently-wrong predictions on species it has never seen.
        val threshold = settingsManager.getClassifierThreshold()
        val label = if (maxIdx == OTHER_INDEX || maxProb < threshold) "unknown" else CROP_CLASSES[maxIdx]
        println("[iOS-ObjectDetector] Classifier probs: ${probs.joinToString()} -> Top: ${CROP_CLASSES[maxIdx]} (${maxProb}), Floor: $threshold, Label: $label")
        return ClassificationResult(
            label = label,
            confidence = maxProb,
            isAccepted = label != "unknown"
        )
    }

    actual fun detect(imageBytes: ByteArray): List<DetectionResult> {
        val spec = _loadedSpec
        if (!_isLoaded || spec == null) {
            println("[iOS-ObjectDetector] Detection model not loaded")
            return emptyList()
        }

        val nsData = imageBytes.toNSData() ?: return emptyList()
        // The letterboxed path lets the bridge un-letterbox the boxes back to the
        // original aspect ratio; the stretched path returns them untouched and
        // normalized, so the two cannot share one call.
        val rawOutput = if (spec.letterbox) {
            bridge.runDetectionWithImageData(nsData)
        } else {
            bridge.runDetectionStretchedWithImageData(nsData, inputSize = spec.inputSize.toLong())
        }
        if (rawOutput.isEmpty()) {
            println("[iOS-ObjectDetector] Detection raw output empty")
            return emptyList()
        }

        val stride = spec.numClasses + 4
        val n = rawOutput.size / stride
        if (n <= 0) return emptyList()

        val detectionThreshold = settingsManager.getDetectionThreshold()
        val boxScale = if (spec.normalizedBoxes) 640f else 1f
        val preliminary = mutableListOf<DetectionResult>()

        for (i in 0 until n) {
            var maxScore = 0f
            var classId = -1
            var cx = 0f; var cy = 0f; var w = 0f; var h = 0f

            when (spec.layout) {
                // [1, 4 + numClasses, n] — a prediction's fields are n apart.
                DetectionOutputLayout.YOLO_ATTRIBUTE_MAJOR -> {
                    for (j in 0 until spec.numClasses) {
                        val score = (rawOutput[(j + 4) * n + i] as NSNumber).floatValue
                        if (score > maxScore) { maxScore = score; classId = j }
                    }
                    cx = (rawOutput[0 * n + i] as NSNumber).floatValue
                    cy = (rawOutput[1 * n + i] as NSNumber).floatValue
                    w  = (rawOutput[2 * n + i] as NSNumber).floatValue
                    h  = (rawOutput[3 * n + i] as NSNumber).floatValue
                }
                // [1, numQueries, 4 + numClasses] — one contiguous row per query.
                DetectionOutputLayout.DETR_QUERY_MAJOR -> {
                    val row = i * stride
                    for (j in 0 until spec.numClasses) {
                        val score = (rawOutput[row + 4 + j] as NSNumber).floatValue
                        if (score > maxScore) { maxScore = score; classId = j }
                    }
                    cx = (rawOutput[row + 0] as NSNumber).floatValue
                    cy = (rawOutput[row + 1] as NSNumber).floatValue
                    w  = (rawOutput[row + 2] as NSNumber).floatValue
                    h  = (rawOutput[row + 3] as NSNumber).floatValue
                }
            }

            if (maxScore > detectionThreshold && classId >= 0) {
                val sx = cx * boxScale; val sy = cy * boxScale
                val sw = w * boxScale;  val sh = h * boxScale
                preliminary.add(DetectionResult(
                    classIndex = classId,
                    score = maxScore,
                    box = floatArrayOf(sx - sw / 2f, sy - sh / 2f, sx + sw / 2f, sy + sh / 2f),
                    className = AppConstants.CLASS_LABELS.getOrElse(classId) { "Unknown" }
                ))
            }
        }

        // RT-DETR's query head already emits one box per object; running NMS over it
        // would merge distinct detections that legitimately overlap.
        val finalDetections = if (spec.applyNms) {
            nonMaxSuppression(preliminary, settingsManager.getIouThreshold())
        } else {
            preliminary
        }
        println("[iOS-ObjectDetector] Detections count: ${finalDetections.size} (preliminary: ${preliminary.size}, threshold: $detectionThreshold, model: ${spec.id})")
        return finalDetections
    }

    actual fun release() {
        bridge.releaseModels()
        _isLoaded = false
        _loadedSpec = null
        _isClassifierLoaded = false
    }
}

// ---- helpers ----------------------------------------------------------------

// The 4-class classifier with a learned "Other" class. It matches the 3-class
// model on crop accuracy (97.54%) while rejecting 98.7% of non-crop images
// against the old 40.8% at a 0.55 threshold — see the project's
// docs/10_classifier_ood_adoption.md. Rejection is by argmax, with the
// confidence floor kept on top of it.
private const val CLASSIFIER_RESOURCE = "crop_classifier_ood"
private val CROP_CLASSES = arrayOf("Corn", "Pepper", "Tomato", "Other")
private const val OTHER_INDEX = 3

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
