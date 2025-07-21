package com.nkwabyte.cropdiseasedetection.ui.screens.home

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkwabyte.cropdiseasedetection.R
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.utils.drawBoundingBoxesOnBitmap
import com.nkwabyte.cropdiseasedetection.common.utils.loadBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.ui.components.GradientSnackBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionResultScreen(
    modifier: Modifier = Modifier,
    onDrawerButtonClick: () -> Unit = { },
    onCloseDetection: () -> Unit,
    detectionViewModel: DetectionViewModel,
    appViewModel: AppViewModel,
) {
    val context = LocalContext.current
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

    var processedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val selectedImageUri = appState.selectedImageUri?.toUri()

    // Effect to load the image and draw bounding boxes
    LaunchedEffect(selectedImageUri, detectionResults) {
        if (selectedImageUri == null) {
            withContext(Dispatchers.Main) { processedImageBitmap = null }
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            val originalBitmap = loadBitmapFromUri(context, selectedImageUri)
            if (originalBitmap != null) {
                val bitmapWithBoxes = if (detectionResults.isNotEmpty()) {
                    drawBoundingBoxesOnBitmap(
                        originalBitmap = originalBitmap,
                        results = detectionResults,
                        modelWidth = 640,
                        modelHeight = 640
                    )
                } else {
                    originalBitmap
                }
                withContext(Dispatchers.Main) {
                    processedImageBitmap = bitmapWithBoxes
                }
            } else {
                withContext(Dispatchers.Main) { processedImageBitmap = null }
            }
        }
    }

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = false
    )
    val scope = rememberCoroutineScope()


    Scaffold(
        topBar = {
            AppBar(
                title = { Text(stringResource(R.string.app_name)) },
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
                if (processedImageBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.0.dp),
                        contentAlignment = Alignment.Center
                    ){
                        Image(
                            bitmap = processedImageBitmap!!.asImageBitmap(),
                            contentDescription = stringResource(
                                R.string.detected_image_content_description
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop,
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
                                    text = stringResource(R.string.detection_details_title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.weight(1f) // Allow title to take available space
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
                                            MaterialTheme.shapes.small
                                        )
                                        .size(40.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            appState.selectedCrop?.let {
                                Text(
                                    text = stringResource(R.string.selected_crop_label, it),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }

                        // List of detection result cards
                        if (detectionResults.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_detection_message),
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
                                            Toast.makeText(
                                                context,
                                                "Flag button clicked!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                                    ) {
                                        BasicText(
                                            text = "Flag",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            maxLines = 1,
                                            autoSize = TextAutoSize.StepBased(
                                                minFontSize = 12.sp,
                                                maxFontSize = 18.sp,
                                            )
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            Toast.makeText(
                                                context,
                                                "Recommendations button clicked!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                                    ) {
                                        BasicText(
                                            text = "Recommendations",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            ),
                                            maxLines = 1,
                                            autoSize = TextAutoSize.StepBased(
                                                minFontSize = 12.sp,
                                                maxFontSize = 18.sp,
                                            )
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
                text = "Confidence: %.1f%%".format(result.score * 100),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}