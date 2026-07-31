package com.nkwabyte.cropdiseasedetection.common.utils

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class SettingsManager actual constructor() : KoinComponent {
    private val context: Context by inject()
    private val prefs by lazy {
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    actual fun getTheme(): String {
        return prefs.getString("theme_key", "System Default") ?: "System Default"
    }

    actual fun setTheme(theme: String) {
        prefs.edit().putString("theme_key", theme).apply()
    }

    actual fun getClassifierThreshold(): Float {
        return prefs.getFloat("classifier_threshold", 0.55f)
    }

    actual fun setClassifierThreshold(value: Float) {
        prefs.edit().putFloat("classifier_threshold", value).apply()
    }

    actual fun getIouThreshold(): Float {
        return prefs.getFloat("iou_threshold", 0.10f)
    }

    actual fun setIouThreshold(value: Float) {
        prefs.edit().putFloat("iou_threshold", value).apply()
    }

    actual fun getDetectionThreshold(): Float {
        return prefs.getFloat("detection_threshold", 0.10f)
    }

    actual fun setDetectionThreshold(value: Float) {
        prefs.edit().putFloat("detection_threshold", value).apply()
    }

    actual fun getRecommendationLanguage(): String {
        return prefs.getString("recommendation_language", "ENGLISH") ?: "ENGLISH"
    }

    actual fun setRecommendationLanguage(value: String) {
        prefs.edit().putString("recommendation_language", value).apply()
    }

    actual fun getDetectionModel(): String {
        return prefs.getString("detection_model", "YOLO26") ?: "YOLO26"
    }

    actual fun setDetectionModel(model: String) {
        prefs.edit().putString("detection_model", model).apply()
    }

    actual fun getAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = pInfo.versionName ?: "1.0.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            "v$versionName ($versionCode)"
        } catch (e: Exception) {
            "v1.0.0 (1)"
        }
    }
}
