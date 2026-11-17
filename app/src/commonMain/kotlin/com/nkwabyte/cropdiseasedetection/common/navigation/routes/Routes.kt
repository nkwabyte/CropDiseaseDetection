package com.nkwabyte.cropdiseasedetection.common.navigation.routes

import kotlinx.serialization.Serializable

// ── Tab graphs ──────────────────────────────────────────────────────────────
// One nested graph per bottom-bar tab, mirroring the iOS TabView where every tab
// owns its own NavigationStack: switching tabs keeps each tab's back stack.

@Serializable
data object DiagnosticsTabRoute

@Serializable
data object EncyclopediaTabRoute

@Serializable
data object HistoryTabRoute

@Serializable
data object SettingsTabRoute

@Serializable
data object AccountTabRoute

// ── Tab roots ───────────────────────────────────────────────────────────────

@Serializable
data object HomeScreenRoute

@Serializable
data object EncyclopediaScreenRoute

@Serializable
data object HistoryScreenRoute

@Serializable
data object SettingsScreenRoute

@Serializable
data object ProfileScreenRoute

// ── Pushed screens ──────────────────────────────────────────────────────────

@Serializable
data object DetectionResultScreenRoute

@Serializable
data object RecommendationScreenRoute

/** Treatment guidelines for a saved scan, identified by [HistoryDetailScreenRoute.recordKey]. */
@Serializable
data class HistoryRecommendationScreenRoute(val recordKey: String)

/** A disease page, identified by the database id of the [DiseaseInfo] entry. */
@Serializable
data class DiseaseDetailScreenRoute(val diseaseId: Int)

/** A saved scan, identified by [com.nkwabyte.cropdiseasedetection.ui.screens.history.historyKey]. */
@Serializable
data class HistoryDetailScreenRoute(val recordKey: String)

@Serializable
data object AboutScreenRoute

@Serializable
data object HelpScreenRoute

@Serializable
data object PrivacyScreenRoute

@Serializable
data object ForgotPasswordScreenRoute
