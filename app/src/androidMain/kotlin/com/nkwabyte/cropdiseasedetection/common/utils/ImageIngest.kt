package com.nkwabyte.cropdiseasedetection.common.utils

import android.graphics.Bitmap
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Normalizes image bytes at the point they enter the app, from the camera or
 * the gallery.
 *
 * This is where Android's orientation bug actually bit. `PlatformCameraGalleryManager`
 * decoded the picked URI with an EXIF-blind `BitmapFactory` and re-compressed
 * the result to JPEG before handing the bytes on — which both failed to rotate
 * the pixels AND discarded the EXIF tag that said they needed rotating. By the
 * time the bytes reached the detector there was nothing left to recover the
 * orientation from, so correcting orientation inside the detector alone would
 * have fixed nothing for a real photo taken in portrait.
 *
 * Applying the correction once, here, also rules out double rotation: the JPEG
 * this produces carries no orientation tag, so the detector's own
 * [ImageOrientation.decodeUpright] reads it as already upright and leaves it be.
 */
object ImageIngest {

    private const val TAG = "ImageIngest"

    /**
     * Returns [imageBytes] re-encoded as an upright JPEG, or null if the bytes
     * cannot be decoded.
     *
     * @param quality JPEG quality; 100 by default, because these pixels are
     *        about to be fed to two models and compression artifacts are not
     *        worth the bytes saved.
     */
    fun uprightJpegBytes(imageBytes: ByteArray, quality: Int = 100): ByteArray? {
        val decoded = ImageOrientation.decodeUpright(imageBytes) ?: run {
            Log.w(TAG, "Could not decode the selected image")
            return null
        }
        val bitmap = decoded.bitmap
        return try {
            ByteArrayOutputStream().use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                    Log.w(TAG, "JPEG re-compression failed")
                    return null
                }
                out.toByteArray()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to re-encode the selected image", e)
            null
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }
}
