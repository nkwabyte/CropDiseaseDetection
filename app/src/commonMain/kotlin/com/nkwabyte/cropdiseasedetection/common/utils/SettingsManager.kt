package com.nkwabyte.cropdiseasedetection.common.utils

expect class SettingsManager() {
    fun getTheme(): String
    fun setTheme(theme: String)
    fun getClassifierThreshold(): Float
    fun setClassifierThreshold(value: Float)
    fun getIouThreshold(): Float
    fun setIouThreshold(value: Float)
    fun getDetectionThreshold(): Float
    fun setDetectionThreshold(value: Float)
}
