package com.nkwabyte.cropdiseasedetection.common.helpers

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.nkwabyte.cropdiseasedetection.common.utils.ImageIngest
import com.nkwabyte.cropdiseasedetection.common.utils.readBytesFromUri
import java.io.File

@Composable
actual fun PlatformCameraGalleryManager(
    onImageBytesReceived: (ByteArray?) -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (takePicture: () -> Unit, selectPicture: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    fun processUri(uri: Uri?) {
        if (uri == null) {
            onImageBytesReceived(null)
            return
        }
        // Orientation is applied HERE, once, to the original bytes — before the
        // re-encode that would otherwise discard the EXIF tag. Everything
        // downstream (preview, classifier, detector, overlay) therefore sees the
        // same upright pixels. See ImageIngest's doc comment.
        val original = readBytesFromUri(context, uri)
        val upright = original?.let { ImageIngest.uprightJpegBytes(it) }
        onImageBytesReceived(upright)
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
