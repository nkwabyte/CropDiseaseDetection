package com.nkwabyte.cropdiseasedetection.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.ui.components.GradientSnackBar
import com.nkwabyte.cropdiseasedetection.generated.resources.Res
import com.nkwabyte.cropdiseasedetection.generated.resources.app_name
import com.nkwabyte.cropdiseasedetection.generated.resources.detected_image_content_description
import com.nkwabyte.cropdiseasedetection.generated.resources.detection_details_title
import com.nkwabyte.cropdiseasedetection.generated.resources.selected_crop_label
import com.nkwabyte.cropdiseasedetection.generated.resources.no_detection_message
import org.jetbrains.compose.resources.stringResource
import com.nkwabyte.cropdiseasedetection.common.utils.BoundingBoxImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionResultScreen(
    modifier: Modifier = Modifier,
    onDrawerButtonClick: () -> Unit = { },
    onCloseDetection: () -> Unit,
    detectionViewModel: DetectionViewModel,
    appViewModel: AppViewModel,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val appState by appViewModel.appState.collectAsState()
    val detectionState by detectionViewModel.detectionState.collectAsState()

    // Filter results to only include detections relevant to the selected crop
    val detectionResults by remember(detectionState.results, appState.selectedCrop) {
        derivedStateOf {
            val selectedCrop = appState.selectedCrop
            if (selectedCrop.isNullOrEmpty()) {
                detectionState.results
            } else {
                detectionState.results.filter {
                    it.className?.contains(selectedCrop, ignoreCase = true) == true
                }
            }
        }
    }

    val selectedImageBytes = appState.selectedImageByteArray

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = false
    )
    val scope = rememberCoroutineScope()


    Scaffold(
        topBar = {
            AppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                onDrawerButtonClick = onDrawerButtonClick,
                isHomeScreen = false
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackBarHostState,
                modifier = Modifier.padding(8.dp),
                snackbar = {
                    GradientSnackBar(
                        message = it.visuals.message,
                        actionLabel = it.visuals.actionLabel,
                        onAction = { it.dismiss() }
                    )
                }
            )
        },
    ) { contentPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // --- 1. Image Display Area (Fixed at the top) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopCenter)
            ) {
                if (selectedImageBytes != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.0.dp),
                        contentAlignment = Alignment.Center
                    ){
                        BoundingBoxImage(
                            imageBytes = selectedImageBytes,
                            results = detectionResults,
                            modelWidth = 640,
                            modelHeight = 640,
                            contentDescription = stringResource(Res.string.detected_image_content_description),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                } else {
                    // Loading indicator
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // --- 2. Modal Bottom Sheet for Scrollable Content ---
            ModalBottomSheet(
                onDismissRequest = {
                    scope.launch { sheetState.partialExpand() }
                },
                sheetState = sheetState,
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header item with the close button
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.detection_details_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        appViewModel.reset()
                                        detectionViewModel.reset()
                                        onCloseDetection()
                                    },
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            MaterialTheme.shapes.small,
                                        )
                                        .size(40.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            appState.selectedCrop?.let {
                                Text(
                                    text = stringResource(Res.string.selected_crop_label, it),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        // List of detection result cards
                        if (detectionResults.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(Res.string.no_detection_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        } else {
                            items(detectionResults) { result ->
                                DetectionResultCard(result = result)
                                Spacer(modifier = Modifier.height(12.0.dp))
                            }
                        }

                        // --- New: Row with "Flag" and "Recommendations" buttons ---
                        if(detectionResults.isNotEmpty()){
                            item {
                                Box(
                                    modifier = Modifier
                                        .height(15.0.dp)
                                ){}
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                snackBarHostState.showSnackbar(
                                                    "Flagging process initiated"
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                                    ) {
                                        Text(
                                            text = "Flag",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            maxLines = 1,
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                snackBarHostState.showSnackbar(
                                                    "Recommendations button clicked!"
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                                    ) {
                                        Text(
                                            text = "Recommendations",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                        // --- End of New Buttons ---
                    }
                }
            }
        }
    }
}


@Composable
fun DetectionResultCard(result: DetectionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = result.className ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Confidence: ${((result.score * 1000).toInt() / 10.0)}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}