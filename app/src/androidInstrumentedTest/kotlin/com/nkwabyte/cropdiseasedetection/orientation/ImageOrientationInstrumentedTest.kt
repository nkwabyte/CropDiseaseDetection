package com.nkwabyte.cropdiseasedetection.orientation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nkwabyte.cropdiseasedetection.common.model.ExifOrientationContract
import com.nkwabyte.cropdiseasedetection.common.model.ImageCorner
import com.nkwabyte.cropdiseasedetection.common.utils.ImageIngest
import com.nkwabyte.cropdiseasedetection.common.utils.ImageOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

/**
 * Verifies Android's EXIF orientation normalization against the shared
 * [ExifOrientationContract] — the same definition of "upright" iOS's
 * `UIImage.fixOrientation()` implements.
 *
 * The fixture is deliberately asymmetric in BOTH senses: its width differs from
 * its height (so a transposing orientation is visible in the dimensions) and all
 * four quadrants are different colours (so a rotation is distinguishable from a
 * mirror, which a symmetric fixture cannot do).
 *
 * These are instrumentation tests because the thing under test is real
 * `android.graphics.Bitmap` and real `ExifInterface` behaviour on a real
 * Android runtime; a JVM stub would prove nothing about either.
 */
@RunWith(AndroidJUnit4::class)
class ImageOrientationInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val storedWidth = 64
    private val storedHeight = 32

    private val cornerColors = mapOf(
        ImageCorner.TOP_LEFT to Color.rgb(220, 20, 20),      // red
        ImageCorner.TOP_RIGHT to Color.rgb(20, 200, 20),     // green
        ImageCorner.BOTTOM_LEFT to Color.rgb(20, 20, 220),   // blue
        ImageCorner.BOTTOM_RIGHT to Color.rgb(230, 210, 20), // yellow
    )

    /** A 64x32 image with four distinctly coloured quadrants. */
    private fun asymmetricBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(storedWidth, storedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val halfW = storedWidth / 2f
        val halfH = storedHeight / 2f
        val paint = Paint()
        paint.color = cornerColors.getValue(ImageCorner.TOP_LEFT)
        canvas.drawRect(0f, 0f, halfW, halfH, paint)
        paint.color = cornerColors.getValue(ImageCorner.TOP_RIGHT)
        canvas.drawRect(halfW, 0f, storedWidth.toFloat(), halfH, paint)
        paint.color = cornerColors.getValue(ImageCorner.BOTTOM_LEFT)
        canvas.drawRect(0f, halfH, halfW, storedHeight.toFloat(), paint)
        paint.color = cornerColors.getValue(ImageCorner.BOTTOM_RIGHT)
        canvas.drawRect(halfW, halfH, storedWidth.toFloat(), storedHeight.toFloat(), paint)
        return bitmap
    }

    /** The fixture encoded as a JPEG carrying the given EXIF orientation tag. */
    private fun jpegWithOrientation(orientation: Int): ByteArray {
        val file = File(context.cacheDir, "exif_fixture_$orientation.jpg")
        FileOutputStream(file).use { out ->
            asymmetricBitmap().compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        // Written into the file's metadata only — the pixel data is untouched,
        // exactly as a camera would produce it.
        val exif = ExifInterface(file)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
        exif.saveAttributes()
        return file.readBytes()
    }

    private fun pngWithoutExif(): ByteArray = ByteArrayOutputStream().use { out ->
        asymmetricBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
        out.toByteArray()
    }

    /** Samples well inside a quadrant so JPEG ringing at the edges cannot flip a result. */
    private fun sampleCorner(bitmap: Bitmap, corner: ImageCorner): Int {
        val x = when (corner) {
            ImageCorner.TOP_LEFT, ImageCorner.BOTTOM_LEFT -> bitmap.width / 4
            else -> bitmap.width * 3 / 4
        }
        val y = when (corner) {
            ImageCorner.TOP_LEFT, ImageCorner.TOP_RIGHT -> bitmap.height / 4
            else -> bitmap.height * 3 / 4
        }
        return bitmap.getPixel(x, y)
    }

    private fun assertColorNear(expected: Int, actual: Int, message: String) {
        val tolerance = 48 // JPEG at quality 100 still shifts flat colours slightly
        val dr = abs(Color.red(expected) - Color.red(actual))
        val dg = abs(Color.green(expected) - Color.green(actual))
        val db = abs(Color.blue(expected) - Color.blue(actual))
        assertTrue(
            "$message: expected ~#${Integer.toHexString(expected)} " +
                "but was #${Integer.toHexString(actual)}",
            dr <= tolerance && dg <= tolerance && db <= tolerance,
        )
    }

    @Test
    fun allEightOrientationsProduceTheContractUprightImage() {
        for (case in ExifOrientationContract.cases) {
            val bytes = jpegWithOrientation(case.exifValue)
            val decoded = ImageOrientation.decodeUpright(bytes)
            assertNotNull("EXIF ${case.exifValue} (${case.description}) failed to decode", decoded)
            val bitmap = decoded!!.bitmap

            val expectedWidth = if (case.swapsDimensions) storedHeight else storedWidth
            val expectedHeight = if (case.swapsDimensions) storedWidth else storedHeight
            assertEquals(
                "EXIF ${case.exifValue} (${case.description}) upright width",
                expectedWidth, bitmap.width,
            )
            assertEquals(
                "EXIF ${case.exifValue} (${case.description}) upright height",
                expectedHeight, bitmap.height,
            )

            // Corner placement: this is what separates a rotation from a mirror.
            val uprightPositions = listOf(
                ImageCorner.TOP_LEFT, ImageCorner.TOP_RIGHT,
                ImageCorner.BOTTOM_LEFT, ImageCorner.BOTTOM_RIGHT,
            )
            uprightPositions.forEachIndexed { index, position ->
                val sourceCorner = case.uprightCorners[index]
                assertColorNear(
                    cornerColors.getValue(sourceCorner),
                    sampleCorner(bitmap, position),
                    "EXIF ${case.exifValue} (${case.description}): $position should hold the " +
                        "stored image's $sourceCorner",
                )
            }

            assertEquals(case.exifValue, decoded.exifOrientation)
            assertEquals(case.exifValue != 1, decoded.orientationApplied)
        }
    }

    @Test
    fun orientedSizeAgreesWithTheDecodedBitmapForEveryOrientation() {
        // The detector reports boxes relative to the upright image's dimensions,
        // and the pipeline reports those dimensions from orientedSize(). If these
        // two ever disagree, every box is placed against the wrong canvas.
        for (case in ExifOrientationContract.cases) {
            val bytes = jpegWithOrientation(case.exifValue)
            val decoded = ImageOrientation.decodeUpright(bytes)!!
            val reported = ImageOrientation.orientedSize(bytes)
            assertNotNull("orientedSize returned null for EXIF ${case.exifValue}", reported)
            assertEquals(
                "width disagreement for EXIF ${case.exifValue}",
                decoded.bitmap.width, reported!!.widthPx,
            )
            assertEquals(
                "height disagreement for EXIF ${case.exifValue}",
                decoded.bitmap.height, reported.heightPx,
            )
            assertEquals(case.exifValue, reported.exifOrientation)
        }
    }

    @Test
    fun classifierAndDetectorReceiveTheSameUprightPixels() {
        // Both stages call decodeUpright(); this pins down that it is
        // deterministic, so the two models cannot be looking at different images.
        for (case in ExifOrientationContract.cases) {
            val bytes = jpegWithOrientation(case.exifValue)
            val first = ImageOrientation.decodeUpright(bytes)!!.bitmap
            val second = ImageOrientation.decodeUpright(bytes)!!.bitmap
            assertEquals(first.width, second.width)
            assertEquals(first.height, second.height)
            for (corner in ImageCorner.entries) {
                assertEquals(
                    "EXIF ${case.exifValue}: two decodes disagreed at $corner",
                    sampleCorner(first, corner), sampleCorner(second, corner),
                )
            }
        }
    }

    @Test
    fun imageWithoutExifMetadataIsLeftAlone() {
        val bytes = pngWithoutExif()
        val decoded = ImageOrientation.decodeUpright(bytes)
        assertNotNull(decoded)
        assertEquals(ExifInterface.ORIENTATION_NORMAL, decoded!!.exifOrientation)
        assertFalse("a PNG with no orientation tag was transformed", decoded.orientationApplied)
        assertEquals(storedWidth, decoded.bitmap.width)
        assertEquals(storedHeight, decoded.bitmap.height)
        for (corner in ImageCorner.entries) {
            assertColorNear(
                cornerColors.getValue(corner),
                sampleCorner(decoded.bitmap, corner),
                "PNG without EXIF moved $corner",
            )
        }
    }

    @Test
    fun alreadyUprightJpegIsNotCopiedOrRotated() {
        val decoded = ImageOrientation.decodeUpright(jpegWithOrientation(1))!!
        assertFalse(decoded.orientationApplied)
        assertEquals(storedWidth, decoded.bitmap.width)
        assertEquals(storedHeight, decoded.bitmap.height)
    }

    @Test
    fun malformedExifIsHandledAsUprightRatherThanThrowing() {
        val valid = jpegWithOrientation(6)

        // Corrupt the metadata region while leaving the JPEG's SOI marker intact,
        // which is what a partially-written or damaged file actually looks like.
        val corrupted = valid.copyOf()
        for (i in 2 until minOf(64, corrupted.size)) corrupted[i] = 0xFF.toByte()
        assertEquals(
            ExifInterface.ORIENTATION_NORMAL,
            ImageOrientation.readExifOrientation(corrupted),
        )

        // Truncated to metadata only: no decodable pixels, so decodeUpright must
        // return null rather than crash the inference request.
        assertNull(ImageOrientation.decodeUpright(valid.copyOf(24)))

        // Empty and obviously-not-an-image input.
        assertEquals(ExifInterface.ORIENTATION_NORMAL, ImageOrientation.readExifOrientation(ByteArray(0)))
        assertNull(ImageOrientation.decodeUpright(ByteArray(0)))
        assertNull(ImageOrientation.orientedSize("not an image at all".toByteArray()))
    }

    @Test
    fun undefinedAndOutOfRangeOrientationValuesAreTreatedAsUpright() {
        for (value in listOf(0, 9, 42, -1)) {
            assertNull(
                "orientation $value should map to no transform",
                ImageOrientation.matrixFor(value),
            )
        }
        assertNull(ImageOrientation.matrixFor(ExifInterface.ORIENTATION_UNDEFINED))
        assertNull(ImageOrientation.matrixFor(ExifInterface.ORIENTATION_NORMAL))
    }

    @Test
    fun applyOrientationDoesNotCopyForNoOpAndNeverReturnsARecycledBitmap() {
        val source = asymmetricBitmap()
        // A no-op must return the very same object, so callers that recycle by
        // identity comparison cannot free a bitmap that is still in use.
        assertTrue(source === ImageOrientation.applyOrientation(source, ExifInterface.ORIENTATION_NORMAL))

        val rotated = ImageOrientation.applyOrientation(source, ExifInterface.ORIENTATION_ROTATE_90)
        assertTrue("a real rotation must produce a new bitmap", rotated !== source)
        assertFalse(rotated.isRecycled)
        assertEquals(storedHeight, rotated.width)
        assertEquals(storedWidth, rotated.height)

        source.recycle()
        // A recycled source must not blow up, and must not be transformed.
        val afterRecycle = ImageOrientation.applyOrientation(source, ExifInterface.ORIENTATION_ROTATE_90)
        assertTrue(afterRecycle === source)
        rotated.recycle()
    }

    @Test
    fun decodeUprightReturnsALiveBitmapAndTimesTheCorrection() {
        for (case in ExifOrientationContract.cases) {
            val decoded = ImageOrientation.decodeUpright(jpegWithOrientation(case.exifValue))!!
            assertFalse(
                "EXIF ${case.exifValue} returned a recycled bitmap",
                decoded.bitmap.isRecycled,
            )
            // orientationCorrectionMs is a real measurement, not the constant 0.0
            // Android used to report; it must at least be finite and non-negative.
            assertTrue(decoded.orientationCorrectionMs >= 0.0)
            assertFalse(decoded.orientationCorrectionMs.isNaN())
            assertFalse(decoded.orientationCorrectionMs.isInfinite())
        }
    }

    @Test
    fun ingestProducesUprightBytesForEveryOrientation() {
        // The entry point is where this has to happen: the camera/gallery handler
        // re-encodes to JPEG, which drops the EXIF tag, so an image not corrected
        // here arrives at the models sideways with no evidence that it is sideways.
        for (case in ExifOrientationContract.cases) {
            val ingested = ImageIngest.uprightJpegBytes(jpegWithOrientation(case.exifValue))
            assertNotNull("ingest returned null for EXIF ${case.exifValue}", ingested)

            val decoded = ImageOrientation.decodeUpright(ingested!!)!!
            val expectedWidth = if (case.swapsDimensions) storedHeight else storedWidth
            val expectedHeight = if (case.swapsDimensions) storedWidth else storedHeight
            assertEquals(
                "EXIF ${case.exifValue} (${case.description}) ingested width",
                expectedWidth, decoded.bitmap.width,
            )
            assertEquals(
                "EXIF ${case.exifValue} (${case.description}) ingested height",
                expectedHeight, decoded.bitmap.height,
            )

            val uprightPositions = listOf(
                ImageCorner.TOP_LEFT, ImageCorner.TOP_RIGHT,
                ImageCorner.BOTTOM_LEFT, ImageCorner.BOTTOM_RIGHT,
            )
            uprightPositions.forEachIndexed { index, position ->
                assertColorNear(
                    cornerColors.getValue(case.uprightCorners[index]),
                    sampleCorner(decoded.bitmap, position),
                    "ingested EXIF ${case.exifValue}: $position",
                )
            }
        }
    }

    @Test
    fun ingestedBytesCarryNoOrientationTagSoTheyAreNotRotatedTwice() {
        for (case in ExifOrientationContract.cases) {
            val ingested = ImageIngest.uprightJpegBytes(jpegWithOrientation(case.exifValue))!!
            // No tag left to act on: the detector's own decode is a no-op, which is
            // what rules out a second rotation being applied downstream.
            assertEquals(
                "ingested EXIF ${case.exifValue} still carries an orientation tag",
                ExifInterface.ORIENTATION_NORMAL,
                ImageOrientation.readExifOrientation(ingested),
            )
            assertFalse(ImageOrientation.decodeUpright(ingested)!!.orientationApplied)
        }
    }

    @Test
    fun ingestReturnsNullForUndecodableBytesRatherThanThrowing() {
        assertNull(ImageIngest.uprightJpegBytes(ByteArray(0)))
        assertNull(ImageIngest.uprightJpegBytes("definitely not an image".toByteArray()))
    }

    @Test
    fun transposingOrientationSetMatchesTheContract() {
        val expected = ExifOrientationContract.cases.filter { it.swapsDimensions }.map { it.exifValue }.toSet()
        assertEquals(expected, ImageOrientation.TRANSPOSING_ORIENTATIONS)
    }
}
