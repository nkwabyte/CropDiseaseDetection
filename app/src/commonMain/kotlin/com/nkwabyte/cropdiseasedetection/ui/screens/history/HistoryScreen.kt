package com.nkwabyte.cropdiseasedetection.ui.screens.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.nkwabyte.cropdiseasedetection.common.data.DiseaseDatabase
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.HistoryViewModel
import com.nkwabyte.cropdiseasedetection.common.pipeline.BOX_COORDINATE_SPACE
import com.nkwabyte.cropdiseasedetection.common.utils.BoundingBoxImage
import com.nkwabyte.cropdiseasedetection.data.repository.DetectionRecord
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import com.nkwabyte.cropdiseasedetection.ui.components.BoxFilterControls
import com.nkwabyte.cropdiseasedetection.ui.components.GroupedCard
import com.nkwabyte.cropdiseasedetection.ui.components.PrimaryActionButton
import com.nkwabyte.cropdiseasedetection.ui.components.WarningColor
import com.nkwabyte.cropdiseasedetection.ui.screens.home.displayNameRes
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * A stable id for a saved scan, usable as a navigation argument and a list key. The
 * Firestore id when there is one; otherwise timestamp + image, because a queued scan and
 * its uploaded twin share a timestamp.
 */
fun DetectionRecord.historyKey(): String = docId ?: "$timestamp-${imageUrl.hashCode()}"

