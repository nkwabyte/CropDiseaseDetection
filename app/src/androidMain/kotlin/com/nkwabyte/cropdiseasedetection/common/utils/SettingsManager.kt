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
}
