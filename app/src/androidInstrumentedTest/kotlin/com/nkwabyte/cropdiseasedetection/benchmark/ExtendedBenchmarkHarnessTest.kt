package com.nkwabyte.cropdiseasedetection.benchmark

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_EXPORT_SCHEMA_VERSION
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkPath
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.java.KoinJavaComponent

/**
 * Runs the publication-protocol extended benchmark once, end to end, as harness
 * validation — this checks that the instrumentation produces a well-formed,
 * correctly-ordered export, NOT that the numbers in it are citable. It runs on
 * an emulator; the numbers are emulator numbers.
 *
 * It calls the same `runExtendedBenchmark()` the Settings screen's developer
 * button calls, with the protocol's default run counts.
 */
@RunWith(AndroidJUnit4::class)
class ExtendedBenchmarkHarnessTest {

    @Test
    fun extendedBenchmarkProducesACorrectlyOrderedExport() {
        val detector: ObjectDetector = KoinJavaComponent.get(ObjectDetector::class.java)
        val export = runBlocking { detector.runExtendedBenchmark() }

        assertEquals(BENCHMARK_EXPORT_SCHEMA_VERSION, export.schemaVersion)
        assertTrue("no routing path was measured", export.endToEndByPath.isNotEmpty())

        for ((path, e2e) in export.endToEndByPath) {
            Log.i(
                "BenchmarkHarness",
                "path=$path n=${e2e.stats.n} mean=${e2e.stats.meanMs} p95=${e2e.stats.p95Ms} " +
                    "detectorExecuted=${e2e.detectorExecutedCount} skipped=${e2e.detectorSkippedCount} " +
                    "order=${e2e.observedStageOrder.joinToString("->")}",
            )
            // The corrected order, asserted on the real export.
            val classifyAt = e2e.observedStageOrder.indexOfFirst { it.startsWith("classifierPreprocess") }
            val routeAt = e2e.observedStageOrder.indexOf("routingDecision")
            val detectAt = e2e.observedStageOrder.indexOfFirst { it.startsWith("detectorPreprocess") }
            assertTrue("$path: classifier must precede routing", classifyAt in 0 until routeAt)
            assertTrue("$path: routing must precede detection", routeAt < detectAt)

            assertEquals(e2e.measuredRuns, e2e.detectorExecutedCount + e2e.detectorSkippedCount)
            if (path == BenchmarkPath.REJECTED || path == BenchmarkPath.MISMATCH) {
                assertEquals("$path executed the detector", 0, e2e.detectorExecutedCount)
            }
            for (v in listOf(e2e.stats.meanMs, e2e.stats.p50Ms, e2e.stats.p95Ms, e2e.stats.p99Ms, e2e.stats.stdDevMs)) {
                assertFalse("$path produced a non-finite latency", v.isNaN() || v.isInfinite())
            }

            val breakdown = e2e.stageBreakdown
            assertTrue("$path has no per-stage breakdown", breakdown != null)
            assertEquals(e2e.measuredRuns, breakdown!!.routing.n)
            // Routing is measured, not assumed: it is the stage that only exists
            // in the composed pipeline, so a constant zero here would mean the
            // benchmark is not actually running the routing decision.
            assertTrue("$path routing was never measured", breakdown.routing.maxMs > 0.0)
            assertTrue(
                "$path orientation correction is a constant zero",
                breakdown.orientationCorrection.maxMs > 0.0,
            )
            if (e2e.detectorExecutedCount == 0) {
                assertEquals(
                    "$path reported detector inference time without running the detector",
                    0.0, breakdown.detectorInference.maxMs, 0.0,
                )
            }
        }

        // Routing is a measured stage, not a constant.
        val classifierStage = export.classifierStage
        assertTrue("classifierStage was omitted", classifierStage != null)
        assertTrue(
            "orientationCorrectionMs is still a constant zero on Android",
            classifierStage!!.orientationCorrection.maxMs > 0.0,
        )

        assertTrue(export.modelArtifacts.all { it.sha256.length == 64 })
        assertTrue(export.imageManifest.all { it.sha256.length == 64 })
        assertTrue(export.deviceEnvironment.buildIdentifier.isNotBlank())
        assertTrue(
            "an emulator run must be flagged as such in the notes",
            !export.deviceEnvironment.isEmulator ||
                export.notes.any { it.contains("EMULATOR", ignoreCase = true) },
        )
    }
}
