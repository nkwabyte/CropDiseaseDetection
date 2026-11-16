package com.nkwabyte.cropdiseasedetection.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.nkwabyte.cropdiseasedetection.common.helpers.PlatformCameraGalleryManager
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.generated.resources.Res
import com.nkwabyte.cropdiseasedetection.generated.resources.error_detecting_message
import com.nkwabyte.cropdiseasedetection.generated.resources.home_analyzing
import com.nkwabyte.cropdiseasedetection.generated.resources.home_camera
import com.nkwabyte.cropdiseasedetection.generated.resources.home_clear_image_description
import com.nkwabyte.cropdiseasedetection.generated.resources.home_crop_required_message
import com.nkwabyte.cropdiseasedetection.generated.resources.home_gallery
import com.nkwabyte.cropdiseasedetection.generated.resources.home_header_subtitle
import com.nkwabyte.cropdiseasedetection.generated.resources.home_header_title
import com.nkwabyte.cropdiseasedetection.generated.resources.home_no_photo_subtitle
import com.nkwabyte.cropdiseasedetection.generated.resources.home_no_photo_title
import com.nkwabyte.cropdiseasedetection.generated.resources.home_permission_denied
import com.nkwabyte.cropdiseasedetection.generated.resources.home_run_diagnostics
import com.nkwabyte.cropdiseasedetection.generated.resources.home_select_target_crop
import com.nkwabyte.cropdiseasedetection.generated.resources.home_selected_image_description
import com.nkwabyte.cropdiseasedetection.generated.resources.ok_text
import com.nkwabyte.cropdiseasedetection.generated.resources.tab_diagnostics
import com.nkwabyte.cropdiseasedetection.generated.resources.unable_to_load_image
import com.nkwabyte.cropdiseasedetection.ui.components.GroupedCard
import com.nkwabyte.cropdiseasedetection.ui.components.GradientSnackBar
import com.nkwabyte.cropdiseasedetection.ui.components.InfoColor
import com.nkwabyte.cropdiseasedetection.ui.components.PrimaryActionButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** The crops offered on the Diagnostics tab, in the same order and with the same
 *  glyphs as the iOS app. */
private val cropChoices = listOf(
    SupportedCrop.CORN to "🌽",
    SupportedCrop.PEPPER to "🫑",
    SupportedCrop.TOMATO to "🍅",
)

