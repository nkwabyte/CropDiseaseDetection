package com.nkwabyte.cropdiseasedetection.common.model

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.DrawableResource

/**
 * A crop the user can pick on the select-crop screen.
 *
 * [crop] is the typed, locale-independent identity; [nameResId] is only how it
 * is spelled for this user. The two are kept apart deliberately — the crop name
 * is translated ("Corn" is "Maïs" in French, "Aburo" in Twi), so anything that
 * has to survive a language change, routing above all, must travel as [crop].
 *
 * @param crop The crop this tile selects, and the id handed to the pipeline.
 * @param nameResId The string resource ID for the crop's display name.
 * @param imageResId The drawable resource ID for the crop's image.
 * @param imageContentDescriptionResId The string resource ID for the image's content description.
 */
data class CropOption(
    val crop: SupportedCrop,
    val nameResId: StringResource,
    val imageResId: DrawableResource,
    val imageContentDescriptionResId: StringResource
)