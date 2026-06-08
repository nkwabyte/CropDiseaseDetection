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

private var koinInitialized = false

fun MainViewController(): UIViewController = ComposeUIViewController {
    CropDiseaseDetectionTheme {
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
