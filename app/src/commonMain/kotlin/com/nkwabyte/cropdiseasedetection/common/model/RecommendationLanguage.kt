package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable

@Serializable
enum class RecommendationLanguage(
    val displayName: String,
    val nativeName: String,
    val flag: String
) {
    ENGLISH("English", "English", "🇬🇧"),
    ASANTE_TWI("Asante Twi", "Twi (Asante)", "🇬🇭"),
    AKUAPEM_TWI("Akuapem Twi", "Twi (Akuapem)", "🇬🇭"),
    EWE("Ewe", "Eʋegbe", "🇬🇭"),
    HAUSA("Hausa", "Hausa", "🇬🇭")
}
