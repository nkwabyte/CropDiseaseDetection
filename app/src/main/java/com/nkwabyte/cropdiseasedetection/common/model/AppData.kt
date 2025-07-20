package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable

@Serializable()
data class AppData(
    val selectedCrop: String? = null,
    val detectionResult: List<DetectionResult> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedImageUri: String? = null // Uri serialized as string
) {
    fun isNoResult(): Boolean {
        return detectionResult.isEmpty()
    }

    fun isError(): Boolean {
        return errorMessage != null
    }
}
