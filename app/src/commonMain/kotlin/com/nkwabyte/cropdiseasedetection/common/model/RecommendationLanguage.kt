package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class RecommendationLanguage(
    val displayName: String,
    val nativeName: String,
    val flag: String
) {
    ENGLISH("English", "English", "🇬🇧"),
    HAUSA("Hausa", "Hausa", "🇬🇭"),
    EWE("Ewe", "Eʋegbe", "🇬🇭"),
    ASANTE_TWI("Asante Twi", "Twi (Asante)", "🇬🇭"),
    GA("Ga", "Ga", "🇬🇭")
}
