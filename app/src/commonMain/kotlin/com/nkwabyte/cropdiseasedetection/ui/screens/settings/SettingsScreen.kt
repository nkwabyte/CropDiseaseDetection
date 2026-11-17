package com.nkwabyte.cropdiseasedetection.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_EXTENDED_MEASURED_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_EXTENDED_WARMUP_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelCatalog
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelSpec
import com.nkwabyte.cropdiseasedetection.common.model.ModelHosting
import com.nkwabyte.cropdiseasedetection.common.models.ModelDownloadManager
import com.nkwabyte.cropdiseasedetection.common.models.ModelDownloadState
import com.nkwabyte.cropdiseasedetection.common.model.RecommendationLanguage
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager
import com.nkwabyte.cropdiseasedetection.common.utils.formatDecimals
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import com.nkwabyte.cropdiseasedetection.ui.components.GroupedCard
import com.nkwabyte.cropdiseasedetection.ui.components.GroupedDivider
import com.nkwabyte.cropdiseasedetection.ui.components.GroupedRow
import com.nkwabyte.cropdiseasedetection.ui.components.SectionFooter
import com.nkwabyte.cropdiseasedetection.ui.components.SectionHeader
import com.nkwabyte.cropdiseasedetection.ui.components.WarningColor
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Stored theme values (persisted as-is) and their translated labels. */
private val themeOptions: List<Pair<String, StringResource>> = listOf(
    "System Default" to Res.string.theme_system_default,
    "Light" to Res.string.theme_light,
    "Dark" to Res.string.theme_dark,
)

private fun RecommendationLanguage.label() = "$displayName $flag"

