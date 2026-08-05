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

@Serializable
data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val isAccepted: Boolean
)

@Serializable
data class DetectionData(
    val results: List<DetectionResult> = emptyList(),
    val isModelLoading: Boolean = false,
    val isDetecting: Boolean = false,
    val isDetected: Boolean = false,
    val isCropMissMatch: Boolean = false,
    val isDetectionSuccessful: Boolean = false,
    val isClassifierRejected: Boolean = false,
    val classifierConfidence: Float = 0f,
    val classificationLabel: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val modelName: String? = null,
    val modelVersion: String? = null,
)