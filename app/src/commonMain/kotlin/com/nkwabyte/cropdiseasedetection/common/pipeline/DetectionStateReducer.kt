package com.nkwabyte.cropdiseasedetection.common.pipeline

import com.nkwabyte.cropdiseasedetection.common.model.DetectionData

/**
 * Turns a [DetectionPipelineResult] into the [DetectionData] the UI observes.
 *
 * Pure and separate from the ViewModel so the state contract can be tested
 * without Firebase, Cloudinary or a ViewModel scope: every branch below is a
 * claim about what the user is told, and each one is asserted in
 * `DetectionStateReducerTest`.
 */
object DetectionStateReducer {

    /**
     * The state a new request starts in.
     *
     * Every result-bearing field is cleared here, so a slow request can never
     * show the previous image's boxes, label or confidence while it runs — the
     * stale-result bug that the old `detect()` had, where only `isDetecting` and
     * `isClassifierRejected` were reset.
     */
    fun starting(previous: DetectionData): DetectionData = previous.copy(
        isDetecting = true,
        isDetected = false,
        isDetectionSuccessful = false,
        isCropMissMatch = false,
        isClassifierRejected = false,
        isInferenceError = false,
        results = emptyList(),
        classificationLabel = null,
        classifierConfidence = 0f,
        predictedCropId = null,
        selectedCropId = null,
        detectorExecuted = false,
        orientedImageWidth = null,
        orientedImageHeight = null,
        outcome = null,
        errorMessage = null,
    )

    /** The terminal state for a completed request. Always leaves
     *  `isDetecting = false`, on every path including the error ones. */
    fun from(result: DetectionPipelineResult, selectedCropId: String?): DetectionData {
        val base = DetectionData(
            isModelLoading = false,
            isDetecting = false,
            isDetected = true,
            results = result.routedResults,
            classificationLabel = result.classification?.label,
            classifierConfidence = result.classification?.confidence ?: 0f,
            predictedCropId = result.routedCrop?.canonicalLabel
                ?: (result.decision as? RoutingDecision.SelectionMismatch)?.predicted?.canonicalLabel,
            selectedCropId = selectedCropId?.takeIf { it.isNotBlank() },
            detectorExecuted = result.detectorExecuted,
            imageWidth = result.boxCoordinateSpace,
            imageHeight = result.boxCoordinateSpace,
            orientedImageWidth = result.orientedImage?.widthPx,
            orientedImageHeight = result.orientedImage?.heightPx,
            outcome = result.outcome.name,
            errorMessage = result.errorMessage,
        )

        return when (result.outcome) {
            PipelineOutcome.DETECTED -> base.copy(isDetectionSuccessful = true)

            // A supported crop that simply has no detectable disease. Not a
            // rejection and not a mismatch — the scan worked.
            PipelineOutcome.NO_DETECTIONS -> base

            // Not one of the three crops: flagged as both rejected and mismatched
            // so existing screens that key off isCropMissMatch keep showing the
            // "unsupported image" result rather than an empty success.
            PipelineOutcome.REJECTED_OUT_OF_DISTRIBUTION -> base.copy(
                isClassifierRejected = true,
                isCropMissMatch = true,
            )

            PipelineOutcome.CROP_MISMATCH -> base.copy(isCropMissMatch = true)

            PipelineOutcome.CLASSIFIER_ERROR,
            PipelineOutcome.DETECTOR_ERROR -> base.copy(isInferenceError = true)
        }
    }
}
