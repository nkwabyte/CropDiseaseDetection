package com.nkwabyte.cropdiseasedetection.common.data

import com.nkwabyte.cropdiseasedetection.common.model.RecommendationLanguage

/**
 * Body text for one disease in one language.
 *
 * `name`, `crop`, `id`, `isHealthy` and `imageUrl` are deliberately absent —
 * those stay canonical across languages so lookups, filtering and the
 * encyclopedia keep working regardless of the selected language.
 */
data class DiseaseTranslation(
    val localName: String,
    val description: String,
    val symptoms: String,
    val causes: String,
    val effects: String,
    val prevention: String,
    val organicMitigation: String,
    val chemicalMitigation: String
)

/**
 * Per-language overlays on top of the canonical English [DiseaseDatabase].
 *
 * Overlaying rather than widening [DiseaseInfo] keeps every existing call site
 * — the encyclopedia, history, detection results, both platforms — working
 * against plain `String` fields.
 *
 * Coverage is allowed to be partial. Any disease with no entry for the selected
 * language falls back to English, and [hasTranslation] lets the screen say so
 * honestly instead of claiming a translation that isn't there.
 *
 * Chemical product names, active ingredients, dosages, concentrations and
 * growth stages (Azoxystrobin, Ridomil Gold, 2 g/L, VT, R1 …) are kept verbatim
 * in every language. Mistranslating a dose is a safety problem, and those terms
 * appear on the product label in Latin script anyway.
 *
 * Non-English text is pending native-speaker review.
 */
object DiseaseTranslations {

    private val byLanguage: Map<RecommendationLanguage, Map<Int, DiseaseTranslation>> = mapOf(
        RecommendationLanguage.HAUSA to HausaDiseaseText.entries,
        RecommendationLanguage.EWE to EweDiseaseText.entries,
        RecommendationLanguage.ASANTE_TWI to TwiDiseaseText.entries,
        RecommendationLanguage.GA to GaDiseaseText.entries,
        RecommendationLanguage.FRENCH to FrenchDiseaseText.entries
    )

    /** True when [id]'s body text is available in [lang]. English is always true. */
    fun hasTranslation(id: Int, lang: RecommendationLanguage): Boolean =
        lang == RecommendationLanguage.ENGLISH || byLanguage[lang]?.containsKey(id) == true

    /** [disease] with its body text swapped for [lang], or unchanged if untranslated. */
    fun localized(disease: DiseaseInfo, lang: RecommendationLanguage): DiseaseInfo {
        val t = byLanguage[lang]?.get(disease.id) ?: return disease
        return disease.copy(
            localName = t.localName,
            description = t.description,
            symptoms = t.symptoms,
            causes = t.causes,
            effects = t.effects,
            prevention = t.prevention,
            organicMitigation = t.organicMitigation,
            chemicalMitigation = t.chemicalMitigation
        )
    }
}
