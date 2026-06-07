package com.nkwabyte.cropdiseasedetection.common.model

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.DrawableResource

/**
 * Data class to represent a crop option.
 * @param nameResId The string resource ID for the crop's name.
 * @param imageResId The drawable resource ID for the crop's image.
 * @param imageContentDescriptionResId The string resource ID for the image's content description.
 */

data class CropOption(
    val nameResId: StringResource,
    val imageResId: DrawableResource,
    val imageContentDescriptionResId: StringResource
)