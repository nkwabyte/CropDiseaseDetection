package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class RecommendationLanguage(
    val displayName: String,
    val nativeName: String,
    val flag: String,
    val code: String
) {
    ENGLISH("English", "English", "🇬🇧", "en"),
    HAUSA("Hausa", "Hausa", "🇬🇭", "ha"),
    EWE("Ewe", "Eʋe", "🇬🇭", "ee"),
    ASANTE_TWI("Asante Twi", "Twi (Asante)", "🇬🇭", "tw"),
    GA("Ga", "Ga", "🇬🇭", "ga"),
    FRENCH("French", "Français", "🇫🇷", "fr")
}
