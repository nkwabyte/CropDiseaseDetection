package com.nkwabyte.cropdiseasedetection.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nkwabyte.cropdiseasedetection.R
import com.nkwabyte.cropdiseasedetection.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.navigation.viewmodel.DetectionResultViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionResultScreen(
    modifier: Modifier = Modifier,
    onDrawerButtonClick: () -> Unit = { },
    viewModel: DetectionResultViewModel = koinViewModel(),
) {
    Scaffold (
        topBar = {
            AppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onDrawerButtonClick = onDrawerButtonClick,
                isHomeScreen = false,
            )
        },
    ){
        contentPadding ->
        Column (
            modifier = modifier.fillMaxSize().padding(contentPadding)
        ) {

        }
    }
}