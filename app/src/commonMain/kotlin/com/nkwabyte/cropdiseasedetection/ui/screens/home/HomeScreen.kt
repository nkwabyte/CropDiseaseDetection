package com.nkwabyte.cropdiseasedetection.ui.screens.home

import com.nkwabyte.cropdiseasedetection.generated.resources.*

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.ui.components.GradientSnackBar
import com.nkwabyte.cropdiseasedetection.ui.components.LoadingDialog
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme
import org.koin.compose.koinInject
import com.nkwabyte.cropdiseasedetection.generated.resources.Res
import com.nkwabyte.cropdiseasedetection.generated.resources.home_button_submit
import com.nkwabyte.cropdiseasedetection.generated.resources.home_button_take_picture
import com.nkwabyte.cropdiseasedetection.generated.resources.home_button_select_picture
import com.nkwabyte.cropdiseasedetection.generated.resources.no_detection_message
import com.nkwabyte.cropdiseasedetection.generated.resources.ok_text
import com.nkwabyte.cropdiseasedetection.generated.resources.error_detecting_message
import com.nkwabyte.cropdiseasedetection.generated.resources.classifier_rejection_message
import com.nkwabyte.cropdiseasedetection.generated.resources.home_permission_denied
import com.nkwabyte.cropdiseasedetection.generated.resources.home_selected_image_description
import com.nkwabyte.cropdiseasedetection.generated.resources.home_clear_image_description
import com.nkwabyte.cropdiseasedetection.generated.resources.home_tap_take_picture
import com.nkwabyte.cropdiseasedetection.generated.resources.home_tap_select_picture
import com.nkwabyte.cropdiseasedetection.generated.resources.unable_to_load_image
import com.nkwabyte.cropdiseasedetection.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import com.nkwabyte.cropdiseasedetection.common.helpers.PlatformCameraGalleryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onDrawerButtonClick: () -> Unit = {},
    appViewModel: AppViewModel,
    detectionViewModel: DetectionViewModel,
    navigateToResult: (List<DetectionResult>) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isCameraMode by remember { mutableStateOf(false) }
    var selectedImageData by remember { mutableStateOf<ByteArray?>(null) }

    val appState by appViewModel.appState.collectAsState()
    val detectionData by detectionViewModel.detectionState.collectAsState()

    val noDetectionMessage = stringResource(Res.string.no_detection_message)
    val okText = stringResource(Res.string.ok_text)
    val errorDetectingMessage = stringResource(Res.string.error_detecting_message)
    val classifierRejectionMessage = stringResource(Res.string.classifier_rejection_message)

    LaunchedEffect(detectionData) {
        if (detectionData.isClassifierRejected) {
            snackBarHostState.showSnackbar(
                message = classifierRejectionMessage,
                actionLabel = okText
            )
            return@LaunchedEffect
        }

        if (detectionData.results.isNotEmpty() || detectionData.isDetectionSuccessful) {
            navigateToResult(detectionData.results)
        }
        if (detectionData.isDetected && detectionData.results.isEmpty()) {
            snackBarHostState.showSnackbar(
                message = noDetectionMessage,
                actionLabel = okText
            )
        }
        if (detectionData.results.isEmpty() && (detectionData.isDetected && !detectionData.isCropMissMatch)) {
            snackBarHostState.showSnackbar(
                message = errorDetectingMessage,
                actionLabel = okText
            )
        }
    }

    val submitText = stringResource(Res.string.home_button_submit)
    val takePictureText = stringResource(Res.string.home_button_take_picture)
    val selectPictureText = stringResource(Res.string.home_button_select_picture)
    val permissionDeniedMsg = stringResource(Res.string.home_permission_denied)
    val unableToLoadMsg = stringResource(Res.string.unable_to_load_image)

    val mainButtonText by remember {
        derivedStateOf {
            when {
                selectedImageData != null -> submitText
                isCameraMode -> takePictureText
                else -> selectPictureText
            }
        }
    }

    var showCropRequiredDialog by remember { mutableStateOf(false) }

    PlatformCameraGalleryManager(
        onImageBytesReceived = { bytes ->
            selectedImageData = bytes
            appViewModel.setSelectedImageByteArray(bytes)
            if (bytes == null && mainButtonText == submitText) {
                scope.launch {
                    snackBarHostState.showSnackbar(
                        message = unableToLoadMsg,
                        actionLabel = okText
                    )
                }
            }
        },
        onPermissionDenied = {
            scope.launch {
                snackBarHostState.showSnackbar(
                    message = permissionDeniedMsg,
                    actionLabel = okText
                )
            }
        }
    ) { takePicture, selectPicture ->

        val handleMainAction: () -> Unit = {
            when (mainButtonText) {
                takePictureText -> takePicture()
                selectPictureText -> selectPicture()
                submitText -> {
                    if (appState.selectedCrop.isNullOrEmpty()) {
                        showCropRequiredDialog = true
                    } else {
                        selectedImageData?.let { bytes ->
                            appViewModel.setSelectedImageByteArray(bytes)
                            val targetCrop = appState.selectedCrop ?: ""
                            detectionViewModel.detect(bytes, targetCrop, 640, 640)
                        } ?: run {
                            scope.launch {
                                snackBarHostState.showSnackbar(
                                    message = unableToLoadMsg,
                                    actionLabel = okText
                                )
                            }
                        }
                    }
                }
            }
        }

        Scaffold(
            modifier = modifier,
            topBar = {
                AppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.app_name),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                    onDrawerButtonClick = onDrawerButtonClick,
                    isHomeScreen = true,
                    isCameraMode = isCameraMode,
                    onCheckedChange = {
                        isCameraMode = it
                        selectedImageData = null
                        appViewModel.setSelectedImageByteArray(null)
                    },
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackBarHostState,
                    modifier = Modifier.padding(8.dp),
                    snackbar = { snackBarData ->
                        GradientSnackBar(
                            message = snackBarData.visuals.message,
                            actionLabel = snackBarData.visuals.actionLabel,
                            onAction = { snackBarData.dismiss() }
                        )
                    }
                )
            },
            content = { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 16.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp
                    ) {
                        androidx.compose.animation.Crossfade(
                            targetState = selectedImageData,
                            animationSpec = androidx.compose.animation.core.tween(300)
                        ) { imageData ->
                            if (imageData != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(imageData),
                                        contentDescription = stringResource(Res.string.home_selected_image_description),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(MaterialTheme.shapes.large),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (detectionData.classificationLabel != null) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(16.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                                                    shape = MaterialTheme.shapes.medium
                                                )
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = detectionData.classificationLabel!!,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            appViewModel.setSelectedImageByteArray(null)
                                            detectionViewModel.reset()
                                            selectedImageData = null
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                shape = MaterialTheme.shapes.small
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(Res.string.home_clear_image_description),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { handleMainAction() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (isCameraMode)
                                                stringResource(Res.string.home_tap_take_picture)
                                            else
                                                stringResource(Res.string.home_tap_select_picture),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = handleMainAction,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 8.dp,
                            pressedElevation = 2.dp,
                            hoveredElevation = 10.dp
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = mainButtonText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                if(detectionData.isDetecting){
                    LoadingDialog()
                }
                if (showCropRequiredDialog) {
                    AlertDialog(
                        onDismissRequest = { showCropRequiredDialog = false },
                        title = {
                            Text(
                                text = "Select Target Crop",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        text = {
                            Text(
                                text = "Please select a target crop type (Corn, Pepper, or Tomato) before running disease analysis.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(onClick = { showCropRequiredDialog = false }) {
                                Text(okText)
                            }
                        }
                    )
                }
            }
        )
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    CropDiseaseDetectionTheme {
        HomeScreen(
            onDrawerButtonClick = {},
            navigateToResult = {},
            detectionViewModel = koinInject<DetectionViewModel>(),
            appViewModel = koinInject<AppViewModel>(),
        )
    }
}