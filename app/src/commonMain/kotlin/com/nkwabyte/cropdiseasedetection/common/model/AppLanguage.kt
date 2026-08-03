package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val flag: String
) {
    ENGLISH("en", "English", "English", "🇬🇧"),
    FRENCH("fr", "French", "Français", "🇫🇷"),
    TWI("tw", "Twi", "Twi (Akan)", "🇬🇭")
}
