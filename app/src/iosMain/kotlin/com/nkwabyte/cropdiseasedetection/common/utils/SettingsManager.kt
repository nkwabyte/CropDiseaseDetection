package com.nkwabyte.cropdiseasedetection.common.utils

import platform.Foundation.NSUserDefaults

actual class SettingsManager actual constructor() {
    actual fun getTheme(): String {
        return NSUserDefaults.standardUserDefaults.stringForKey("theme_key") ?: "System Default"
    }

    actual fun setTheme(theme: String) {
        NSUserDefaults.standardUserDefaults.setObject(theme, "theme_key")
    }
}
