package com.nkwabyte.cropdiseasedetection.common.data

import com.nkwabyte.cropdiseasedetection.common.model.RecommendationLanguage

/**
 * Fixed copy for the Treatment Guidelines screen — section headings, the
 * translation banner, the audio notice and the advisory disclaimer.
 *
 * This lives here rather than in the `composeResources` locale folders
 * because [RecommendationLanguage] is deliberately decoupled from the app
 * locale: it carries six languages while `AppLanguage` carries three, and
 * changing it does not switch the Compose locale. `stringResource()` resolves
 * against the app locale, so it cannot serve this screen. Keying off the enum
 * directly is the only lookup that works on both platforms.
 *
 * iOS reads the same table through the shared framework
 * (`RecommendationStrings.shared.symptomsTitle(lang:)`) — do not re-declare
 * these in a Swift extension. Swift cannot add a member the imported type
 * already has, and the duplicate immediately drifts from this one.
 *
 * Where a heading already existed in `iosApp/Localizations.json`, that wording
 * is reused verbatim so the two catalogues agree.
 *
 * Translations here are pending native-speaker review for Hausa, Ewe,
 * Asante Twi and Ga.
 */
object RecommendationStrings {

    // ── Section headings ──────────────────────────────────────────────────────

    /** Reuses the "Symptoms" entry from Localizations.json. */
    fun symptomsTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Symptoms"
        RecommendationLanguage.HAUSA -> "Alamomin Cuta"
        RecommendationLanguage.EWE -> "Dɔléle Dzesiwo"
        RecommendationLanguage.ASANTE_TWI -> "Yare Agyiraehyɛde"
        RecommendationLanguage.GA -> "Hela Kadimɔwo"
        RecommendationLanguage.FRENCH -> "Symptômes"
    }

    fun causeTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Cause"
        RecommendationLanguage.HAUSA -> "Sanadi"
        RecommendationLanguage.EWE -> "Nu Si Hea Edzi Vɛ"
        RecommendationLanguage.ASANTE_TWI -> "Deɛ Ɛde Ba"
        RecommendationLanguage.GA -> "Nɔ Ni Kɛbaa"
        RecommendationLanguage.FRENCH -> "Cause"
    }

    fun effectsTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Effects on Crop & Yield"
        RecommendationLanguage.HAUSA -> "Tasiri kan Amfanin Gona"
        RecommendationLanguage.EWE -> "Nu Si Wòwɔna Ɖe Nukuwo Ŋu"
        RecommendationLanguage.ASANTE_TWI -> "Nkɛntɛnsoɔ a Ɛba Nnɔbae So"
        RecommendationLanguage.GA -> "Nɔ Ni Efeɔ Yɛ Ŋmɔ Lɛ He"
        RecommendationLanguage.FRENCH -> "Effets sur les cultures et le rendement"
    }

    /** Reuses the "Organic Management" entry from Localizations.json. */
    fun organicControlTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Organic / Biological Control"
        RecommendationLanguage.HAUSA -> "Hanyoyin Gargajiya"
        RecommendationLanguage.EWE -> "Dzɔdzɔme Atikewo"
        RecommendationLanguage.ASANTE_TWI -> "Abode Mu Nnuru"
        RecommendationLanguage.GA -> "Je Mli Tsofa"
        RecommendationLanguage.FRENCH -> "Gestion biologique"
    }

    /** Reuses the "Chemical Control" entry from Localizations.json. */
    fun chemicalControlTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Chemical Control"
        RecommendationLanguage.HAUSA -> "Magungunan Zamani"
        RecommendationLanguage.EWE -> "Atike Siesie"
        RecommendationLanguage.ASANTE_TWI -> "Kɛmikaal Nnuru"
        RecommendationLanguage.GA -> "Kɛmikaal Tsofa"
        RecommendationLanguage.FRENCH -> "Contrôle chimique"
    }

    /** Reuses the "Prevention Measures" entry from Localizations.json. */
    fun preventionTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Prevention & Cultural Practices"
        RecommendationLanguage.HAUSA -> "Hanyoyin Kiyaye"
        RecommendationLanguage.EWE -> "Dɔléle Xoxe Mɔwo"
        RecommendationLanguage.ASANTE_TWI -> "Nsiyɛ ne Nneɛma a Ɛsɛ sɛ Yɛyɛ"
        RecommendationLanguage.GA -> "Tsibaa Sane"
        RecommendationLanguage.FRENCH -> "Mesures de prévention"
    }

    // ── Healthy-plant headings ────────────────────────────────────────────────

    fun healthyTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Your plant looks healthy!"
        RecommendationLanguage.HAUSA -> "Tsironka yana da lafiya!"
        RecommendationLanguage.EWE -> "Wò ati la le lãmesẽ me!"
        RecommendationLanguage.ASANTE_TWI -> "Wo afifideɛ no ho yɛ den!"
        RecommendationLanguage.GA -> "O tso lɛ he wa!"
        RecommendationLanguage.FRENCH -> "Votre plante est en bonne santé !"
    }

    fun healthyOrganicTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Best Organic Practices to Maintain Health"
        RecommendationLanguage.HAUSA -> "Hanyoyin Gargajiya na Kiyaye Lafiya"
        RecommendationLanguage.EWE -> "Dzɔdzɔme Mɔnu Nyuitɔwo Na Lãmesẽ"
        RecommendationLanguage.ASANTE_TWI -> "Abode Mu Akwan Pa a Ɛkora Ahoɔden So"
        RecommendationLanguage.GA -> "Je Mli Gbɛi Kpakpa Ni Buɔ Hewalɛ"
        RecommendationLanguage.FRENCH -> "Meilleures pratiques biologiques d'entretien"
    }

    fun healthyChemicalTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Agrochemical Maintenance Tips"
        RecommendationLanguage.HAUSA -> "Shawarwarin Magungunan Gona na Kiyaye Lafiya"
        RecommendationLanguage.EWE -> "Agble Atike Ŋuti Aɖaŋuɖoɖowo"
        RecommendationLanguage.ASANTE_TWI -> "Afuom Nnuru Ho Afotuo"
        RecommendationLanguage.GA -> "Ŋmɔ Mli Kɛmikaal He Ŋaawo"
        RecommendationLanguage.FRENCH -> "Conseils d'entretien agrochimique"
    }

    // ── Translation banner ────────────────────────────────────────────────────
    //
    // Two states. The screen picks between them by asking
    // DiseaseTranslations.hasTranslation() — so the banner can never claim more
    // than has actually been translated for the disease on screen.

    /** Shown when the disease body text exists in the selected language. */
    fun bannerLocalizedTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Guidelines localized"
        RecommendationLanguage.HAUSA -> "An fassara jagororin"
        RecommendationLanguage.EWE -> "Wotrɔ mɔfiamewo gɔme"
        RecommendationLanguage.ASANTE_TWI -> "Wɔakyerɛ akwankyerɛ no ase"
        RecommendationLanguage.GA -> "Atsɔɔ ŋaawo lɛ ashishi"
        RecommendationLanguage.FRENCH -> "Recommandations traduites"
    }

    fun bannerLocalizedBody(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Headings and guidance are shown in English."
        RecommendationLanguage.HAUSA -> "Ana nuna kanun labarai da cikakken shawarwari cikin Hausa."
        RecommendationLanguage.EWE -> "Wotrɔ tanyawo kple aɖaŋuɖoɖowo katã gɔme ɖe Eʋegbe me."
        RecommendationLanguage.ASANTE_TWI -> "Wɔakyerɛ tiri nsɛm ne afotuo no nyinaa ase wɔ Twi mu."
        RecommendationLanguage.GA -> "Atsɔɔ yitsei kɛ ŋaawo lɛ fɛɛ ashishi yɛ Ga mli."
        RecommendationLanguage.FRENCH -> "Les titres et les conseils détaillés sont affichés en français."
    }

    /** Shown when only the headings are translated and the body is still English. */
    fun bannerHeadingsOnlyTitle(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Headings only"
        RecommendationLanguage.HAUSA -> "Kanun labarai kaɗai aka fassara"
        RecommendationLanguage.EWE -> "Tanyawo ko wotrɔ gɔme"
        RecommendationLanguage.ASANTE_TWI -> "Tiri nsɛm nkutoo na wɔakyerɛ ase"
        RecommendationLanguage.GA -> "Yitsei pɛ atsɔɔ shishi"
        RecommendationLanguage.FRENCH -> "Titres traduits uniquement"
    }

    fun bannerHeadingsOnlyBody(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Detailed guidance is still shown in English."
        RecommendationLanguage.HAUSA -> "Ana nuna cikakken bayani da Turanci har yanzu."
        RecommendationLanguage.EWE -> "Wogale numeɖeɖe blibo la ɖem fia le Yevugbe me."
        RecommendationLanguage.ASANTE_TWI -> "Wɔda nsɛm no mu nkyerɛkyerɛmu adi wɔ Borɔfo mu."
        RecommendationLanguage.GA -> "Aakɛ saji lɛ fɛɛ tsɔɔ yɛ Blɔfo mli lolo."
        RecommendationLanguage.FRENCH -> "Les conseils détaillés sont encore affichés en anglais."
    }

    // ── Notices ───────────────────────────────────────────────────────────────

    fun audioNotice(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH -> "Audio playback in English is coming soon!"
        RecommendationLanguage.HAUSA -> "Sauraron murya cikin Hausa yana zuwa nan ba da jimawa ba!"
        RecommendationLanguage.EWE -> "Gbeɖiɖi ɖoɖo le Eʋegbe me gbɔna kpuie!"
        RecommendationLanguage.ASANTE_TWI -> "Nne mu tie wɔ Twi mu reba ntɛm!"
        RecommendationLanguage.GA -> "Gbee mli toiboo yɛ Ga mli miiba etsɛŋ!"
        RecommendationLanguage.FRENCH -> "La lecture audio en français arrive bientôt !"
    }

    fun disclaimer(lang: RecommendationLanguage): String = when (lang) {
        RecommendationLanguage.ENGLISH ->
            "These recommendations are for general guidance. Consult a local agronomist or extension officer from Ghana MoFA for location-specific advice and approved chemical products."
        RecommendationLanguage.HAUSA ->
            "Waɗannan shawarwari na gaba ɗaya ne. Tuntuɓi masanin aikin gona na kusa da kai ko jami'in bunƙasa aikin gona na Ghana MoFA don shawarwarin da suka dace da yankinka da magungunan da aka amince da su."
        RecommendationLanguage.EWE ->
            "Aɖaŋuɖoɖo siawo nye mɔfiame bliboe. Bia aɖaŋu tso agbledenunya ŋutinunyala alo Ghana MoFA ƒe agbledeɖe dɔwɔla gbɔ hena aɖaŋuɖoɖo si sɔ na wò nutome kple atike siwo ŋu wode asi."
        RecommendationLanguage.ASANTE_TWI ->
            "Saa afotuo yi yɛ akwankyerɛ a ɛfa ne nyinaa ho. Kɔ afuom ho nimdefoɔ anaa Ghana MoFA kuayɛ panin nkyɛn na wo nsa aka wo mpɔtam mu afotuo ne nnuru a wɔapene so."
        RecommendationLanguage.GA ->
            "Ŋaawo nɛɛ ji mɔfiamɔ ni kɔɔ fɛɛ he. Bi ŋmɔ mli nilelɔ ni bɛŋkɛ bo loo Ghana MoFA ŋmɔ mli nilelɔ koni oná ŋaawo ni sa okpokpaa lɛ kɛ kɛmikaal ni akpɛlɛ nɔ."
        RecommendationLanguage.FRENCH ->
            "Ces recommandations sont données à titre indicatif. Consultez un agronome local ou un agent de vulgarisation du MoFA du Ghana pour des conseils adaptés à votre localité et des produits chimiques homologués."
    }
}
