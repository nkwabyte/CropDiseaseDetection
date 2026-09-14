package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable

@Serializable()
data class AppData(
    /** The selected crop as the user sees it — translated, for display only. */
    val selectedCrop: String? = null,
    /** The same selection as a canonical [SupportedCrop] id. This is the value
     *  routing uses; [selectedCrop] must never be used for that. */
    val selectedCropId: String? = null,
    val detectionResult: List<DetectionResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedImageByteArray: ByteArray? = null, // Replaced Uri string with ByteArray for KMP
    val selectedTheme: String = "System Default",
    val classifierThreshold: Float = 0.55f,
    val iouThreshold: Float = 0.10f,
    val detectionThreshold: Float = 0.10f,
    val userRole: UserRole = UserRole.FARMER,
    val recommendationLanguage: RecommendationLanguage = RecommendationLanguage.ENGLISH,
    val selectedDetectionModel: String = "YOLO26"
) {
    fun isNoResult(): Boolean {
        return detectionResult.isEmpty()
    }

    fun isError(): Boolean {
        return errorMessage != null
    }
}
