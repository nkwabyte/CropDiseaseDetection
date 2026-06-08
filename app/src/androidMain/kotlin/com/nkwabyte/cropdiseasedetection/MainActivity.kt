package com.nkwabyte.cropdiseasedetection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.nkwabyte.cropdiseasedetection.common.navigation.NavigationRoot
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CropDiseaseDetectionTheme {
                NavigationRoot(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
