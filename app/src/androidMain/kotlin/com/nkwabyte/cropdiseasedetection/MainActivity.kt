package com.nkwabyte.cropdiseasedetection

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.NavigationRoot
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRunOutputAgreement()
        setContent {
            val appViewModel: AppViewModel = koinInject()
            val appState by appViewModel.appState.collectAsState()
            val isDarkTheme = when (appState.selectedTheme) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }
            CropDiseaseDetectionTheme(darkTheme = isDarkTheme) {
                NavigationRoot(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    /**
     * Cross-platform output-agreement run, debuggable builds only:
     *   adb shell am start -n com.nkwabyte.cropdiseasedetection/.MainActivity --ez run_output_agreement true
     * Reads externalFiles/agreement_in, writes externalFiles/output_agreement_<ms>.json
     * and logs the path under the "Agreement" tag.
     */
    private fun maybeRunOutputAgreement() {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable || !intent.getBooleanExtra("run_output_agreement", false)) return
        val detector: ObjectDetector = KoinJavaComponent.get(ObjectDetector::class.java)
        lifecycleScope.launch {
            Log.i("Agreement", "START")
            val path = runCatching { detector.runOutputAgreement() }
                .onFailure { Log.e("Agreement", "FAILED", it) }
                .getOrNull()
            Log.i("Agreement", "DONE export=$path")
        }
    }
}
