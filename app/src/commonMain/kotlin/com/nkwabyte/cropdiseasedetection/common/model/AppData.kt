package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable

@Serializable()
data class AppData(
    val selectedCrop: String? = null,
    val detectionResult: List<DetectionResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedImageByteArray: ByteArray? = null, // Replaced Uri string with ByteArray for KMP
    val selectedTheme: String = "System Default",
    val classifierThreshold: Float = 0.55f,
    val iouThreshold: Float = 0.10f,
    val detectionThreshold: Float = 0.10f,
    val userRole: UserRole = UserRole.FARMER
) {
    fun isNoResult(): Boolean {
        return detectionResult.isEmpty()
    }

    fun isError(): Boolean {
        return errorMessage != null
    }
}