/**
 * The Settings tab — the counterpart of iOS `SettingsView`, with the same sections in the
 * same order: model, appearance, localization, ML thresholds, the developer benchmark,
 * about & support, and app info. Account and data-management actions live on the
 * Account tab, as they do on iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenAbout: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = koinInject(),
) {
    val appState by appViewModel.appState.collectAsState()
    val settingsManager: SettingsManager = koinInject()
    val appVersion = remember { settingsManager.getAppVersion() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showModelDialog by remember { mutableStateOf(false) }
    var downloadPrompt by remember { mutableStateOf<DetectionModelSpec?>(null) }
    var showDownloadsUnavailable by remember { mutableStateOf(false) }
    val modelDownloadManager: ModelDownloadManager = koinInject()
    val modelDownloads by modelDownloadManager.downloads.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val currentModel = DetectionModelCatalog.byId(appState.selectedDetectionModel)
    val themeLabel = themeOptions.firstOrNull { it.first == appState.selectedTheme }?.second
        ?: Res.string.theme_system_default

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AppBar(title = stringResource(Res.string.tab_settings), scrollBehavior = scrollBehavior) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Section(Res.string.settings_section_model) {
                ValueRow(
                    title = stringResource(Res.string.settings_detection_model),
                    value = currentModel.displayName,
                    onClick = { showModelDialog = true },
                )
                DetectionModelCatalog.all.filter { it.requiresDownload }.forEach { spec ->
                    ModelDownloadStatus(
                        spec = spec,
                        state = modelDownloads.stateOf(spec.id),
                        onCancel = { modelDownloadManager.cancel(spec.id) },
                        onRetry = { appViewModel.downloadDetectionModel(spec.id) },
                        onDismissError = { modelDownloadManager.dismissError(spec.id) },
                    )
                }
            }

            Section(Res.string.settings_section_appearance) {
                ValueRow(
                    title = stringResource(Res.string.settings_theme_mode),
                    value = stringResource(themeLabel),
                    onClick = { showThemeDialog = true },
                )
            }

            Section(Res.string.settings_section_localization) {
                ValueRow(
                    title = stringResource(Res.string.settings_language),
                    value = appState.recommendationLanguage.label(),
                    onClick = { showLanguageDialog = true },
                )
            }

            Section(
                title = Res.string.settings_section_thresholds,
                footer = Res.string.settings_thresholds_footer,
            ) {
                PercentSlider(
                    title = stringResource(Res.string.settings_detection_threshold),
                    value = appState.detectionThreshold,
                    range = 0.05f..0.90f,
                    onValueChange = appViewModel::setDetectionThreshold,
                )
                GroupedDivider()
                PercentSlider(
                    title = stringResource(Res.string.settings_iou_threshold),
                    value = appState.iouThreshold,
                    range = 0.05f..0.90f,
                    onValueChange = appViewModel::setIouThreshold,
                )
                GroupedDivider()
                PercentSlider(
                    title = stringResource(Res.string.settings_classifier_threshold),
                    value = appState.classifierThreshold,
                    range = 0.10f..0.95f,
                    onValueChange = appViewModel::setClassifierThreshold,
                )
            }

            DeveloperBenchmarkSection(snackbarHostState)

            Section(Res.string.settings_section_about) {
                GroupedRow(
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(Res.string.settings_about_app),
                    onClick = onOpenAbout,
                )
                GroupedDivider()
                GroupedRow(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(Res.string.title_help_support),
                    onClick = onOpenHelp,
                )
            }

            Section(Res.string.settings_section_app_info) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.settings_app_version), modifier = Modifier.weight(1f))
                    Text(appVersion, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showModelDialog) {
        // Picking a model whose weights are not on the device asks whether to download
        // it or stay on the bundled default; one that is downloading just shows progress.
        ModelPickerDialog(
            selectedId = currentModel.id,
            downloads = modelDownloads,
            onSelect = { spec ->
                showModelDialog = false
                when {
                    modelDownloadManager.isReady(spec) -> appViewModel.setSelectedDetectionModel(spec.id)
                    modelDownloads.stateOf(spec.id) is ModelDownloadState.Downloading -> Unit
                    else -> downloadPrompt = spec
                }
            },
            onDelete = { spec -> appViewModel.deleteDownloadedModel(spec.id) },
            onDismiss = { showModelDialog = false },
        )
    }

    downloadPrompt?.let { spec ->
        ModelDownloadPrompt(
            spec = spec,
            onDownload = {
                if (ModelHosting.DOWNLOADS_ENABLED) appViewModel.downloadDetectionModel(spec.id)
                else showDownloadsUnavailable = true
            },
            onUseDefault = { appViewModel.useDefaultDetectionModel() },
            onDismiss = { downloadPrompt = null },
        )
    }

    if (showDownloadsUnavailable) {
        ModelDownloadsUnavailableDialog(onDismiss = { showDownloadsUnavailable = false })
    }

    if (showThemeDialog) {
        val labels = themeOptions.map { (value, res) -> stringResource(res) to value }
        SelectionDialog(
            title = stringResource(Res.string.settings_theme_mode),
            options = labels.map { it.first },
            selectedOption = stringResource(themeLabel),
            onOptionSelected = { label -> labels.firstOrNull { it.first == label }?.let { appViewModel.setSelectedTheme(it.second) } },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showLanguageDialog) {
        val langMap = RecommendationLanguage.entries.associateBy { it.label() }
        SelectionDialog(
            title = stringResource(Res.string.settings_language),
            options = langMap.keys.toList(),
            selectedOption = appState.recommendationLanguage.label(),
            onOptionSelected = { key -> langMap[key]?.let(appViewModel::setRecommendationLanguage) },
            onDismiss = { showLanguageDialog = false },
        )
    }
}

/** A Form section: header, a grouped card of rows, and an optional footer. */
@Composable
private fun Section(
    title: StringResource,
    footer: StringResource? = null,
    content: @Composable () -> Unit,
) {
    Column {
        SectionHeader(stringResource(title))
        GroupedCard(padding = 0) { content() }
        if (footer != null) SectionFooter(stringResource(footer))
    }
}

/** "Label ........ value ›" — an iOS Form picker row. */
@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A threshold slider shown as a percentage, snapped to 5% steps like iOS. */
@Composable
private fun PercentSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    val steps = ((range.endInclusive - range.start) / 0.05f).toInt() - 1
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                "${(value * 100).toInt()}%",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps.coerceAtLeast(0),
        )
    }
}

