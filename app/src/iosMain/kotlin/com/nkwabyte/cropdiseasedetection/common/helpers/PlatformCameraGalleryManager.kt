package com.nkwabyte.cropdiseasedetection.common.helpers

import androidx.compose.runtime.*
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import platform.AVFoundation.*
import platform.Foundation.NSData
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
private class ImagePickerDelegate(
    private val onImagePicked: (ByteArray?) -> Unit,
    private val onDismiss: () -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        if (image != null) {
            val jpegData = UIImageJPEGRepresentation(image, 0.9)
            if (jpegData != null) {
                val size = jpegData.length.toInt()
                val bytes = jpegData.bytes?.readBytes(size)
                onImagePicked(bytes)
            } else {
                onImagePicked(null)
            }
        } else {
            onImagePicked(null)
        }
        onDismiss()
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        onImagePicked(null)
        onDismiss()
    }
}

@Composable
actual fun PlatformCameraGalleryManager(
    onImageBytesReceived: (ByteArray?) -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (takePicture: () -> Unit, selectPicture: () -> Unit) -> Unit
) {
    val uiViewController = LocalUIViewController.current
    var activeDelegate by remember { mutableStateOf<ImagePickerDelegate?>(null) }

    fun presentImagePicker(sourceType: UIImagePickerControllerSourceType) {
        if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
            onImageBytesReceived(null)
            return
        }

        val delegate = ImagePickerDelegate(
            onImagePicked = { bytes ->
                onImageBytesReceived(bytes)
                activeDelegate = null
            },
            onDismiss = {
                uiViewController.dismissViewControllerAnimated(true, completion = null)
                activeDelegate = null
            }
        )

        activeDelegate = delegate

        val imagePickerController = UIImagePickerController().apply {
            this.sourceType = sourceType
            this.delegate = delegate
        }

        uiViewController.presentViewController(imagePickerController, animated = true, completion = null)
    }

    fun requestCameraPermissionAndLaunch() {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        when (status) {
            AVAuthorizationStatusAuthorized -> {
                presentImagePicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
            }
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (granted) {
                            presentImagePicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
                        } else {
                            onPermissionDenied()
                        }
                    }
                }
            }
            else -> {
                onPermissionDenied()
            }
        }
    }

    content(
        {
            requestCameraPermissionAndLaunch()
        },
        {
            presentImagePicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
        }
    )
}
