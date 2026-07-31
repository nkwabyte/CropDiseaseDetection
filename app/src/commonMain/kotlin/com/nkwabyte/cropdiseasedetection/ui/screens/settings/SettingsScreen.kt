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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.model.RecommendationLanguage
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

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
    val syncRepository = remember { SyncRepository() }

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
                        subtitle = "${appState.recommendationLanguage.flag} ${appState.recommendationLanguage.displayName}",
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
                        title = "Delete My Data",
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
                        subtitle = when (appState.selectedDetectionModel) {
                            "FasterRCNN" -> "Faster R-CNN (Under Training)"
                            "VisionTransformer" -> "Vision Transformer (ViT) (Under Training)"
                            else -> "YOLO26 (Active / Default)"
                        },
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
        val langMap = RecommendationLanguage.entries.associateBy { "${it.flag} ${it.displayName}" }
        val currentDisplay = "${appState.recommendationLanguage.flag} ${appState.recommendationLanguage.displayName}"
        
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
        val optionsMap = mapOf(
            "YOLO26 (Active)" to "YOLO26",
            "Faster R-CNN (Under Training)" to "FasterRCNN",
            "Vision Transformer (ViT) (Under Training)" to "VisionTransformer"
        )
        val currentDisplay = optionsMap.entries.firstOrNull { it.value == appState.selectedDetectionModel }?.key ?: "YOLO26 (Active)"
        
        SelectionDialog(
            title = "Select Disease Detection Model",
            options = optionsMap.keys.toList(),
            selectedOption = currentDisplay,
            onOptionSelected = { selectedKey ->
                optionsMap[selectedKey]?.let { modelKey ->
                    appViewModel.setSelectedDetectionModel(modelKey)
                }
            },
            onDismiss = { showModelDialog = false }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache") },
            text = { Text("Are you sure you want to clear the local cache? This will free up storage space on your device.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    // Simulate clearing cache
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Cache cleared successfully.")
                    }
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = { Text("Delete My Data") },
            text = { Text("Are you sure you want to delete your data? This will unlink your identity from your previous scans. Your scans will become completely anonymous and will only be used to help train our AI models for the benefit of all farmers. This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDataDialog = false
                    coroutineScope.launch {
                        try {
                            syncRepository.anonymizeUserData()
                            snackbarHostState.showSnackbar("Your data has been successfully anonymized.")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Failed to process your request.")
                        }
                    }
                }) {
                    Text("Proceed", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
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
    onDismiss: () -> Unit
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = null, // handled by row click
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
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
