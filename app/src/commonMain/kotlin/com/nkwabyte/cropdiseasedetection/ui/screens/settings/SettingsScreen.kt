package com.nkwabyte.cropdiseasedetection.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.utils.formatDecimals
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelCatalog
import com.nkwabyte.cropdiseasedetection.common.model.RecommendationLanguage
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.nkwabyte.cropdiseasedetection.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onDrawerButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel = koinInject()
) {
    val appState by appViewModel.appState.collectAsState()
    val selectedTheme = appState.selectedTheme

    // Mock States for UI
    var selectedLanguage by remember { mutableStateOf("English") }
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var emailUpdatesEnabled by remember { mutableStateOf(false) }

    // Dialog States
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    // Action States
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }

    // Helpers
    val settingsManager: com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager = koinInject()
    val appVersion = remember { settingsManager.getAppVersion() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val syncRepository: SyncRepository = koinInject()
    val objectDetector: ObjectDetector = koinInject()
    var benchmarkRunning by remember { mutableStateOf(false) }
    var benchmarkSummary by remember { mutableStateOf<String?>(null) }
    var extendedBenchmarkRunning by remember { mutableStateOf(false) }
    var extendedBenchmarkSummary by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppBar(
                title = { 
                    Text(
                        "Settings", 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    ) 
                },
                onDrawerButtonClick = onDrawerButtonClick,
                isHomeScreen = false
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // General Section
                SettingsSectionHeader("General")
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "${appState.recommendationLanguage.flag} ${appState.recommendationLanguage.displayName} (${appState.recommendationLanguage.nativeName})",
                        onClick = { showLanguageDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsClickableRow(
                        icon = Icons.Default.DarkMode,
                        title = "Theme",
                        subtitle = selectedTheme,
                        onClick = { showThemeDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Notifications Section
                SettingsSectionHeader("Notifications")
                SettingsCard {
                    SettingsSwitchRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Push Notifications",
                        subtitle = "Receive scan alerts and tips",
                        checked = pushNotificationsEnabled,
                        onCheckedChange = { pushNotificationsEnabled = it }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSwitchRow(
                        icon = Icons.Default.Email,
                        title = "Email Updates",
                        subtitle = "Monthly newsletter and reports",
                        checked = emailUpdatesEnabled,
                        onCheckedChange = { emailUpdatesEnabled = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Data & Privacy Section
                SettingsSectionHeader("Data & Privacy")
                SettingsCard {
                    SettingsActionRow(
                        icon = Icons.Default.CleaningServices,
                        title = "Clear Cache",
                        subtitle = "Free up local storage space",
                        titleColor = MaterialTheme.colorScheme.onSurface,
                        onClick = { showClearCacheDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsActionRow(
                        icon = Icons.Default.DeleteForever,
                        title = "Anonymize My Data",
                        subtitle = "Anonymize your data for research",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showDeleteDataDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Disease Detection Model Section
                SettingsSectionHeader("Disease Detection Model")
                SettingsCard {
                    SettingsClickableRow(
                        icon = Icons.Default.DarkMode,
                        title = "Select Object Detection Model",
                        subtitle = DetectionModelCatalog.byId(appState.selectedDetectionModel).displayName,
                        onClick = { showModelDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Model Thresholds Section
                SettingsSectionHeader("Model Thresholds")
                SettingsCard {
                    SettingsSliderRow(
                        title = "Classifier Confidence",
                        subtitle = "Minimum confidence to accept crop class",
                        value = appState.classifierThreshold,
                        onValueChange = { appViewModel.setClassifierThreshold(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSliderRow(
                        title = "Detection Score",
                        subtitle = "Minimum score to detect diseases",
                        value = appState.detectionThreshold,
                        onValueChange = { appViewModel.setDetectionThreshold(it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsSliderRow(
                        title = "Intersection over Union (IoU)",
                        subtitle = "Overlap threshold for duplicate boxes",
                        value = appState.iouThreshold,
                        onValueChange = { appViewModel.setIouThreshold(it) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Developer - Benchmark Section
                // Added for Phase 3 of the accompanying research project: measures
                // real on-device latency (warmup + repeated timed runs, synthetic
                // images) so the README's previously unmeasured "~18ms"/"~24ms"/
                // "sub-100ms" claims can be replaced with an instrumented number.
                SettingsSectionHeader("Developer — Benchmark")
                SettingsCard {
                    SettingsActionRow(
                        icon = Icons.Default.Speed,
                        title = if (benchmarkRunning) "Running benchmark…" else "Run Latency Benchmark",
                        subtitle = benchmarkSummary
                            ?: "Measures on-device classifier + detector latency (warmup + 50 timed runs) and saves a CSV",
                        titleColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            if (!benchmarkRunning) {
                                benchmarkRunning = true
                                benchmarkSummary = null
                                coroutineScope.launch {
                                    try {
                                        objectDetector.loadClassifierModel()
                                        objectDetector.loadModel()
                                        val results = objectDetector.runLatencyBenchmark()
                                        benchmarkSummary = if (results.isEmpty()) {
                                            "No measurements — load a model first, then try again"
                                        } else {
                                            results.joinToString("  •  ") { r ->
                                                "${r.stage}: ${r.stats.meanMs.formatDecimals(1)}ms mean"
                                            }
                                        }
                                        snackbarHostState.showSnackbar(
                                            if (results.isEmpty()) {
                                                "Benchmark produced no measurements"
                                            } else {
                                                "Benchmark complete — CSV saved to app storage (${results.size} result rows)"
                                            }
                                        )
                                    } catch (e: Exception) {
                                        benchmarkSummary = "Benchmark failed: ${e.message}"
                                        snackbarHostState.showSnackbar("Benchmark failed: ${e.message}")
                                    } finally {
                                        benchmarkRunning = false
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsActionRow(
                        icon = Icons.Default.Speed,
                        title = if (extendedBenchmarkRunning) "Running extended benchmark…" else "Run Extended Benchmark (Publication Protocol)",
                        subtitle = extendedBenchmarkSummary
                            ?: "Cold load, per-stage, end-to-end, memory/CPU/thermal (10 warmup + 100 measured runs); flags metrics this device can't measure defensibly. Saves JSON+CSV.",
                        titleColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            if (!extendedBenchmarkRunning) {
                                extendedBenchmarkRunning = true
                                extendedBenchmarkSummary = null
                                coroutineScope.launch {
                                    try {
                                        val export = objectDetector.runExtendedBenchmark()
                                        val flaggedCount = export.notes.size
                                        extendedBenchmarkSummary = "end-to-end mean ${export.endToEnd.stats.meanMs.formatDecimals(1)}ms, " +
                                            "p95 ${export.endToEnd.stats.p95Ms.formatDecimals(1)}ms  •  " +
                                            "${export.endToEnd.offlineSuccessCount} ok / ${export.endToEnd.offlineFailureCount} failed  •  " +
                                            "$flaggedCount caveat(s) in export — see JSON/CSV notes before citing any figure"
                                        snackbarHostState.showSnackbar(
                                            "Extended benchmark complete on ${export.deviceEnvironment.manufacturer} ${export.deviceEnvironment.model}" +
                                                if (export.deviceEnvironment.isEmulator) " (EMULATOR — not physical-device performance)" else ""
                                        )
                                    } catch (e: Exception) {
                                        extendedBenchmarkSummary = "Extended benchmark failed: ${e.message}"
                                        snackbarHostState.showSnackbar("Extended benchmark failed: ${e.message}")
                                    } finally {
                                        extendedBenchmarkRunning = false
                                    }
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // App Info Section
                SettingsSectionHeader("App Info")
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "App Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "App Version",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = appVersion,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // Dialogs
    if (showLanguageDialog) {
        val langMap = RecommendationLanguage.entries.associateBy { "${it.flag} ${it.displayName} (${it.nativeName})" }
        val currentDisplay = "${appState.recommendationLanguage.flag} ${appState.recommendationLanguage.displayName} (${appState.recommendationLanguage.nativeName})"
        
        SelectionDialog(
            title = "Select Language",
            options = langMap.keys.toList(),
            selectedOption = currentDisplay,
            onOptionSelected = { selectedKey ->
                langMap[selectedKey]?.let { selectedLang ->
                    appViewModel.setRecommendationLanguage(selectedLang)
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = "Select Theme",
            options = listOf("Light", "Dark", "System Default"),
            selectedOption = selectedTheme,
            onOptionSelected = { appViewModel.setSelectedTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showModelDialog) {
        // Every model is listed, but the ones with no shippable weights are marked and
        // inert — selecting one would load a detector that returns nothing.
        val optionsMap = DetectionModelCatalog.all.associate { it.listLabel to it.id }
        val current = DetectionModelCatalog.byId(appState.selectedDetectionModel)

        SelectionDialog(
            title = "Select Disease Detection Model",
            options = optionsMap.keys.toList(),
            selectedOption = current.listLabel,
            onOptionSelected = { selectedKey ->
                optionsMap[selectedKey]?.let { modelKey ->
                    appViewModel.setSelectedDetectionModel(modelKey)
                }
            },
            onDismiss = { showModelDialog = false },
            disabledOptions = DetectionModelCatalog.all
                .filterNot { it.available }
                .map { it.listLabel }
                .toSet()
        )
    }

    val clearCacheSuccessMsg = stringResource(Res.string.settings_clear_cache_success)
    val anonymizeSuccessMsg = stringResource(Res.string.settings_anonymize_success)
    val anonymizeErrorMsg = stringResource(Res.string.settings_anonymize_error)

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(Res.string.settings_clear_cache_title)) },
            text = { Text(stringResource(Res.string.settings_clear_cache_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    // Simulate clearing cache
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(clearCacheSuccessMsg)
                    }
                }) {
                    Text(stringResource(Res.string.clear_button_text), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(Res.string.cancel_text), color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = { Text(stringResource(Res.string.settings_anonymize_title)) },
            text = { Text(stringResource(Res.string.settings_anonymize_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDataDialog = false
                    coroutineScope.launch {
                        try {
                            syncRepository.anonymizeUserData()
                            snackbarHostState.showSnackbar(anonymizeSuccessMsg)
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(anonymizeErrorMsg)
                        }
                    }
                }) {
                    Text(stringResource(Res.string.proceed_button_text), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) {
                    Text(stringResource(Res.string.cancel_text), color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface),
        content = content
    )
}

@Composable
fun SettingsClickableRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
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

fun formatFloat(value: Float): String {
    val rounded = ((value * 100).toInt() / 100.0)
    val str = rounded.toString()
    return if (str.contains(".") && str.substringAfter(".").length == 1) {
        "${str}0"
    } else {
        str
    }
}

@Composable
fun SettingsSliderRow(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
            Text(
                text = formatFloat(value),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
