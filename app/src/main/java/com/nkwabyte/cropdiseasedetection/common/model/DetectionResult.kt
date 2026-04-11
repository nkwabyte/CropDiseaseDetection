package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable

@Serializable
data class DetectionResult(
    val classIndex: Int,
    val score: Float,
    val box: FloatArray,
    val className: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

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
data class DetectionData(
    val results: List<DetectionResult> = emptyList(),
    val isModelLoading: Boolean = false,
    val isDetecting: Boolean = false,
    val isDetected: Boolean = false,
    val isCropMissMatch: Boolean = false,
    val isDetectionSuccessful: Boolean = false,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val modelName: String? = null,
    val modelVersion: String? = null,
)