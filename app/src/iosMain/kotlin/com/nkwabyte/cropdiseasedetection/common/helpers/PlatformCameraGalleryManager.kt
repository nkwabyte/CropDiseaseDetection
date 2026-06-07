package com.nkwabyte.cropdiseasedetection.common.helpers

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformCameraGalleryManager(
    onImageBytesReceived: (ByteArray?) -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (takePicture: () -> Unit, selectPicture: () -> Unit) -> Unit
) {
    content(
        {
            // TODO: Implement iOS camera
            onImageBytesReceived(null)
        },
        {
            // TODO: Implement iOS gallery
            onImageBytesReceived(null)
        }
    )
}
