package com.nkwabyte.cropdiseasedetection.common.utils

import platform.Foundation.NSUserDefaults

actual class SettingsManager actual constructor() {
    actual fun getTheme(): String {
        return NSUserDefaults.standardUserDefaults.stringForKey("theme_key") ?: "System Default"
    }

    // Every setter flushes with synchronize(). NSUserDefaults writes back lazily and on
    // graceful termination; a force-quit shortly after a change can drop it, so the
    // choice is silently lost on next launch. Deprecated as "unnecessary" by Apple, but
    // that guidance assumes the app is allowed to terminate normally.
    actual fun setTheme(theme: String) {
        NSUserDefaults.standardUserDefaults.setObject(theme, "theme_key")
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun getClassifierThreshold(): Float {
        val obj = NSUserDefaults.standardUserDefaults.objectForKey("classifier_threshold")
        return if (obj != null) NSUserDefaults.standardUserDefaults.floatForKey("classifier_threshold") else 0.55f
    }

    actual fun setClassifierThreshold(value: Float) {
        NSUserDefaults.standardUserDefaults.setFloat(value, "classifier_threshold")
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun getIouThreshold(): Float {
        val obj = NSUserDefaults.standardUserDefaults.objectForKey("iou_threshold")
        return if (obj != null) NSUserDefaults.standardUserDefaults.floatForKey("iou_threshold") else 0.10f
    }

    actual fun setIouThreshold(value: Float) {
        NSUserDefaults.standardUserDefaults.setFloat(value, "iou_threshold")
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun getDetectionThreshold(): Float {
        val obj = NSUserDefaults.standardUserDefaults.objectForKey("detection_threshold")
        return if (obj != null) NSUserDefaults.standardUserDefaults.floatForKey("detection_threshold") else 0.10f
    }

    actual fun setDetectionThreshold(value: Float) {
        NSUserDefaults.standardUserDefaults.setFloat(value, "detection_threshold")
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun getRecommendationLanguage(): String {
        return NSUserDefaults.standardUserDefaults.stringForKey("recommendation_language") ?: "ENGLISH"
    }

    actual fun setRecommendationLanguage(value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, "recommendation_language")
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun getDetectionModel(): String {
        return NSUserDefaults.standardUserDefaults.stringForKey("detection_model") ?: "YOLO26"
    }

    actual fun setDetectionModel(model: String) {
        NSUserDefaults.standardUserDefaults.setObject(model, "detection_model")
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun getAppVersion(): String {
        val dict = platform.Foundation.NSBundle.mainBundle.infoDictionary
        val version = dict?.get("CFBundleShortVersionString") as? String ?: "1.0.0"
        val build = dict?.get("CFBundleVersion") as? String ?: "1"
        return "v$version ($build)"
    }
}
