package com.nkwabyte.cropdiseasedetection.model

import kotlinx.serialization.Serializable

/**
 * Data class to represent a crop option.
 * @param nameResId The string resource ID for the crop's name.
 * @param imageResId The drawable resource ID for the crop's image.
 * @param imageContentDescriptionResId The string resource ID for the image's content description.
 */

@Serializable
data class CropOption(
    val nameResId: Int,
    val imageResId: Int,
    val imageContentDescriptionResId: Int
)