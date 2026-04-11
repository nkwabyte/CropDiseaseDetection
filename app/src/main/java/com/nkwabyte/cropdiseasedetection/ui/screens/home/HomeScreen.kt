package com.nkwabyte.cropdiseasedetection.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.nkwabyte.cropdiseasedetection.R
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AppViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.DetectionViewModel
import com.nkwabyte.cropdiseasedetection.common.utils.loadBitmapFromUri
import com.nkwabyte.cropdiseasedetection.ui.components.GradientSnackBar
import com.nkwabyte.cropdiseasedetection.ui.components.LoadingDialog
import com.nkwabyte.cropdiseasedetection.ui.theme.CropDiseaseDetectionTheme
import org.koin.compose.koinInject
import java.io.File

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
    var isCameraMode by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val appState by appViewModel.appState.collectAsState()
    val detectionData by detectionViewModel.detectionState.collectAsState()

    LaunchedEffect(detectionData) {
        if (detectionData.results.isNotEmpty() || detectionData.isDetectionSuccessful) {
            navigateToResult(detectionData.results)
        }
        if (detectionData.isDetected && detectionData.results.isEmpty()) {
            snackBarHostState.showSnackbar(
                message = context.getString(R.string.no_detection_message),
                actionLabel = context.getString(R.string.ok_text)
            )
        }
        if (detectionData.results.isEmpty() && (detectionData.isDetected && !detectionData.isCropMissMatch)) {
            snackBarHostState.showSnackbar(
                message = context.getString(R.string.error_detecting_message),
                actionLabel = context.getString(R.string.ok_text)
            )
        }
    }

    // Resolve string resources outside the derivedStateOf block
    val submitText = stringResource(R.string.home_button_submit)
    val takePictureText = stringResource(R.string.home_button_take_picture)
    val selectPictureText = stringResource(R.string.home_button_select_picture)

    val mainButtonText by remember {
        derivedStateOf {
            when {
                selectedImageUri != null -> submitText
                isCameraMode -> takePictureText
                else -> selectPictureText
            }
        }
    }

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = photoUri
            appViewModel.setSelectedImageUri(photoUri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File.createTempFile("captured_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            photoUri = uri
            appViewModel.setSelectedImageUri(uri)
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.home_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            appViewModel.setSelectedImageUri(uri)
        }
    }

    val handleMainAction: () -> Unit = {
        when (mainButtonText) {
            takePictureText -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val photoFile = File.createTempFile("captured_", ".jpg", context.cacheDir)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        photoFile
                    )
                    photoUri = uri
                    appViewModel.setSelectedImageUri(uri)
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
            selectPictureText -> galleryLauncher.launch("image/*")
            submitText -> {
                selectedImageUri?.let { uri ->
                    val bitmap = loadBitmapFromUri(context, uri)
                    if (bitmap != null) {
                        appViewModel.setSelectedImageUri(uri)
                        appState.selectedCrop?.let {
                            detectionViewModel.detectWithPyTorch(bitmap, it)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.unable_to_load_image),
                            Toast.LENGTH_SHORT
                        ).show()
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
                        text = stringResource(R.string.app_name),
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
                    selectedImageUri = null
                    appViewModel.setSelectedImageUri(null)
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
                        .padding(vertical = 16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri),
                                contentDescription = stringResource(
                                    R.string.home_selected_image_description
                                ),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = {
                                    selectedImageUri = null
                                    appViewModel.setSelectedImageUri(null)
                                    detectionViewModel.reset()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.home_clear_image_description),
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
                            Text(
                                text = if (isCameraMode)
                                    stringResource(R.string.home_tap_take_picture)
                                else
                                    stringResource(R.string.home_tap_select_picture),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = handleMainAction,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
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
        }
    )
}

@Preview(showBackground = true)
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