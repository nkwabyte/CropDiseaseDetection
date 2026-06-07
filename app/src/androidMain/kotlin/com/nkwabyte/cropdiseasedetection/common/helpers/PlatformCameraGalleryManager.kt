package com.nkwabyte.cropdiseasedetection.common.helpers

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.nkwabyte.cropdiseasedetection.common.utils.loadBitmapFromUri
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
actual fun PlatformCameraGalleryManager(
    onImageBytesReceived: (ByteArray?) -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (takePicture: () -> Unit, selectPicture: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    fun processUri(uri: Uri?) {
        if (uri == null) {
            onImageBytesReceived(null)
            return
        }
        val bitmap = loadBitmapFromUri(context, uri)
        if (bitmap != null) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            onImageBytesReceived(outputStream.toByteArray())
        } else {
            onImageBytesReceived(null)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            processUri(photoUri)
        } else {
            onImageBytesReceived(null)
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
            cameraLauncher.launch(uri)
        } else {
            onPermissionDenied()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            processUri(uri)
        } else {
            onImageBytesReceived(null)
        }
    }

    content(
        {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val photoFile = File.createTempFile("captured_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    photoFile
                )
                photoUri = uri
                cameraLauncher.launch(uri)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        {
            galleryLauncher.launch("image/*")
        }
    )
}
