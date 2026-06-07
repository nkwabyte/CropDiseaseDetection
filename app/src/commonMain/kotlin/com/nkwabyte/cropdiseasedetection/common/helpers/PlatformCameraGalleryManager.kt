package com.nkwabyte.cropdiseasedetection.common.helpers

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformCameraGalleryManager(
    onImageBytesReceived: (ByteArray?) -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (takePicture: () -> Unit, selectPicture: () -> Unit) -> Unit
)