/**
 * The History tab — the counterpart of iOS `HistoryView`. Rows open
 * [HistoryDetailScreen]; swipe a row away (or use its delete button) to hide it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenRecord: (recordKey: String) -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = koinInject(),
) {
    val state by historyViewModel.historyState.collectAsState()
    var pendingDeletion by remember { mutableStateOf<DetectionRecord?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) { historyViewModel.loadHistory() }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppBar(title = stringResource(Res.string.title_scan_history), scrollBehavior = scrollBehavior) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    CircularProgressIndicator()
                    Text(
                        stringResource(Res.string.history_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                state.isGuest -> GuestHistoryLockView(onSignInClick = onSignInClick)

                state.records.isEmpty() -> EmptyHistoryView()

                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { historyViewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.records, key = { it.historyKey() }) { record ->
                            SwipeToDeleteRow(onDelete = { pendingDeletion = record }) {
                                HistoryRecordRow(record = record, onClick = { onOpenRecord(record.historyKey()) })
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeletion?.let { record ->
        DeleteScanDialog(
            onConfirm = {
                historyViewModel.deleteRecord(record)
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null },
        )
    }
}

/** Swipe left to ask for deletion. The row springs back: deletion is confirmed first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteRow(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.history_delete_content_description),
                    tint = MaterialTheme.colorScheme.onError,
                )
            }
        },
    ) { content() }
}

@Composable
private fun DeleteScanDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    // Deleting only hides the scan; the record is retained, so the wording promises
    // exactly that and nothing stronger.
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.history_delete_title)) },
        text = { Text(stringResource(Res.string.history_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(Res.string.history_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel_text)) } },
    )
}

@Composable
private fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
    }
}

@Composable
private fun GuestHistoryLockView(onSignInClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        CircleIcon(Icons.Default.Lock)
        Text(
            stringResource(Res.string.history_locked_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        GroupedCard(cornerRadius = 20) {
            Text(
                stringResource(Res.string.history_locked_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            PrimaryActionButton(text = stringResource(Res.string.sign_in_unlock_history), onClick = onSignInClick)
        }
    }
}

@Composable
private fun EmptyHistoryView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        CircleIcon(Icons.Default.Eco)
        Text(
            stringResource(Res.string.history_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(Res.string.history_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

private fun cropEmoji(cropName: String) = when (cropName.lowercase()) {
    "corn" -> "🌽"
    "tomato" -> "🍅"
    "pepper" -> "🫑"
    else -> "🌱"
}

@Composable
private fun cropLabel(cropName: String): String {
    val crop = SupportedCrop.fromCanonicalId(cropName)
    return if (crop != null) stringResource(crop.displayNameRes()) else cropName.replaceFirstChar { it.uppercase() }
}

@Composable
private fun HistoryRecordRow(record: DetectionRecord, onClick: () -> Unit) {
    val primary = record.matchingResults.maxByOrNull { it.score }
    val noDetection = stringResource(Res.string.history_no_detection)
    val diseaseLabel = primary?.displayName?.ifEmpty { null } ?: noDetection
    val isHealthy = diseaseLabel.contains("healthy", ignoreCase = true)
    val confidence = primary?.let { "${(it.score * 100).toInt()}%" } ?: "0%"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (record.imageUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(record.imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${cropEmoji(record.cropName)} ${cropLabel(record.cropName).uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatTimestamp(record.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                diseaseLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(
                    stringResource(if (isHealthy) Res.string.enc_healthy else Res.string.history_diseased),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tint.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(Res.string.result_confidence, confidence),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Detail ──────────────────────────────────────────────────────────────────

private data class DistinctDisease(val name: String, val count: Int, val maxScore: Float)

/**
 * One saved scan — the counterpart of iOS `HistoryDetailView`: the photo with its boxes,
 * the box filter, scan metadata, the distinct conditions found (each opens the disease
 * page) and the treatment guidelines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    recordKey: String,
    onBack: () -> Unit,
    onOpenDisease: (diseaseId: Int) -> Unit,
    onViewTreatment: () -> Unit,
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = koinInject(),
) {
    val state by historyViewModel.historyState.collectAsState()
    val record = state.records.firstOrNull { it.historyKey() == recordKey }
    var confirmDelete by remember { mutableStateOf(false) }
    var hasLeft by remember { mutableStateOf(false) }

    // The record can vanish from under the page — deleted here (the view model drops it
    // from the list immediately), or the user signed out. Leave once rather than show an
    // empty shell; the guard stops a second pop of the tab's root.
    LaunchedEffect(record == null, state.isLoading) {
        if (record == null && !state.isLoading && !hasLeft) {
            hasLeft = true
            onBack()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AppBar(
                title = stringResource(Res.string.title_scan_details),
                onBack = onBack,
                actions = {
                    if (record != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.history_delete_content_description),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (record == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            HistoryDetailContent(
                record = record,
                onOpenDisease = onOpenDisease,
                onViewTreatment = onViewTreatment,
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (confirmDelete && record != null) {
        DeleteScanDialog(
            onConfirm = {
                confirmDelete = false
                // Leaving is handled by the effect above once the row is gone.
                historyViewModel.deleteRecord(record)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun HistoryDetailContent(
    record: DetectionRecord,
    onOpenDisease: (Int) -> Unit,
    onViewTreatment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unknown = stringResource(Res.string.unknown_disease)
    val results = record.matchingResults
    fun nameOf(r: DetectionResult) = r.displayName.ifEmpty { unknown }

    // Saved scans carry no threshold, so the floor is the weakest stored detection.
    val boxFloor = remember(results) { (results.minOfOrNull { it.score } ?: 0.10f).coerceIn(0.05f, 0.95f) }
    var boxConfidence by remember(boxFloor) { mutableStateOf(boxFloor) }
    var selectedName by remember { mutableStateOf<String?>(null) }

    val names = remember(results) { results.map(::nameOf).distinct() }
    val visibleBoxes = results.filter { r ->
        r.score >= boxConfidence && (selectedName == null || nameOf(r) == selectedName)
    }
    val distinct = remember(results) {
        results.groupBy(::nameOf)
            .map { (name, list) -> DistinctDisease(name, list.size, list.maxOf { it.score }) }
            .sortedByDescending { it.count }
    }
    val isSupportedCrop = remember(record) {
        !record.isCropMismatch && SupportedCrop.fromCanonicalId(record.cropName) != null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        RecordImage(imageUrl = record.imageUrl, boxes = visibleBoxes)

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

        GroupedCard {
            MetadataRow(
                label = stringResource(Res.string.history_crop_category),
                value = if (isSupportedCrop) cropLabel(record.cropName)
                else "${cropLabel(record.cropName)} (${stringResource(Res.string.history_unsupported_crop)})",
                valueColor = if (isSupportedCrop) MaterialTheme.colorScheme.onSurface else WarningColor,
                bold = true,
            )
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            MetadataRow(label = stringResource(Res.string.history_timestamp), value = formatTimestamp(record.timestamp))
        }

        GroupedCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isSupportedCrop) Icons.Default.Verified else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSupportedCrop) MaterialTheme.colorScheme.primary else WarningColor,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.history_detected_conditions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (isSupportedCrop) stringResource(Res.string.history_distinct_count, distinct.size.toString())
                    else stringResource(Res.string.history_unsupported_crop),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSupportedCrop) MaterialTheme.colorScheme.primary else WarningColor,
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            when {
                !isSupportedCrop -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarningColor.copy(alpha = 0.12f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningColor)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(Res.string.history_not_supported_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = WarningColor,
                        )
                        Text(
                            stringResource(Res.string.history_not_supported_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                distinct.isEmpty() -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.result_no_disease_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    distinct.forEach { item ->
                        val pct = "${(item.maxScore * 100).toInt()}%"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable { onOpenDisease(DiseaseDatabase.getDiseaseInfo(item.name).id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    stringResource(
                                        Res.string.history_detected_times,
                                        item.count.toString(),
                                        if (item.count > 1) "s" else "",
                                        pct,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                pct,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
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

        if (results.isNotEmpty()) {
            PrimaryActionButton(
                text = stringResource(Res.string.result_view_treatment),
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = onViewTreatment,
            )
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    bold: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

/** Shared client for fetching saved scan images; one per process is plenty. */
private val imageClient by lazy { HttpClient() }

