package com.nkwabyte.cropdiseasedetection.common.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.nkwabyte.cropdiseasedetection.common.model.OrientedImageSize
import java.io.ByteArrayInputStream

/**
 * Android's EXIF orientation normalization — the counterpart of iOS's
 * `UIImage.fixOrientation()` in ExecuTorchBridge.swift.
 *
 * Before this existed, Android decoded camera bytes with a bare
 * `BitmapFactory.decodeByteArray()` and fed the raw sensor-order pixel buffer
 * to models trained on upright images, while iOS corrected orientation first.
 * A portrait phone photo carries `ORIENTATION_ROTATE_90` in EXIF and decodes
 * sideways, so the two platforms were classifying and detecting on visibly
 * different images from the same bytes.
 *
 * Every entry point here is safe on images with no EXIF block at all (PNG),
 * with a truncated or corrupt one, and on images that are already upright — all
 * of those take the identity path and return the source bitmap untouched.
 */
object ImageOrientation {

    private const val TAG = "ImageOrientation"

    /**
     * A decoded, upright bitmap plus what it took to get there.
     *
     * @param bitmap            the upright image. Owned by the caller.
     * @param exifOrientation   the raw tag value 1-8; 1 when absent or unreadable.
     * @param orientationApplied true when pixels were actually transformed.
     * @param orientationCorrectionMs EXIF parsing + matrix transformation, the
     *        figure reported as `StageLatencyMs.orientationCorrectionMs`. It
     *        excludes the JPEG decode itself, which is timed separately.
     */
    data class Decoded(
        val bitmap: Bitmap,
        val exifOrientation: Int,
        val orientationApplied: Boolean,
        val orientationCorrectionMs: Double,
    )

    /**
     * Decodes image bytes and returns them visually upright.
     *
     * The caller owns the returned bitmap. Any intermediate bitmap created
     * while transforming is recycled here; the returned one never is, and a
     * recycled bitmap is never returned.
     */
    fun decodeUpright(imageBytes: ByteArray): Decoded? {
        val decoded = try {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.w(TAG, "Bitmap decode failed", e)
            null
        } ?: return null

        val exifStart = System.nanoTime()
        val orientation = readExifOrientation(imageBytes)
        val upright = applyOrientation(decoded, orientation)
        val exifEnd = System.nanoTime()

        // applyOrientation returns the source itself for the no-op orientations,
        // so recycling unconditionally here would free the bitmap we return.
        if (upright !== decoded) decoded.recycle()

        return Decoded(
            bitmap = upright,
            exifOrientation = orientation,
            orientationApplied = upright !== decoded,
            orientationCorrectionMs = (exifEnd - exifStart) / 1_000_000.0,
        )
    }

    /**
     * The image's dimensions once upright, without decoding the pixels.
     *
     * Uses a bounds-only decode plus the EXIF tag, so it is cheap enough to call
     * on every request. Width and height are swapped for the four orientations
     * that transpose the image (5-8).
     */
    fun orientedSize(imageBytes: ByteArray): OrientedImageSize? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
        } catch (e: Exception) {
            Log.w(TAG, "Bounds decode failed", e)
            return null
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        val orientation = readExifOrientation(imageBytes)
        val transposes = orientation in TRANSPOSING_ORIENTATIONS
        return OrientedImageSize(
            widthPx = if (transposes) options.outHeight else options.outWidth,
            heightPx = if (transposes) options.outWidth else options.outHeight,
            exifOrientation = orientation,
            orientationApplied = orientation != ExifInterface.ORIENTATION_NORMAL &&
                orientation != ExifInterface.ORIENTATION_UNDEFINED,
        )
    }

    /**
     * Reads the EXIF orientation tag from the ORIGINAL bytes, without modifying
     * them. Returns [ExifInterface.ORIENTATION_NORMAL] for formats with no EXIF
     * block, for a missing tag, for a corrupt block (ExifInterface throws or
     * returns garbage), and for any value outside the defined range 1-8 — in
     * every one of those cases the safe reading is "already upright".
     */
    fun readExifOrientation(imageBytes: ByteArray): Int {
        val raw = try {
            ByteArrayInputStream(imageBytes).use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        } catch (e: Exception) {
            // Corrupt or truncated metadata: proceed with the undistorted image
            // rather than failing the whole inference request.
            Log.w(TAG, "Could not read EXIF orientation; treating image as upright", e)
            ExifInterface.ORIENTATION_NORMAL
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM reading EXIF orientation; treating image as upright", e)
            ExifInterface.ORIENTATION_NORMAL
        }
        return if (raw in ExifInterface.ORIENTATION_NORMAL..ExifInterface.ORIENTATION_ROTATE_270) {
            raw
        } else {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * The transformation for an EXIF orientation, or null when none is needed.
     *
     * These eight cases are the standard EXIF interpretation, and match what
     * `UIImage.fixOrientation()` produces on iOS for the same bytes, which is
     * what keeps the two platforms' models looking at the same image.
     *
     * | EXIF | meaning          | transform                     |
     * |------|------------------|-------------------------------|
     * | 1    | normal           | none                          |
     * | 2    | mirror horizontal| scale(-1, 1)                  |
     * | 3    | rotate 180       | rotate(180)                   |
     * | 4    | mirror vertical  | rotate(180) + scale(-1, 1)    |
     * | 5    | transpose        | rotate(90) + scale(-1, 1)     |
     * | 6    | rotate 90 CW     | rotate(90)                    |
     * | 7    | transverse       | rotate(-90) + scale(-1, 1)    |
     * | 8    | rotate 270 CW    | rotate(-90)                   |
     */
    fun matrixFor(orientation: Int): Matrix? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL,
            ExifInterface.ORIENTATION_UNDEFINED -> return null

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return null
        }
        return matrix
    }

    /**
     * Applies [orientation] to [source].
     *
     * Returns `source` itself — not a copy — when the orientation is a no-op, so
     * callers must compare by identity before recycling anything. Returns
     * `source` unchanged if the transform throws or runs out of memory: a
     * possibly-rotated image beats no image at all.
     */
    fun applyOrientation(source: Bitmap, orientation: Int): Bitmap {
        if (source.isRecycled) return source
        val matrix = matrixFor(orientation) ?: return source
        return try {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                // createBitmap can hand back the source when the matrix turns out
                // to be a no-op for these dimensions; that is not an error.
                ?: source
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "Out of memory rotating bitmap for EXIF orientation $orientation", e)
            source
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply EXIF orientation $orientation", e)
            source
        }
    }

    /** The orientations that swap width and height. */
    val TRANSPOSING_ORIENTATIONS = setOf(
        ExifInterface.ORIENTATION_TRANSPOSE,
        ExifInterface.ORIENTATION_ROTATE_90,
        ExifInterface.ORIENTATION_TRANSVERSE,
        ExifInterface.ORIENTATION_ROTATE_270,
    )
}
