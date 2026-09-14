package com.nkwabyte.cropdiseasedetection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.nkwabyte.cropdiseasedetection.common.navigation.NavigationRoot
import com.nkwabyte.cropdiseasedetection.common.navigation.di.appModule
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AuthViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.ProfileViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.HistoryViewModel
import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository

private var koinInitialized = false

fun MainViewController(): UIViewController = ComposeUIViewController {
    val appViewModel: AppViewModel = koinInject()
    val appState by appViewModel.appState.collectAsState()
    val isDarkTheme = when (appState.selectedTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemInDarkTheme()
    }
    CropDiseaseDetectionTheme(darkTheme = isDarkTheme) {
        NavigationRoot(modifier = Modifier.fillMaxSize())
    }
}

fun initKoin() {
    if (!koinInitialized) {
        startKoin {
            modules(appModule)
        }
        koinInitialized = true
    }
}

object KoinDependencies : KoinComponent {
    val appViewModel: AppViewModel by inject()
    val authViewModel: AuthViewModel by inject()
    val detectionViewModel: DetectionViewModel by inject()
    val profileViewModel: ProfileViewModel by inject()
    val settingsManager: SettingsManager by inject()
    val historyViewModel: HistoryViewModel by inject()

    /**
     * The same singleton [ObjectDetector] the DetectionViewModel drives.
     *
     * Exposed so the native SwiftUI Settings screen can run the benchmarks
     * against the loaded models rather than constructing a second detector —
     * see iosApp/iosApp/Views/Settings/BenchmarkSection.swift. The iOS app
     * renders SwiftUI, not the shared Compose UI, so the Compose Settings
     * screen's benchmark controls are unreachable there; this is what the
     * native equivalent binds to.
     */
    val objectDetector: ObjectDetector by inject()

    fun getAppViewModel(): AppViewModel = appViewModel
    fun getAuthViewModel(): AuthViewModel = authViewModel
    fun getDetectionViewModel(): DetectionViewModel = detectionViewModel
    fun getProfileViewModel(): ProfileViewModel = profileViewModel
    fun getSettingsManager(): SettingsManager = settingsManager
    fun getHistoryViewModel(): HistoryViewModel = historyViewModel
    fun getObjectDetector(): ObjectDetector = objectDetector
}
