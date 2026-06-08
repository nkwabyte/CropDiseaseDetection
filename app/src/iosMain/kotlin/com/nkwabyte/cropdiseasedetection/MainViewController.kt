package com.nkwabyte.cropdiseasedetection

import androidx.compose.foundation.layout.fillMaxSize
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
