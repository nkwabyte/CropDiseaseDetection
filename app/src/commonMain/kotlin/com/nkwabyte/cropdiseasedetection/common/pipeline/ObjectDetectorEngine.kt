package com.nkwabyte.cropdiseasedetection.common.pipeline

import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.OrientedImageSize
import com.nkwabyte.cropdiseasedetection.common.model.StageLatencyMs

/**
 * Production [InferenceEngine], backed by the platform [ObjectDetector].
 *
 * It is a pass-through by design: every method forwards to the stage-timed
 * platform helper that `classify()`/`detect()` themselves call, so the app and
 * the extended benchmark cannot diverge in what they execute. The only thing
 * this class adds is the interface boundary that lets `DetectionPipeline`'s
 * routing be tested without ExecuTorch.
 */
class ObjectDetectorEngine(private val detector: ObjectDetector) : InferenceEngine {

    /** Idempotent: the platform implementations return immediately when the
     *  requested model is already resident. */
    override suspend fun ensureClassifierLoaded() = detector.loadClassifierModel()

    /** Also picks up a detector swapped in Settings since the last request. */
    override suspend fun ensureDetectorLoaded() = detector.loadModel()

    override fun classify(imageBytes: ByteArray): Pair<ClassificationResult?, StageLatencyMs> =
        detector.classifyStageTimed(imageBytes)

    override fun detect(imageBytes: ByteArray): Pair<List<DetectionResult>, StageLatencyMs> =
        detector.detectStageTimed(imageBytes)

    override fun orientedImageSize(imageBytes: ByteArray): OrientedImageSize? =
        detector.orientedImageSize(imageBytes)
}
