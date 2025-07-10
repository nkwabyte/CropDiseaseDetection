package com.nkwabyte.cropdiseasedetection.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.R
import com.nkwabyte.cropdiseasedetection.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.components.CropTileButton
import com.nkwabyte.cropdiseasedetection.model.CropOption
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme



/**
 * A composable function that displays the Select Crop screen.
 * Users can choose a crop for disease analysis and prediction.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param onCropSelected Lambda function to be invoked when a crop button is clicked.
 * Provides the selected crop name (String resource ID) as an argument.
 * @param onContinueClick Lambda function to be invoked when the Continue button is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCropScreen(
    modifier: Modifier = Modifier,
    onCropSelected: (Int) -> Unit = {},
    onContinueClick: () -> Unit = {},
    onDrawerButtonClick: () -> Unit = { }
) {
    var selectedCropResId by remember { mutableStateOf<Int?>(null) }

    // List of available crop options
    val cropOptions = remember {
        listOf(
            CropOption(R.string.crop_corn, R.drawable.corn_image, R.string.crop_corn_image_desc),
            CropOption(R.string.crop_tomato, R.drawable.tomato_image, R.string.crop_tomato_image_desc),
            CropOption(R.string.crop_pepper, R.drawable.pepper_image, R.string.crop_pepper_image_desc),
            // Add more crops here if needed
        )
    }

    Scaffold(
        topBar = {
            AppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                },
                onDrawerButtonClick = onDrawerButtonClick,
                isHomeScreen = false,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                menuIconColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top green section for title and description
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
                    .background(MaterialTheme.colorScheme.primary),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.select_crop_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.select_crop_description),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Middle section for crop selection buttons using LazyVerticalGrid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                items(cropOptions) { crop ->
                    val isSelected = crop.nameResId == selectedCropResId
                    CropTileButton(
                        cropNameResId = crop.nameResId,
                        cropImageResId = crop.imageResId,
                        cropImageContentDescResId = crop.imageContentDescriptionResId,
                        isSelected = isSelected,
                        onClick = {
                            selectedCropResId = crop.nameResId
                            onCropSelected(crop.nameResId)
                        }
                    )
                }
            }

            // Bottom section for Continue button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onContinueClick,
                    enabled = selectedCropResId != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.continue_button_text),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSelectCropScreen() {
    CropDiseaseDetectionTheme {
        SelectCropScreen()
    }
}
