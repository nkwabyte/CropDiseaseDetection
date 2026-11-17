package com.nkwabyte.cropdiseasedetection.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseDatabase
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.FlagState
import com.nkwabyte.cropdiseasedetection.common.pipeline.BOX_COORDINATE_SPACE
import com.nkwabyte.cropdiseasedetection.common.utils.BoundingBoxImage
import com.nkwabyte.cropdiseasedetection.generated.resources.Res
import com.nkwabyte.cropdiseasedetection.generated.resources.action_done
import com.nkwabyte.cropdiseasedetection.generated.resources.detected_image_content_description
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_cancel_button
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_dialog_message
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_dialog_title
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_error_prefix
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_notes_hint
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_submit_button
import com.nkwabyte.cropdiseasedetection.generated.resources.flag_success_message
import com.nkwabyte.cropdiseasedetection.generated.resources.result_confidence
import com.nkwabyte.cropdiseasedetection.generated.resources.result_found_count
import com.nkwabyte.cropdiseasedetection.generated.resources.result_mismatch_message
import com.nkwabyte.cropdiseasedetection.generated.resources.result_mismatch_title
import com.nkwabyte.cropdiseasedetection.generated.resources.result_no_disease_message
import com.nkwabyte.cropdiseasedetection.generated.resources.result_no_disease_title
import com.nkwabyte.cropdiseasedetection.generated.resources.result_report_inaccurate
import com.nkwabyte.cropdiseasedetection.generated.resources.result_summary_title
import com.nkwabyte.cropdiseasedetection.generated.resources.result_unrecognized_message
import com.nkwabyte.cropdiseasedetection.generated.resources.result_unrecognized_title
import com.nkwabyte.cropdiseasedetection.generated.resources.result_view_treatment
import com.nkwabyte.cropdiseasedetection.generated.resources.title_diagnostic_results
import com.nkwabyte.cropdiseasedetection.generated.resources.unknown_label
import com.nkwabyte.cropdiseasedetection.ui.components.BoxFilterControls
import com.nkwabyte.cropdiseasedetection.ui.components.GradientSnackBar
import com.nkwabyte.cropdiseasedetection.ui.components.GroupedCard
import com.nkwabyte.cropdiseasedetection.ui.components.PrimaryActionButton
import com.nkwabyte.cropdiseasedetection.ui.components.StatusBanner
import com.nkwabyte.cropdiseasedetection.ui.components.WarningColor
import org.jetbrains.compose.resources.stringResource

