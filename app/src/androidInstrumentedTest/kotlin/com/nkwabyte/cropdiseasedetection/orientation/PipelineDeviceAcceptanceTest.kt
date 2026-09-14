package com.nkwabyte.cropdiseasedetection.orientation

import android.util.Log
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.OrientedImageSize
import com.nkwabyte.cropdiseasedetection.common.model.StageLatencyMs
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.common.pipeline.DetectionPipeline
import com.nkwabyte.cropdiseasedetection.common.pipeline.DetectionPipelineResult
import com.nkwabyte.cropdiseasedetection.common.pipeline.InferenceEngine
import com.nkwabyte.cropdiseasedetection.common.pipeline.ObjectDetectorEngine
import com.nkwabyte.cropdiseasedetection.common.pipeline.PipelineOutcome
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.java.KoinJavaComponent
import java.io.File
import java.io.FileOutputStream

/**
 * End-to-end acceptance of the corrected two-stage pipeline against the REAL
 * ExecuTorch models, on a real Android runtime.
 *
 * The images are real photographs, not synthetic patterns: three from the ML
 * project's held-out detector test split (one per crop, checksums recorded in
 * the worklog) and one cassava leaf from its out-of-distribution sample set —
 * the near-miss case the two-stage design exists to reject.
 *
 * The detector invocation counter is the point. A test that only checked the
 * returned detections could not tell "the detector ran and its output was
 * filtered away" from "the detector never ran", and those are exactly the two
 * architectures at issue in C024.
 */
@RunWith(AndroidJUnit4::class)
class PipelineDeviceAcceptanceTest {

    private companion object {
        const val TAG = "PipelineAcceptance"
    }

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Wraps the real engine and counts what actually got invoked. */
    private class CountingEngine(private val delegate: InferenceEngine) : InferenceEngine {
        var classifyCalls = 0
        var detectCalls = 0
        var detectorLoadCalls = 0

        override suspend fun ensureClassifierLoaded() = delegate.ensureClassifierLoaded()
        override suspend fun ensureDetectorLoaded() {
            detectorLoadCalls++
            delegate.ensureDetectorLoaded()
        }

        override fun classify(imageBytes: ByteArray): Pair<ClassificationResult?, StageLatencyMs> {
            classifyCalls++
            return delegate.classify(imageBytes)
        }

        override fun detect(imageBytes: ByteArray): Pair<List<DetectionResult>, StageLatencyMs> {
            detectCalls++
            return delegate.detect(imageBytes)
        }

        override fun orientedImageSize(imageBytes: ByteArray): OrientedImageSize? =
            delegate.orientedImageSize(imageBytes)
    }

    private fun detector(): ObjectDetector =
        KoinJavaComponent.get(ObjectDetector::class.java)

    private fun asset(name: String): ByteArray =
        InstrumentationRegistry.getInstrumentation().context.assets
            .open("acceptance/$name").use { it.readBytes() }

    private data class Run(
        val result: DetectionPipelineResult,
        val classifyCalls: Int,
        val detectCalls: Int,
        val detectorLoadCalls: Int,
    )

    private fun runPipeline(imageBytes: ByteArray, selectedCropId: String?, label: String): Run {
        val counting = CountingEngine(ObjectDetectorEngine(detector()))
        val result = runBlocking { DetectionPipeline(counting).run(imageBytes, selectedCropId) }
        // Structured evidence, greppable in logcat alongside the assertions.
        Log.i(
            TAG,
            "case=$label selected=${selectedCropId ?: "<none>"} outcome=${result.outcome} " +
                "classifier=${result.classification?.label}@${result.classification?.confidence} " +
                "detectorExecuted=${result.detectorExecuted} classifyCalls=${counting.classifyCalls} " +
                "detectCalls=${counting.detectCalls} raw=${result.rawResults.size} " +
                "routed=${result.routedResults.size} " +
                "routedClassIds=${result.routedResults.map { it.classIndex }} " +
                "oriented=${result.orientedImage?.widthPx}x${result.orientedImage?.heightPx}" +
                "@exif${result.orientedImage?.exifOrientation}",
        )
        return Run(result, counting.classifyCalls, counting.detectCalls, counting.detectorLoadCalls)
    }

    /** corn.jpg re-encoded byte-for-byte with an EXIF orientation tag added. */
    private fun withExifOrientation(source: ByteArray, orientation: Int): ByteArray {
        val file = File(context.cacheDir, "acceptance_exif_$orientation.jpg")
        FileOutputStream(file).use { it.write(source) }
        val exif = ExifInterface(file)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
        exif.saveAttributes()
        return file.readBytes()
    }

