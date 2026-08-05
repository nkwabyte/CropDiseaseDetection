package com.nkwabyte.cropdiseasedetection.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.FlagState
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.UserRole
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseDatabase
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseInfo
import com.nkwabyte.cropdiseasedetection.ui.screens.encyclopedia.DiseaseDetailDialog
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
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_button_text
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_dialog_title
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_dialog_message
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_notes_hint
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_submit_button
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_cancel_button
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_success_message
import org.jetbrains.compose.resources.stringResource
import com.nkwabyte.cropdiseasedetection.common.utils.BoundingBoxImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionResultScreen(
    modifier: Modifier = Modifier,
    onDrawerButtonClick: () -> Unit = { },
    onCloseDetection: () -> Unit,
    onNavigateToRecommendations: () -> Unit = {},
    detectionViewModel: DetectionViewModel,
    appViewModel: AppViewModel,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val appState by appViewModel.appState.collectAsState()
    val detectionState by detectionViewModel.detectionState.collectAsState()
    val flagState by detectionViewModel.flagState.collectAsState()
    var selectedDisease by remember { mutableStateOf<DiseaseInfo?>(null) }
    var showFlagDialog by remember { mutableStateOf(false) }

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

    // Boxes are drawn for every detection that cleared the inference threshold, which on a
    // leaf with many lesions buries the image. This filters what is drawn only — the
    // detections themselves are untouched, so raising it never changes the diagnosis below.
    // The floor is the inference threshold: nothing weaker than that exists to reveal, so
    // starting there keeps every slider position meaningful.
    val boxFloor = appState.detectionThreshold.coerceIn(0f, 0.95f)
    var boxConfidence by remember(boxFloor) { mutableStateOf(boxFloor) }

    val visibleBoxes by remember(detectionResults, boxConfidence) {
        derivedStateOf { detectionResults.filter { it.score >= boxConfidence } }
    }

    val distinctDetectionResults by remember(detectionResults) {
        derivedStateOf {
            detectionResults
                .groupBy { it.displayName.ifEmpty { it.classIndex.toString() } }
                .mapValues { (_, list) -> list.maxByOrNull { it.score }!! }
                .values
                .sortedByDescending { it.score }
        }
    }

    val selectedImageBytes = appState.selectedImageByteArray

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = false
    )
    val scope = rememberCoroutineScope()

    val flagSuccessMessage = stringResource(Res.string.flag_success_message)

    LaunchedEffect(flagState) {
        when (val state = flagState) {
            is FlagState.Success -> {
                snackBarHostState.showSnackbar(flagSuccessMessage)
                detectionViewModel.resetFlagState()
            }
            is FlagState.Error -> {
                snackBarHostState.showSnackbar("Flag failed: ${state.message}")
                detectionViewModel.resetFlagState()
            }
            else -> {}
        }
    }

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
                    ) {
                        BoundingBoxImage(
                            imageBytes = selectedImageBytes,
                            results = visibleBoxes,
                            modelWidth = 640,
                            modelHeight = 640,
                            contentDescription = stringResource(Res.string.detected_image_content_description),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
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

                            if (detectionResults.isNotEmpty()) {
                                BoxConfidenceSlider(
                                    value = boxConfidence,
                                    onValueChange = { boxConfidence = it },
                                    valueRange = boxFloor..1f,
                                    shown = visibleBoxes.size,
                                    total = detectionResults.size
                                )
                            }
                        }

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
                            items(distinctDetectionResults) { result ->
                                DetectionResultCard(
                                    result = result,
                                    onClick = {
                                        selectedDisease = DiseaseDatabase.diseases.find { disease ->
                                            result.displayName.isNotEmpty() &&
                                                disease.name.contains(result.displayName, ignoreCase = true)
                                        } ?: DiseaseDatabase.diseases.getOrNull(result.classIndex)
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.0.dp))
                            }
                        }

                        if (detectionResults.isNotEmpty()) {
                            item {
                                Box(modifier = Modifier.height(15.0.dp)) {}
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (appState.userRole == UserRole.FIELD_AGENT) {
                                        Button(
                                            onClick = { showFlagDialog = true },
                                            modifier = Modifier.weight(1f).padding(end = 4.dp),
                                            enabled = flagState !is FlagState.Loading
                                        ) {
                                            if (flagState is FlagState.Loading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text(
                                                    text = stringResource(Res.string.flag_button_text),
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        textAlign = TextAlign.Center,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                    ),
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                    Button(
                                        onClick = onNavigateToRecommendations,
                                        modifier = Modifier.weight(1f).padding(
                                            start = if (appState.userRole == UserRole.FIELD_AGENT) 4.dp else 0.dp
                                        )
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
                    }
                }
            }
        }
    }

    selectedDisease?.let { disease ->
        DiseaseDetailDialog(
            disease = disease,
            onDismiss = { selectedDisease = null }
        )
    }

    if (showFlagDialog) {
        FlagDetectionDialog(
            onDismiss = { showFlagDialog = false },
            onConfirm = { notes ->
                showFlagDialog = false
                val imageBytes = appState.selectedImageByteArray ?: return@FlagDetectionDialog
                detectionViewModel.flagDetection(
                    imageBytes = imageBytes,
                    cropName = appState.selectedCrop ?: "Unknown",
                    userRole = appState.userRole.name,
                    notes = notes.ifBlank { null },
                    detectionThreshold = appState.detectionThreshold,
                    iouThreshold = appState.iouThreshold,
                    classifierThreshold = appState.classifierThreshold
                )
            }
        )
    }
}

/**
 * Controls how many boxes are drawn over the image, by hiding the weaker detections.
 * The live "N of M" readout is what makes the control legible — without it, dragging
 * on an image whose boxes are all similarly scored looks like nothing is happening.
 */
@Composable
private fun BoxConfidenceSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    shown: Int,
    total: Int
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Box confidence",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "$shown of $total  ·  ${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun FlagDetectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (notes: String) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.flag_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(Res.string.flag_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text(stringResource(Res.string.flag_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(notes) }) {
                Text(stringResource(Res.string.flag_submit_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.flag_cancel_button))
            }
        }
    )
}

@Composable
fun DetectionResultCard(
    result: DetectionResult,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.displayName.ifEmpty { "Unknown" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confidence: ${((result.score * 1000).toInt() / 10.0)}%",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View disease details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
