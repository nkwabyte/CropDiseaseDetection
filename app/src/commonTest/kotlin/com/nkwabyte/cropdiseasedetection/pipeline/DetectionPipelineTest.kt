package com.nkwabyte.cropdiseasedetection.pipeline

import com.nkwabyte.cropdiseasedetection.common.model.DetectionData
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.common.pipeline.BOX_COORDINATE_SPACE
import com.nkwabyte.cropdiseasedetection.common.pipeline.DetectionPipeline
import com.nkwabyte.cropdiseasedetection.common.pipeline.DetectionStateReducer
import com.nkwabyte.cropdiseasedetection.common.pipeline.PipelineOutcome
import com.nkwabyte.cropdiseasedetection.common.pipeline.RoutingDecision
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetectionPipelineTest {

    @Test
    fun rejectedImageNeverInvokesTheDetector() = runTest {
        val engine = FakeInferenceEngine(
            classification = rejected(),
            detections = (0..22).map { detectionOf(it) },
        )
        val result = DetectionPipeline(engine).run(IMAGE, "Corn")

        assertEquals(0, engine.detectCalls, "the detector ran on an out-of-distribution image")
        assertEquals(0, engine.detectorLoadCalls, "the detector model was even loaded")
        assertFalse(result.detectorExecuted)
        assertEquals(PipelineOutcome.REJECTED_OUT_OF_DISTRIBUTION, result.outcome)
        assertTrue(result.rawResults.isEmpty())
        assertTrue(result.routedResults.isEmpty())
    }

    @Test
    fun classifierFailureNeverInvokesTheDetector() = runTest {
        val nullResult = FakeInferenceEngine(classification = null)
        val a = DetectionPipeline(nullResult).run(IMAGE, "Corn")
        assertEquals(0, nullResult.detectCalls)
        assertEquals(PipelineOutcome.CLASSIFIER_ERROR, a.outcome)

        val throwing = FakeInferenceEngine(
            classification = accepted("Corn"),
            classifierThrows = IllegalStateException("classifier module not loaded"),
        )
        val b = DetectionPipeline(throwing).run(IMAGE, "Corn")
        assertEquals(0, throwing.detectCalls)
        assertEquals(PipelineOutcome.CLASSIFIER_ERROR, b.outcome)
        assertEquals("classifier module not loaded", b.errorMessage)
    }

    @Test
    fun classifierFailureIsNotReportedAsAHealthyCrop() = runTest {
        val engine = FakeInferenceEngine(classification = null)
        val result = DetectionPipeline(engine).run(IMAGE, "")
        val state = DetectionStateReducer.from(result, "")

        assertTrue(state.isInferenceError)
        assertFalse(state.isDetectionSuccessful)
        assertFalse(state.isClassifierRejected, "an inference failure was labelled a rejection")
        assertTrue(state.results.isEmpty())
        // "Healthy" is a detector class; a failed classifier must never produce one.
        assertTrue(state.results.none { it.className?.contains("Healthy") == true })
    }

    @Test
    fun acceptedCornRoutesOnlyCornDetections() = runTest {
        val engine = FakeInferenceEngine(
            classification = accepted("Corn"),
            detections = (0..22).map { detectionOf(it) },
        )
        val result = DetectionPipeline(engine).run(IMAGE, "Corn")

        assertEquals(1, engine.detectCalls)
        assertTrue(result.detectorExecuted)
        assertEquals(23, result.rawResults.size, "raw results must be preserved un-filtered")
        assertEquals(listOf(0, 1, 2, 3, 4), result.routedResults.map { it.classIndex })
        assertEquals(SupportedCrop.CORN, result.routedCrop)
        assertEquals(PipelineOutcome.DETECTED, result.outcome)
    }

    @Test
    fun acceptedPepperRoutesOnlyPepperDetections() = runTest {
        val engine = FakeInferenceEngine(
            classification = accepted("Pepper"),
            detections = (0..22).map { detectionOf(it) },
        )
        val result = DetectionPipeline(engine).run(IMAGE, "")
        assertEquals((5..14).toList(), result.routedResults.map { it.classIndex })
    }

    @Test
    fun acceptedTomatoRoutesOnlyTomatoDetections() = runTest {
        val engine = FakeInferenceEngine(
            classification = accepted("Tomato"),
            detections = (0..22).map { detectionOf(it) },
        )
        val result = DetectionPipeline(engine).run(IMAGE, "")
        assertEquals((15..22).toList(), result.routedResults.map { it.classIndex })
    }

    @Test
    fun blankSelectionUsesTheClassifierCrop() = runTest {
        for (blank in listOf("", "   ")) {
            val engine = FakeInferenceEngine(
                classification = accepted("Tomato"),
                detections = listOf(detectionOf(2), detectionOf(18)),
            )
            val result = DetectionPipeline(engine).run(IMAGE, blank)
            assertEquals(1, engine.detectCalls)
            assertEquals(listOf(18), result.routedResults.map { it.classIndex })
        }
    }

    @Test
    fun conflictingSelectionSkipsTheDetectorAndKeepsThePrediction() = runTest {
        val engine = FakeInferenceEngine(
            classification = accepted("Tomato", 0.93f),
            detections = (0..22).map { detectionOf(it) },
        )
        val result = DetectionPipeline(engine).run(IMAGE, "Corn")

        assertEquals(0, engine.detectCalls, "the detector ran despite a crop mismatch")
        assertEquals(0, engine.detectorLoadCalls)
        assertEquals(PipelineOutcome.CROP_MISMATCH, result.outcome)
        val decision = result.decision
        assertIs<RoutingDecision.SelectionMismatch>(decision)
        assertEquals(SupportedCrop.TOMATO, decision.predicted)
        assertEquals(SupportedCrop.CORN, decision.selected)
        assertEquals(0.93f, result.classification?.confidence)
        assertTrue(result.routedResults.isEmpty())
    }

    @Test
    fun acceptedCropWithNoDetectionsIsDistinctFromRejection() = runTest {
        val clean = FakeInferenceEngine(classification = accepted("Corn"), detections = emptyList())
        val cleanResult = DetectionPipeline(clean).run(IMAGE, "Corn")
        assertEquals(PipelineOutcome.NO_DETECTIONS, cleanResult.outcome)
        assertEquals(1, clean.detectCalls, "an accepted crop must still be scanned")
        assertTrue(cleanResult.detectorExecuted)

        val ood = FakeInferenceEngine(classification = rejected())
        val oodResult = DetectionPipeline(ood).run(IMAGE, "Corn")
        assertEquals(PipelineOutcome.REJECTED_OUT_OF_DISTRIBUTION, oodResult.outcome)

        // Both end with zero results, so the outcomes are what tell them apart.
        assertTrue(cleanResult.routedResults.isEmpty() && oodResult.routedResults.isEmpty())
        val cleanState = DetectionStateReducer.from(cleanResult, "Corn")
        val oodState = DetectionStateReducer.from(oodResult, "Corn")
        assertFalse(cleanState.isClassifierRejected)
        assertTrue(oodState.isClassifierRejected)
    }

    @Test
    fun allCrossCropDetectionsRemovedWhenDetectorSeesEverything() = runTest {
        val engine = FakeInferenceEngine(
            classification = accepted("Corn"),
            detections = listOf(detectionOf(0), detectionOf(9), detectionOf(20)),
        )
        val result = DetectionPipeline(engine).run(IMAGE, "Corn")
        assertEquals(listOf(0), result.routedResults.map { it.classIndex })
        assertEquals(3, result.rawResults.size)
    }

    @Test
    fun eachStageRunsAtMostOncePerRequest() = runTest {
        val engine = FakeInferenceEngine(
            classification = accepted("Corn"),
            detections = listOf(detectionOf(1)),
        )
        DetectionPipeline(engine).run(IMAGE, "Corn")
        assertEquals(1, engine.classifyCalls)
        assertEquals(1, engine.detectCalls)
        assertEquals(1, engine.classifierLoadCalls)
        assertEquals(1, engine.detectorLoadCalls)
    }

    @Test
    fun detectorFailureIsRecordedAsExecutedAndAsAnError() = runTest {
        val engine = FakeInferenceEngine(
            classification = accepted("Corn"),
            detectorThrows = RuntimeException("forward() failed"),
        )
        val result = DetectionPipeline(engine).run(IMAGE, "Corn")
        assertEquals(PipelineOutcome.DETECTOR_ERROR, result.outcome)
        // It was invoked and failed — counting it as "skipped" would make a
        // broken detector look like a correctly gated one in the benchmark.
        assertTrue(result.detectorExecuted)
        assertEquals("forward() failed", result.errorMessage)
    }

    @Test
    fun instrumentationIsOptionalAndMeasuresRouting() = runTest {
        val engine = FakeInferenceEngine(
            classification = accepted("Corn"),
            detections = listOf(detectionOf(0)),
        )
        val pipeline = DetectionPipeline(engine)

        assertNull(pipeline.run(IMAGE, "Corn").timing, "production path must not pay for timing")

        val timed = pipeline.run(IMAGE, "Corn", instrumented = true).timing
        assertNotNull(timed)
        assertTrue(timed.detectorExecuted)
        assertTrue(timed.routingMs >= 0.0 && timed.routingMs.isFinite())
        assertTrue(timed.totalMs >= 0.0 && timed.totalMs.isFinite())

        val skipped = pipeline.run(IMAGE, "Tomato", instrumented = true).timing
        assertNotNull(skipped)
        assertFalse(skipped.detectorExecuted)
        assertEquals(0.0, skipped.detectorInferenceMs)
    }

    @Test
    fun resultCarriesAuthoritativeOrientedDimensionsAndBoxSpace() = runTest {
        val engine = FakeInferenceEngine(
            classification = accepted("Corn"),
            detections = listOf(detectionOf(0)),
        )
        val result = DetectionPipeline(engine).run(IMAGE, "Corn")
        // The fake reports an EXIF-6 image: 480x640 upright, transposed from 640x480.
        assertEquals(480, result.orientedImage?.widthPx)
        assertEquals(640, result.orientedImage?.heightPx)
        assertEquals(6, result.orientedImage?.exifOrientation)
        assertTrue(result.orientedImage?.orientationApplied == true)
        assertEquals(BOX_COORDINATE_SPACE, result.boxCoordinateSpace)
    }
}

