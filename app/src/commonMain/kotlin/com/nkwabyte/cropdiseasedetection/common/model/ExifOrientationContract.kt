package com.nkwabyte.cropdiseasedetection.common.model

/**
 * The cross-platform definition of "upright".
 *
 * EXIF orientation is the one place where Android and iOS could silently
 * disagree about what image a model is looking at, and for a long time they did:
 * iOS corrected orientation inside `UIImage.fixOrientation()` while Android fed
 * the raw sensor-order buffer straight to the models (recorded as C025). This
 * table is the shared specification both implementations are held to —
 * `ImageOrientationInstrumentedTest` asserts Android's transform against it, and
 * it is the same standard interpretation UIKit applies when drawing a UIImage.
 *
 * Corners are named on the STORED image and listed as they appear in the UPRIGHT
 * result, reading top-left, top-right, bottom-left, bottom-right.
 */
enum class ImageCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/**
 * @param exifValue        the tag value, 1-8.
 * @param description      the standard name for this orientation.
 * @param swapsDimensions  true when the upright image's width/height are the
 *                         stored image's height/width.
 * @param uprightCorners   which stored corner ends up at each upright corner,
 *                         in the order top-left, top-right, bottom-left,
 *                         bottom-right.
 */
data class ExifOrientationCase(
    val exifValue: Int,
    val description: String,
    val swapsDimensions: Boolean,
    val uprightCorners: List<ImageCorner>,
)

object ExifOrientationContract {

    private val TL = ImageCorner.TOP_LEFT
    private val TR = ImageCorner.TOP_RIGHT
    private val BL = ImageCorner.BOTTOM_LEFT
    private val BR = ImageCorner.BOTTOM_RIGHT

    /** All eight defined orientations, in tag order. */
    val cases: List<ExifOrientationCase> = listOf(
        ExifOrientationCase(1, "normal", false, listOf(TL, TR, BL, BR)),
        ExifOrientationCase(2, "mirror horizontal", false, listOf(TR, TL, BR, BL)),
        ExifOrientationCase(3, "rotate 180", false, listOf(BR, BL, TR, TL)),
        ExifOrientationCase(4, "mirror vertical", false, listOf(BL, BR, TL, TR)),
        ExifOrientationCase(5, "transpose", true, listOf(TL, BL, TR, BR)),
        ExifOrientationCase(6, "rotate 90 clockwise", true, listOf(BL, TL, BR, TR)),
        ExifOrientationCase(7, "transverse", true, listOf(BR, TR, BL, TL)),
        ExifOrientationCase(8, "rotate 270 clockwise", true, listOf(TR, BR, TL, BL)),
    )

    fun case(exifValue: Int): ExifOrientationCase =
        cases.firstOrNull { it.exifValue == exifValue }
            ?: error("No EXIF orientation case for value $exifValue")
}