/**
 * The Diagnostics tab — the counterpart of iOS `HomeView`. Crop choice, photo and the
 * run button share one scrolling screen, so there is no separate crop-selection step.
 *
 * The selected photo lives in [AppViewModel.appState] (not local state), so returning
 * from the result page shows the same photo, and "Done" there clears it here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    appViewModel: AppViewModel,
    detectionViewModel: DetectionViewModel,
    navigateToResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appState by appViewModel.appState.collectAsState()
    val detectionData by detectionViewModel.detectionState.collectAsState()
    val selectedImage = appState.selectedImageByteArray

    // Set when the user starts a run and cleared once its outcome is handled, so coming
    // back to this tab never re-opens a result that was already shown.
    var awaitingResult by rememberSaveable { mutableStateOf(false) }
    var showCropRequiredDialog by remember { mutableStateOf(false) }

    val okText = stringResource(Res.string.ok_text)
    val errorDetectingMessage = stringResource(Res.string.error_detecting_message)
    val permissionDeniedMsg = stringResource(Res.string.home_permission_denied)
    val unableToLoadMsg = stringResource(Res.string.unable_to_load_image)

    LaunchedEffect(detectionData.isDetected, awaitingResult) {
        if (!awaitingResult || !detectionData.isDetected) return@LaunchedEffect
        awaitingResult = false
        // A model failure is never shown as a result page — it would read as
        // "no disease found" for a scan that never ran.
        if (detectionData.isInferenceError) {
            snackBarHostState.showSnackbar(message = errorDetectingMessage, actionLabel = okText)
        } else {
            navigateToResult()
        }
    }

    PlatformCameraGalleryManager(
        onImageBytesReceived = { bytes ->
            if (bytes == null) {
                scope.launch { snackBarHostState.showSnackbar(message = unableToLoadMsg, actionLabel = okText) }
            } else {
                detectionViewModel.reset()
                appViewModel.setSelectedImageByteArray(bytes)
            }
        },
        onPermissionDenied = {
            scope.launch { snackBarHostState.showSnackbar(message = permissionDeniedMsg, actionLabel = okText) }
        },
    ) { takePicture, selectPicture ->
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AppBar(title = stringResource(Res.string.tab_diagnostics), scrollBehavior = scrollBehavior)
            },
            snackbarHost = {
                SnackbarHost(hostState = snackBarHostState, modifier = Modifier.padding(8.dp)) { data ->
                    GradientSnackBar(
                        message = data.visuals.message,
                        actionLabel = data.visuals.actionLabel,
                        onAction = { data.dismiss() },
                    )
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
                HeaderBanner()

                CropSelector(
                    selectedCropId = appState.selectedCropId,
                    onSelect = { crop, displayName ->
                        // The display name is for the UI; the canonical id is what the
                        // pipeline routes on, so routing never depends on the language.
                        appViewModel.setSelectedCrop(displayName, crop.canonicalLabel)
                    },
                )

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (selectedImage != null) {
                        SelectedPhoto(
                            imageBytes = selectedImage,
                            onClear = {
                                appViewModel.setSelectedImageByteArray(null)
                                detectionViewModel.reset()
                            },
                        )
                    } else {
                        EmptyPhotoPlaceholder(onClick = selectPicture)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SourceButton(
                            text = stringResource(Res.string.home_gallery),
                            icon = Icons.Default.PhotoLibrary,
                            tint = InfoColor,
                            onClick = selectPicture,
                            modifier = Modifier.weight(1f),
                        )
                        SourceButton(
                            text = stringResource(Res.string.home_camera),
                            icon = Icons.Default.CameraAlt,
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = takePicture,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                AnimatedVisibility(visible = selectedImage != null) {
                    PrimaryActionButton(
                        text = stringResource(
                            if (detectionData.isDetecting) Res.string.home_analyzing else Res.string.home_run_diagnostics
                        ),
                        icon = Icons.Default.MonitorHeart,
                        loading = detectionData.isDetecting,
                        onClick = {
                            val cropId = appState.selectedCropId
                            val bytes = appState.selectedImageByteArray
                            when {
                                cropId.isNullOrEmpty() -> showCropRequiredDialog = true
                                bytes == null -> scope.launch {
                                    snackBarHostState.showSnackbar(message = unableToLoadMsg, actionLabel = okText)
                                }
                                else -> {
                                    awaitingResult = true
                                    detectionViewModel.detect(bytes, cropId)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (showCropRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showCropRequiredDialog = false },
            title = { Text(stringResource(Res.string.home_select_target_crop)) },
            text = { Text(stringResource(Res.string.home_crop_required_message)) },
            confirmButton = {
                TextButton(onClick = { showCropRequiredDialog = false }) { Text(okText) }
            },
        )
    }
}

@Composable
private fun HeaderBanner() {
    GroupedCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(Res.string.home_header_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(Res.string.home_header_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.Eco,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun CropSelector(
    selectedCropId: String?,
    onSelect: (SupportedCrop, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(Res.string.home_select_target_crop),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            cropChoices.forEach { (crop, emoji) ->
                val displayName = stringResource(crop.displayNameRes())
                val isSelected = selectedCropId.equals(crop.canonicalLabel, ignoreCase = true)
                val shape = RoundedCornerShape(12.dp)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onSelect(crop, displayName) }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(emoji, fontSize = 28.sp)
                    Text(
                        displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedPhoto(imageBytes: ByteArray, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageBytes),
            contentDescription = stringResource(Res.string.home_selected_image_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onClear,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(Res.string.home_clear_image_description),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun EmptyPhotoPlaceholder(onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 18f))),
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
            }
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Icon(
            Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            stringResource(Res.string.home_no_photo_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(Res.string.home_no_photo_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SourceButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(text, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}