/**
 * The result of one scan — the counterpart of iOS `DetectionResultView`: the photo with
 * its boxes, the box filter, one status banner for the outcome, the per-disease summary
 * (each row opens the disease page), and the treatment / report actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionResultScreen(
    detectionViewModel: DetectionViewModel,
    appViewModel: AppViewModel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onOpenDisease: (diseaseId: Int) -> Unit,
    onViewTreatment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val appState by appViewModel.appState.collectAsState()
    val state by detectionViewModel.detectionState.collectAsState()
    val flagState by detectionViewModel.flagState.collectAsState()
    var showFlagDialog by remember { mutableStateOf(false) }

    // Already routed: the pipeline filtered these to the classifier-derived crop's class
    // ids, so this screen must not filter by crop again.
    val results = state.results
    val unknown = stringResource(Res.string.unknown_label)
    fun nameOf(r: DetectionResult) = r.displayName.ifEmpty { unknown }

    // The box filters change what is DRAWN only. The floor is the inference threshold:
    // nothing weaker was detected, so every slider position reveals or hides something.
    val boxFloor = appState.detectionThreshold.coerceIn(0f, 0.95f)
    var boxConfidence by remember(boxFloor) { mutableStateOf(boxFloor) }
    var selectedName by remember { mutableStateOf<String?>(null) }

    val names = remember(results) { results.map(::nameOf).distinct() }
    val visibleBoxes = results.filter { r ->
        r.score >= boxConfidence && (selectedName == null || nameOf(r) == selectedName)
    }
    // One row per disease, carrying its strongest detection.
    val summary = remember(results) {
        results.groupBy { it.displayName.ifEmpty { it.classIndex.toString() } }
            .map { (_, list) -> list.maxBy { it.score } }
            .sortedByDescending { it.score }
    }

    val flagSuccess = stringResource(Res.string.flag_success_message)
    val flagErrorTemplate = stringResource(Res.string.flag_error_prefix, "%s")
    LaunchedEffect(flagState) {
        when (val s = flagState) {
            is FlagState.Success -> {
                snackBarHostState.showSnackbar(flagSuccess)
                detectionViewModel.resetFlagState()
            }
            is FlagState.Error -> {
                snackBarHostState.showSnackbar(flagErrorTemplate.replace("%s", s.message))
                detectionViewModel.resetFlagState()
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBar(
                title = stringResource(Res.string.title_diagnostic_results),
                onBack = onBack,
                actions = {
                    TextButton(onClick = onDone) {
                        Text(stringResource(Res.string.action_done), fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackBarHostState, modifier = Modifier.padding(8.dp)) {
                GradientSnackBar(message = it.visuals.message, actionLabel = it.visuals.actionLabel, onAction = { it.dismiss() })
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScanImage(imageBytes = appState.selectedImageByteArray, boxes = visibleBoxes)

            if (results.isNotEmpty()) {
                BoxFilterControls(
                    names = names,
                    totalCount = results.size,
                    visibleCount = visibleBoxes.size,
                    countFor = { n -> results.count { nameOf(it) == n } },
                    selectedName = selectedName,
                    onSelectName = { selectedName = it },
                    confidence = boxConfidence,
                    onConfidenceChange = { boxConfidence = it },
                    floor = boxFloor,
                )
            }

            when {
                state.isClassifierRejected -> StatusBanner(
                    icon = Icons.Default.Warning,
                    tint = WarningColor,
                    title = stringResource(Res.string.result_unrecognized_title),
                    message = stringResource(
                        Res.string.result_unrecognized_message,
                        "${(state.classifierConfidence * 100).toInt()}%",
                    ),
                )

                state.isCropMissMatch -> StatusBanner(
                    icon = Icons.Default.Cancel,
                    tint = MaterialTheme.colorScheme.error,
                    title = stringResource(Res.string.result_mismatch_title),
                    message = stringResource(Res.string.result_mismatch_message),
                )

                state.isDetectionSuccessful -> DetectionSummaryCard(
                    summary = summary,
                    totalFound = results.size,
                    nameOf = ::nameOf,
                    onOpen = { r ->
                        val info = DiseaseDatabase.getDiseaseInfo(nameOf(r))
                        onOpenDisease(info.id)
                    },
                )

                else -> StatusBanner(
                    icon = Icons.Default.Eco,
                    tint = MaterialTheme.colorScheme.primary,
                    title = stringResource(Res.string.result_no_disease_title),
                    message = stringResource(Res.string.result_no_disease_message),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isDetectionSuccessful) {
                    PrimaryActionButton(
                        text = stringResource(Res.string.result_view_treatment),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onViewTreatment,
                    )
                }
                TextButton(
                    onClick = { showFlagDialog = true },
                    enabled = flagState !is FlagState.Loading && appState.selectedImageByteArray != null,
                ) {
                    if (flagState is FlagState.Loading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.result_report_inaccurate), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
                    // The role must be one the Firestore rules accept (FARMER/FIELD_AGENT).
                    userRole = appState.userRole.name,
                    notes = notes.ifBlank { null },
                    detectionThreshold = appState.detectionThreshold,
                    iouThreshold = appState.iouThreshold,
                    classifierThreshold = appState.classifierThreshold,
                )
            },
        )
    }
}

@Composable
private fun ScanImage(imageBytes: ByteArray?, boxes: List<DetectionResult>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBytes != null) {
            BoundingBoxImage(
                imageBytes = imageBytes,
                results = boxes,
                // The square canvas detections are expressed in — not the photo's size.
                modelWidth = BOX_COORDINATE_SPACE,
                modelHeight = BOX_COORDINATE_SPACE,
                contentDescription = stringResource(Res.string.detected_image_content_description),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun DetectionSummaryCard(
    summary: List<DetectionResult>,
    totalFound: Int,
    nameOf: (DetectionResult) -> String,
    onOpen: (DetectionResult) -> Unit,
) {
    GroupedCard(cornerRadius = 14) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.result_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.result_found_count, totalFound.toString()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            summary.forEach { r ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpen(r) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(nameOf(r), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(Res.string.result_confidence, "${(r.score * 100).toInt()}%"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { r.score },
                        modifier = Modifier.width(80.dp),
                        color = if (r.score > 0.6f) MaterialTheme.colorScheme.primary else WarningColor,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FlagDetectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (notes: String) -> Unit,
) {
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.flag_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.flag_dialog_message), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text(stringResource(Res.string.flag_notes_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(notes) }) {
                Text(stringResource(Res.string.flag_submit_button), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.flag_cancel_button)) }
        },
    )
}