/**
 * Loads a saved scan's image as raw bytes, so its boxes can be drawn with the same
 * orientation-correct renderer as a live result. Accepts `data:` URIs (offline queue)
 * and http(s) URLs (uploaded scans). Returns null if it cannot be loaded.
 */
@OptIn(ExperimentalEncodingApi::class)
private suspend fun loadImageBytes(imageUrl: String): ByteArray? = runCatching {
    if (imageUrl.startsWith("data:")) {
        Base64.decode(imageUrl.substringAfter(','))
    } else {
        imageClient.get(imageUrl).readRawBytes()
    }
}.getOrNull()

@Composable
private fun RecordImage(imageUrl: String, boxes: List<DetectionResult>) {
    var bytes by remember(imageUrl) { mutableStateOf<ByteArray?>(null) }
    var failed by remember(imageUrl) { mutableStateOf(false) }
    LaunchedEffect(imageUrl) {
        val loaded = if (imageUrl.isBlank()) null else loadImageBytes(imageUrl)
        bytes = loaded
        failed = loaded == null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val loaded = bytes
        when {
            loaded != null -> BoundingBoxImage(
                imageBytes = loaded,
                results = boxes,
                modelWidth = BOX_COORDINATE_SPACE,
                modelHeight = BOX_COORDINATE_SPACE,
                contentDescription = stringResource(Res.string.detected_image_content_description),
                modifier = Modifier.fillMaxSize(),
            )
            // Without the bytes the boxes cannot be drawn, but the photo can still be shown.
            failed && imageUrl.isNotBlank() -> Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            failed -> Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> CircularProgressIndicator()
        }
    }
}

// Pure Kotlin Multiplatform date formatter helper based on Ktor GMTDate
fun formatTimestamp(timestamp: Long): String {
    val date = io.ktor.util.date.GMTDate(timestamp)
    val monthStr = when (date.month) {
        io.ktor.util.date.Month.JANUARY -> "Jan"
        io.ktor.util.date.Month.FEBRUARY -> "Feb"
        io.ktor.util.date.Month.MARCH -> "Mar"
        io.ktor.util.date.Month.APRIL -> "Apr"
        io.ktor.util.date.Month.MAY -> "May"
        io.ktor.util.date.Month.JUNE -> "Jun"
        io.ktor.util.date.Month.JULY -> "Jul"
        io.ktor.util.date.Month.AUGUST -> "Aug"
        io.ktor.util.date.Month.SEPTEMBER -> "Sep"
        io.ktor.util.date.Month.OCTOBER -> "Oct"
        io.ktor.util.date.Month.NOVEMBER -> "Nov"
        io.ktor.util.date.Month.DECEMBER -> "Dec"
    }
    val minutesStr = date.minutes.toString().padStart(2, '0')
    val hoursStr = date.hours.toString().padStart(2, '0')
    return "$monthStr ${date.dayOfMonth}, ${date.year} ${hoursStr}:${minutesStr} GMT"
}
