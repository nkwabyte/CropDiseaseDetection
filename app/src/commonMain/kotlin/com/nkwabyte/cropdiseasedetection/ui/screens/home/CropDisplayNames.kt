package com.nkwabyte.cropdiseasedetection.ui.screens.home

import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.generated.resources.Res
import com.nkwabyte.cropdiseasedetection.generated.resources.crop_corn
import com.nkwabyte.cropdiseasedetection.generated.resources.crop_pepper
import com.nkwabyte.cropdiseasedetection.generated.resources.crop_tomato
import com.nkwabyte.cropdiseasedetection.generated.resources.crop_unknown
import org.jetbrains.compose.resources.StringResource

/**
 * The translated name for a crop.
 *
 * This is the only direction the mapping is allowed to go: typed crop ->
 * display text. Going the other way — reading a crop out of a name the user can
 * see — is what broke routing in every non-English locale, so there is
 * deliberately no inverse here.
 */
fun SupportedCrop?.displayNameRes(): StringResource = when (this) {
    SupportedCrop.CORN -> Res.string.crop_corn
    SupportedCrop.PEPPER -> Res.string.crop_pepper
    SupportedCrop.TOMATO -> Res.string.crop_tomato
    null -> Res.string.crop_unknown
}

/** Resolves a canonical crop id (as carried in DetectionData/AppData) to its
 *  translated name, falling back to the "unknown" string. */
fun cropIdDisplayNameRes(canonicalId: String?): StringResource =
    SupportedCrop.fromCanonicalId(canonicalId).displayNameRes()
