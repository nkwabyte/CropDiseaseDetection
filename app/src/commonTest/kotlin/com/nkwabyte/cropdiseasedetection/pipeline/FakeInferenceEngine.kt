package com.nkwabyte.cropdiseasedetection.pipeline

import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.OrientedImageSize
import com.nkwabyte.cropdiseasedetection.common.model.StageLatencyMs
import com.nkwabyte.cropdiseasedetection.common.model.emptyStageLatency
import com.nkwabyte.cropdiseasedetection.common.pipeline.InferenceEngine

/**
 * A scriptable stand-in for the platform detector.
 *
 * It counts calls, which is how the tests prove the thing that actually matters
 * about the two-stage fix: that the detector is not merely filtered out of the
 * result on a rejected image, but never invoked at all.
 */
class FakeInferenceEngine(
    private val classification: ClassificationResult?,
    private val detections: List<DetectionResult> = emptyList(),
    private val classifierThrows: Throwable? = null,
    private val detectorThrows: Throwable? = null,
    private val orientedSize: OrientedImageSize? = OrientedImageSize(480, 640, 6, true),
) : InferenceEngine {

    var classifyCalls = 0
        private set
    var detectCalls = 0
        private set
    var classifierLoadCalls = 0
        private set
    var detectorLoadCalls = 0
        private set

    override suspend fun ensureClassifierLoaded() {
        classifierLoadCalls++
    }

    override suspend fun ensureDetectorLoaded() {
        detectorLoadCalls++
    }

    override fun classify(imageBytes: ByteArray): Pair<ClassificationResult?, StageLatencyMs> {
        classifyCalls++
        classifierThrows?.let { throw it }
        return classification to emptyStageLatency(1.0)
    }

    override fun detect(imageBytes: ByteArray): Pair<List<DetectionResult>, StageLatencyMs> {
        detectCalls++
        detectorThrows?.let { throw it }
        return detections to emptyStageLatency(2.0).copy(detectorExecuted = true)
    }

    override fun orientedImageSize(imageBytes: ByteArray): OrientedImageSize? = orientedSize
}

/** A detection with a real class index and the label that index carries. */
fun detectionOf(classIndex: Int, score: Float = 0.9f): DetectionResult = DetectionResult(
    classIndex = classIndex,
    score = score,
    box = floatArrayOf(10f, 10f, 100f, 100f),
    className = com.nkwabyte.cropdiseasedetection.common.AppConstants.CLASS_LABELS
        .getOrElse(classIndex) { "Unknown" },
)

fun accepted(label: String, confidence: Float = 0.91f) =
    ClassificationResult(label = label, confidence = confidence, isAccepted = true)

fun rejected(label: String = "unknown", confidence: Float = 0.31f) =
    ClassificationResult(label = label, confidence = confidence, isAccepted = false)

val IMAGE = ByteArray(16) { it.toByte() }
