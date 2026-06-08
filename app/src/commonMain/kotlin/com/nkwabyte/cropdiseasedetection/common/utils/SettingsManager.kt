package com.nkwabyte.cropdiseasedetection.common.utils

expect class SettingsManager() {
    fun getTheme(): String
    fun setTheme(theme: String)
}
