package com.nkwabyte.cropdiseasedetection.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Default Material 3 typography values
val baseline = Typography()

val AppTypography = Typography(
    displayLarge = baseline.displayLarge,
    displayMedium = baseline.displayMedium,
    displaySmall = baseline.displaySmall,
    headlineLarge = baseline.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = baseline.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = baseline.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseline.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = baseline.titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = baseline.titleSmall.copy(fontWeight = FontWeight.Medium),
    bodyLarge = baseline.bodyLarge.copy(lineHeight = 24.sp),
    bodyMedium = baseline.bodyMedium.copy(lineHeight = 20.sp),
    bodySmall = baseline.bodySmall.copy(lineHeight = 16.sp),
    labelLarge = baseline.labelLarge,
    labelMedium = baseline.labelMedium,
    labelSmall = baseline.labelSmall,
)
