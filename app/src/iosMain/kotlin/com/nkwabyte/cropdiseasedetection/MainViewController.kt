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
import com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager

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

    fun getAppViewModel(): AppViewModel = appViewModel
    fun getAuthViewModel(): AuthViewModel = authViewModel
    fun getDetectionViewModel(): DetectionViewModel = detectionViewModel
    fun getProfileViewModel(): ProfileViewModel = profileViewModel
    fun getSettingsManager(): SettingsManager = settingsManager
}
