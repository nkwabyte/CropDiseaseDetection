package com.nkwabyte.cropdiseasedetection.navigation.routes

import androidx.navigation3.runtime.NavKey
import com.nkwabyte.cropdiseasedetection.model.DetectionResult
import kotlinx.serialization.Serializable

@Serializable
data object SplashScreenRoute: NavKey

@Serializable
data object SelectCropScreenRoute: NavKey

@Serializable
data object HomeScreenRoute: NavKey

@Serializable
data object AboutScreenRoute: NavKey

@Serializable
data object HelpScreenRoute: NavKey

@Serializable
data object PrivacyScreenRoute: NavKey

@Serializable
data object LoginScreenRoute: NavKey

@Serializable
data object RegisterScreenRoute: NavKey

@Serializable
data class ProfileScreenRoute(val id: String): NavKey

@Serializable
data class DetectionResultScreenRoute(val detectionResult: List<DetectionResult>): NavKey