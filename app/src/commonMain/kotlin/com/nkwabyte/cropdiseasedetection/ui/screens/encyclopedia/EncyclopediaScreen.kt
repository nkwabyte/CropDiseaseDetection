package com.nkwabyte.cropdiseasedetection.ui.screens.encyclopedia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncyclopediaScreen(
    onDrawerButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppBar(
                title = { Text("Disease Encyclopedia", color = MaterialTheme.colorScheme.onPrimary) },
                onDrawerButtonClick = onDrawerButtonClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Encyclopedia coming soon!",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