/**
 * "Developer — Benchmark": the two shared benchmark entry points. No benchmark logic
 * lives here — each action is one call into [ObjectDetector], which writes the export
 * files itself.
 */
@Composable
private fun DeveloperBenchmarkSection(snackbarHostState: SnackbarHostState) {
    val objectDetector: ObjectDetector = koinInject()
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<String?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var wasEmulator by remember { mutableStateOf<Boolean?>(null) }

    fun start(label: String, block: suspend () -> Unit) {
        if (running != null) return
        running = label
        summary = null
        failure = null
        wasEmulator = null
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                failure = "$label failed: ${e.message}"
                snackbarHostState.showSnackbar(failure!!)
            } finally {
                running = null
            }
        }
    }

    Column {
        SectionHeader(stringResource(Res.string.settings_section_benchmark))
        GroupedCard(padding = 0) {
            BenchmarkRow(
                icon = Icons.Default.Speed,
                title = "Run Latency Benchmark",
                subtitle = "Quick sanity check — 10 warm-up + 50 measured runs, CSV to app storage.",
                enabled = running == null,
                onClick = {
                    start("Latency benchmark") {
                        objectDetector.loadClassifierModel()
                        objectDetector.loadModel()
                        val results = objectDetector.runLatencyBenchmark()
                        summary = if (results.isEmpty()) {
                            "Benchmark produced no measurements — load a model first, then try again."
                        } else {
                            results.joinToString("  •  ") { "${it.stage}: ${it.stats.meanMs.formatDecimals(1)} ms mean" }
                        }
                    }
                },
            )
            GroupedDivider()
            BenchmarkRow(
                icon = Icons.Default.BarChart,
                title = "Run Extended Benchmark (Publication Protocol)",
                subtitle = "Full protocol — $BENCHMARK_EXTENDED_WARMUP_RUNS warm-up + $BENCHMARK_EXTENDED_MEASURED_RUNS " +
                    "measured runs per condition. Takes a while.",
                enabled = running == null,
                onClick = {
                    start("Extended benchmark") {
                        val export = objectDetector.runExtendedBenchmark()
                        val env = export.deviceEnvironment
                        wasEmulator = env.isEmulator
                        summary = buildString {
                            append("schema v${export.schemaVersion} · ${env.manufacturer} ${env.model} · ${env.osVersion}\n")
                            append(
                                "end-to-end mean ${export.endToEnd.stats.meanMs.formatDecimals(1)} ms, " +
                                    "p95 ${export.endToEnd.stats.p95Ms.formatDecimals(1)} ms\n"
                            )
                            append("${export.notes.size} caveat(s) in the export — read notes before citing any figure")
                        }
                    }
                },
            )

            val label = running
            when {
                label != null -> Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Running ${label.lowercase()}…", style = MaterialTheme.typography.bodyMedium)
                }

                failure != null -> Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(failure!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                summary != null -> Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Benchmark complete", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    wasEmulator?.let { emulator ->
                        // Stated in the UI, not just the export notes: these figures are most
                        // often misused by being quoted as physical-device performance.
                        Text(
                            if (emulator) "Emulator run — NOT device performance" else "Physical device (isEmulator = false)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (emulator) WarningColor else MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(summary!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SectionFooter(
            "Runs the app's own in-process instrumentation against the loaded ExecuTorch models. Timing comes " +
                "from monotonic clocks inside the shared implementation; nothing here measures UI or automation overhead."
        )
    }
}

@Composable
private fun BenchmarkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    /** Shown greyed out and inert — listed so the user knows the option exists. */
    disabledOptions: Set<String> = emptySet()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    val isDisabled = option in disabledOptions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDisabled) {
                                onOptionSelected(option)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = null, // handled by row click
                            enabled = !isDisabled,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isDisabled) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            } else {
                                Color.Unspecified
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel_text), color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}