class DetectionStateReducerTest {

    private val stale = DetectionData(
        results = listOf(detectionOf(3)),
        isDetected = true,
        isDetectionSuccessful = true,
        isCropMissMatch = true,
        isClassifierRejected = true,
        isInferenceError = true,
        classificationLabel = "Tomato",
        classifierConfidence = 0.77f,
        predictedCropId = "Tomato",
        detectorExecuted = true,
        outcome = "DETECTED",
        errorMessage = "old failure",
    )

    @Test
    fun startingClearsEveryStaleResultField() {
        val next = DetectionStateReducer.starting(stale)
        assertTrue(next.isDetecting)
        assertTrue(next.results.isEmpty())
        assertFalse(next.isDetected)
        assertFalse(next.isDetectionSuccessful)
        assertFalse(next.isCropMissMatch)
        assertFalse(next.isClassifierRejected)
        assertFalse(next.isInferenceError)
        assertFalse(next.detectorExecuted)
        assertNull(next.classificationLabel)
        assertEquals(0f, next.classifierConfidence)
        assertNull(next.predictedCropId)
        assertNull(next.outcome)
        assertNull(next.errorMessage)
    }

    @Test
    fun everyTerminalOutcomeStopsTheSpinner() = runTest {
        val engines = listOf(
            FakeInferenceEngine(accepted("Corn"), listOf(detectionOf(0))),
            FakeInferenceEngine(accepted("Corn"), emptyList()),
            FakeInferenceEngine(rejected()),
            FakeInferenceEngine(accepted("Tomato"), listOf(detectionOf(16))),
            FakeInferenceEngine(null),
            FakeInferenceEngine(accepted("Corn"), detectorThrows = RuntimeException("boom")),
        )
        val selections = listOf("Corn", "Corn", "Corn", "Corn", "Corn", "Corn")
        val seen = mutableSetOf<PipelineOutcome>()
        for ((engine, selection) in engines.zip(selections)) {
            val result = DetectionPipeline(engine).run(IMAGE, selection)
            val state = DetectionStateReducer.from(result, selection)
            seen += result.outcome
            assertFalse(state.isDetecting, "${result.outcome} left isDetecting true")
            assertTrue(state.isDetected, "${result.outcome} did not mark the request finished")
        }
        assertEquals(PipelineOutcome.entries.toSet(), seen, "not every outcome was exercised")
    }

    @Test
    fun flagsAccuratelyDescribeWhichStagesRan() = runTest {
        val detected = DetectionStateReducer.from(
            DetectionPipeline(FakeInferenceEngine(accepted("Corn"), listOf(detectionOf(0))))
                .run(IMAGE, "Corn"),
            "Corn",
        )
        assertTrue(detected.isDetectionSuccessful && detected.detectorExecuted)
        assertFalse(detected.isCropMissMatch || detected.isClassifierRejected)
        assertEquals("Corn", detected.predictedCropId)

        val mismatch = DetectionStateReducer.from(
            DetectionPipeline(FakeInferenceEngine(accepted("Tomato"), listOf(detectionOf(16))))
                .run(IMAGE, "Corn"),
            "Corn",
        )
        assertTrue(mismatch.isCropMissMatch)
        assertFalse(mismatch.isClassifierRejected)
        assertFalse(mismatch.detectorExecuted)
        assertEquals("Tomato", mismatch.predictedCropId, "the prediction must survive for the UI")
        assertEquals("Corn", mismatch.selectedCropId)
    }
}
