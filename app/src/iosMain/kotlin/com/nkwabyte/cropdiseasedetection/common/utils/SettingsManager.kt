package com.nkwabyte.cropdiseasedetection.common.utils

import platform.Foundation.NSUserDefaults

actual class SettingsManager actual constructor() {
    actual fun getTheme(): String {
        return NSUserDefaults.standardUserDefaults.stringForKey("theme_key") ?: "System Default"
    }

    actual fun setTheme(theme: String) {
        NSUserDefaults.standardUserDefaults.setObject(theme, "theme_key")
    }

    actual fun getClassifierThreshold(): Float {
        val obj = NSUserDefaults.standardUserDefaults.objectForKey("classifier_threshold")
        return if (obj != null) NSUserDefaults.standardUserDefaults.floatForKey("classifier_threshold") else 0.55f
    }

    actual fun setClassifierThreshold(value: Float) {
        NSUserDefaults.standardUserDefaults.setFloat(value, "classifier_threshold")
    }

    actual fun getIouThreshold(): Float {
        val obj = NSUserDefaults.standardUserDefaults.objectForKey("iou_threshold")
        return if (obj != null) NSUserDefaults.standardUserDefaults.floatForKey("iou_threshold") else 0.10f
    }

    actual fun setIouThreshold(value: Float) {
        NSUserDefaults.standardUserDefaults.setFloat(value, "iou_threshold")
    }

    actual fun getDetectionThreshold(): Float {
        val obj = NSUserDefaults.standardUserDefaults.objectForKey("detection_threshold")
        return if (obj != null) NSUserDefaults.standardUserDefaults.floatForKey("detection_threshold") else 0.10f
    }

    actual fun setDetectionThreshold(value: Float) {
        NSUserDefaults.standardUserDefaults.setFloat(value, "detection_threshold")
    }

    actual fun getRecommendationLanguage(): String {
        return NSUserDefaults.standardUserDefaults.stringForKey("recommendation_language") ?: "ENGLISH"
    }

    actual fun setRecommendationLanguage(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, "recommendation_language")
    }
}