    private fun assertRoutedWithinCrop(run: Run, crop: SupportedCrop, label: String) {
        assertTrue(
            "$label: routed ids ${run.result.routedResults.map { it.classIndex }} escaped " +
                "${crop.canonicalLabel}'s range ${crop.classIdRange}",
            run.result.routedResults.all { it.classIndex in crop.classIds },
        )
    }

    @Test
    fun supportedCornRunsTheDetectorAndRoutesOnlyCornClasses() {
        val run = runPipeline(asset("corn.jpg"), null, "supported_corn")
        assertNotNull(run.result.classification)
        assertEquals("Corn", run.result.classification?.label)
        assertTrue("the detector did not run on an accepted crop", run.result.detectorExecuted)
        assertEquals(1, run.classifyCalls)
        assertEquals(1, run.detectCalls)
        assertRoutedWithinCrop(run, SupportedCrop.CORN, "corn")
        assertTrue(
            run.result.outcome == PipelineOutcome.DETECTED ||
                run.result.outcome == PipelineOutcome.NO_DETECTIONS,
        )
    }

    @Test
    fun supportedPepperRunsTheDetectorAndRoutesOnlyPepperClasses() {
        val run = runPipeline(asset("pepper.jpg"), null, "supported_pepper")
        assertEquals("Pepper", run.result.classification?.label)
        assertTrue(run.result.detectorExecuted)
        assertRoutedWithinCrop(run, SupportedCrop.PEPPER, "pepper")
    }

    @Test
    fun supportedTomatoRunsTheDetectorAndRoutesOnlyTomatoClasses() {
        val run = runPipeline(asset("tomato.jpg"), null, "supported_tomato")
        assertEquals("Tomato", run.result.classification?.label)
        assertTrue(run.result.detectorExecuted)
        assertRoutedWithinCrop(run, SupportedCrop.TOMATO, "tomato")
    }

    @Test
    fun outOfDistributionLeafNeverReachesTheDetector() {
        // A cassava leaf: visually a leaf, not one of the three crops. Under the
        // old detect-first architecture this paid for a full detector pass and
        // could surface a confident tomato-disease box.
        val run = runPipeline(asset("ood_cassava.jpg"), null, "ood_cassava")
        assertEquals(PipelineOutcome.REJECTED_OUT_OF_DISTRIBUTION, run.result.outcome)
        assertFalse(run.result.detectorExecuted)
        assertEquals("the detector was invoked on a rejected image", 0, run.detectCalls)
        assertEquals("the detector model was even loaded for a rejected image", 0, run.detectorLoadCalls)
        assertTrue(run.result.rawResults.isEmpty())
        assertTrue(run.result.routedResults.isEmpty())
        assertEquals(1, run.classifyCalls)
    }

    @Test
    fun selectedCropMismatchNeverReachesTheDetector() {
        val run = runPipeline(asset("corn.jpg"), SupportedCrop.TOMATO.canonicalLabel, "mismatch_corn_as_tomato")
        assertEquals(PipelineOutcome.CROP_MISMATCH, run.result.outcome)
        assertFalse(run.result.detectorExecuted)
        assertEquals("the detector was invoked despite a crop mismatch", 0, run.detectCalls)
        assertEquals(0, run.detectorLoadCalls)
        // The prediction survives for the UI to explain the mismatch with.
        assertEquals("Corn", run.result.classification?.label)
    }

    @Test
    fun rotatedImageClassifiesTheSameAsTheUprightOneAndReportsTransposedDimensions() {
        val upright = asset("corn.jpg")
        val rotated = withExifOrientation(upright, ExifInterface.ORIENTATION_ROTATE_90)

        val uprightRun = runPipeline(upright, null, "orientation_exif1")
        val rotatedRun = runPipeline(rotated, null, "orientation_exif6")

        // Same pixels, only the orientation tag differs — so after correction the
        // classifier must reach the same verdict. Before this fix Android fed the
        // sideways buffer straight to the model.
        assertEquals(
            "a 90-degree EXIF tag changed the classification",
            uprightRun.result.classification?.label,
            rotatedRun.result.classification?.label,
        )
        assertTrue(rotatedRun.result.detectorExecuted)
        assertRoutedWithinCrop(rotatedRun, SupportedCrop.CORN, "rotated corn")

        val uprightSize = uprightRun.result.orientedImage!!
        val rotatedSize = rotatedRun.result.orientedImage!!
        assertEquals(1, uprightSize.exifOrientation)
        assertEquals(6, rotatedSize.exifOrientation)
        assertTrue(rotatedSize.orientationApplied)
        // The authoritative dimensions transpose, which is the whole point of
        // reporting them rather than echoing the caller's numbers back.
        assertEquals(uprightSize.heightPx, rotatedSize.widthPx)
        assertEquals(uprightSize.widthPx, rotatedSize.heightPx)
    }
}
