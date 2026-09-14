package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable

@Serializable
data class DetectionResult(
    val classIndex: Int,
    val score: Float,
    val box: FloatArray,
    val className: String? = null,
) {
    /**
     * The class name as it should be shown to a user, and as it should be matched
     * against [com.nkwabyte.cropdiseasedetection.common.data.DiseaseDatabase].
     *
     * Underscores come from the raw YOLO class ids (`Corn_Common_Rust`) and have leaked
     * into stored history records, so stripping them here rather than only fixing the
     * label list also cleans up scans that were already saved. It matters beyond looks:
     * the disease lookups match on `name.contains(className)`, and "Corn Common_Rust"
     * never matched the database's "Corn Common Rust".
     */
    val displayName: String
        get() = className
            ?.replace('_', ' ')
            ?.split(' ')
            ?.filter { it.isNotBlank() }
            ?.joinToString(" ")
            .orEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DetectionResult

        if (classIndex != other.classIndex) return false
        if (score != other.score) return false
        if (!box.contentEquals(other.box)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = classIndex
        result = 31 * result + score.hashCode()
        result = 31 * result + box.contentHashCode()
        return result
    }
}

/**
 * Authoritative dimensions of an image AFTER EXIF orientation has been applied,
 * i.e. the pixel grid the classifier and detector actually saw.
 *
 * This exists because the encoded width/height of a JPEG are not the dimensions
 * of the upright image: an EXIF orientation of 5-8 transposes them. Reporting
 * the encoded values alongside boxes computed from the upright pixels is how a
 * coordinate mismatch gets hidden, so the pipeline carries the corrected values
 * explicitly rather than letting a caller supply its own.
 *
 * @param exifOrientation   the raw EXIF tag value (1-8), or 1 when absent/unreadable.
 * @param orientationApplied true when a rotation and/or mirror was actually performed.
 */
@Serializable
data class OrientedImageSize(
    val widthPx: Int,
    val heightPx: Int,
    val exifOrientation: Int,
    val orientationApplied: Boolean,
)

@Serializable
data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val isAccepted: Boolean
)

/**
 * The UI-facing state of one detection request.
 *
 * The flags are deliberately not interchangeable, because the situations they
 * describe are not: [isClassifierRejected] means the classifier said this is not
 * a supported crop, [isCropMissMatch] means it is a supported crop other than
 * the selected one, and [isInferenceError] means nothing could be determined at
 * all. Conflating the last one with "no disease found" would report a broken
 * model as a healthy plant.
 *
 * [imageWidth]/[imageHeight] are the box COORDINATE SPACE — the square canvas
 * detections are expressed in (see `BOX_COORDINATE_SPACE`) — and are what the
 * overlay renderer scales out of. The image's own pixel dimensions are
 * [orientedImageWidth]/[orientedImageHeight], and those are post-EXIF-
 * orientation values, not the encoded ones.
 */
@Serializable
data class DetectionData(
    val results: List<DetectionResult> = emptyList(),
    val isModelLoading: Boolean = false,
    val isDetecting: Boolean = false,
    val isDetected: Boolean = false,
    val isCropMissMatch: Boolean = false,
    val isDetectionSuccessful: Boolean = false,
    val isClassifierRejected: Boolean = false,
    /** The classifier or detector could not produce a result. Distinct from a
     *  rejection and from a clean run that simply found nothing. */
    val isInferenceError: Boolean = false,
    val classifierConfidence: Float = 0f,
    val classificationLabel: String? = null,
    /** Canonical id of the crop the classifier predicted, when it accepted one. */
    val predictedCropId: String? = null,
    /** Canonical id of the crop the user had selected for this request. */
    val selectedCropId: String? = null,
    /** True only when stage 2 actually ran — the machine-checkable record that
     *  rejected and mismatched images never reached the detector. */
    val detectorExecuted: Boolean = false,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val orientedImageWidth: Int? = null,
    val orientedImageHeight: Int? = null,
    /** Name of the terminating [com.nkwabyte.cropdiseasedetection.common.pipeline.PipelineOutcome]. */
    val outcome: String? = null,
    val errorMessage: String? = null,
    val modelName: String? = null,
    val modelVersion: String? = null,
)