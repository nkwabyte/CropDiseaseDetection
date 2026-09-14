package com.nkwabyte.cropdiseasedetection.benchmark

import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_EXPORT_SCHEMA_VERSION
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkExport
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkImageManifestEntry
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkPath
import com.nkwabyte.cropdiseasedetection.common.model.ColdLoadBenchmark
import com.nkwabyte.cropdiseasedetection.common.model.CpuUtilization
import com.nkwabyte.cropdiseasedetection.common.model.DeviceEnvironment
import com.nkwabyte.cropdiseasedetection.common.model.EndToEndBenchmark
import com.nkwabyte.cropdiseasedetection.common.model.MemorySample
import com.nkwabyte.cropdiseasedetection.common.model.ModelArtifact
import com.nkwabyte.cropdiseasedetection.common.model.StageLatencyMs
import com.nkwabyte.cropdiseasedetection.common.model.computeLatencyStats
import com.nkwabyte.cropdiseasedetection.common.model.computeStageBenchmark
import com.nkwabyte.cropdiseasedetection.common.model.emptyStageLatency
import com.nkwabyte.cropdiseasedetection.common.model.formatBenchmarkExportCsv
import com.nkwabyte.cropdiseasedetection.common.model.formatBenchmarkExportJson
import com.nkwabyte.cropdiseasedetection.common.model.mergeWithDetector
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val CORRECTED_STAGE_ORDER = listOf(
    "ensureClassifierLoaded(idempotent)",
    "decode+exifOrientation",
    "classifierPreprocess+inference+postprocess",
    "routingDecision",
    "ensureDetectorLoaded(idempotent, routed only)",
    "detectorPreprocess+inference+postprocess(routed only)",
    "classIdFiltering(routed only)",
)

private fun stage(detectorExecuted: Boolean, routingMs: Double = 0.04) = StageLatencyMs(
    imageDecodeMs = 3.0,
    orientationCorrectionMs = 0.8,
    preprocessMs = 5.0,
    classifierInferenceMs = 21.0,
    routingMs = routingMs,
    detectorInferenceMs = if (detectorExecuted) 140.0 else 0.0,
    postprocessNmsMs = if (detectorExecuted) 2.0 else 0.0,
    totalMs = if (detectorExecuted) 172.0 else 30.0,
    detectorExecuted = detectorExecuted,
    // A sub-component of detectorInferenceMs, never an addition to it.
    outputTransferMs = if (detectorExecuted) 6.0 else 0.0,
)

private fun endToEnd(path: String, detectorExecuted: Boolean, runs: Int = 100) = EndToEndBenchmark(
    observedStageOrder = CORRECTED_STAGE_ORDER,
    representativeImageId = "detector_1920x1080",
    stats = computeLatencyStats(List(runs) { 150.0 + it * 0.1 }),
    offlineSuccessCount = runs,
    offlineFailureCount = 0,
    failureMessages = emptyList(),
    warmupRuns = 10,
    measuredRuns = runs,
    path = path,
    detectorExecutedCount = if (detectorExecuted) runs else 0,
    detectorSkippedCount = if (detectorExecuted) 0 else runs,
    stageBreakdown = computeStageBenchmark(
        "end_to_end_$path", 10,
        List(runs) { stage(detectorExecuted, routingMs = 0.02 + it * 0.0001) },
    ),
)

private fun sampleExport(): BenchmarkExport = BenchmarkExport(
    generatedAtEpochMs = 1_757_800_000_000,
    deviceEnvironment = DeviceEnvironment(
        platform = "Android",
        manufacturer = "Google",
        model = "sdk_gphone64_arm64",
        osVersion = "15",
        apiLevelOrOsBuild = "35",
        abis = listOf("arm64-v8a"),
        isEmulator = true,
        totalRamBytes = 8L * 1024 * 1024 * 1024,
        batteryLevelPercent = 100,
        isCharging = true,
        thermalStatus = "none",
        thermalStatusNote = "emulator: no real thermal sensor",
        buildIdentifier = "abc123def456",
        appVersionName = "1.0.0",
        appVersionCode = 1,
        installedAppSizeBytes = 210_000_000,
        installedAppSizeNote = "base APK size",
    ),
    modelArtifacts = listOf(
        ModelArtifact("classifier", "crop_classifier_ood.pte", 30_820_096, "a".repeat(64)),
        ModelArtifact("detector (YOLO26n)", "crop_disease_yolo26.pte", 9_768_796, "b".repeat(64)),
    ),
    imageManifest = listOf(
        BenchmarkImageManifestEntry("detector_1920x1080", 1920, 1080, 12345, "c".repeat(64), "synthetic_striped_pattern"),
    ),
    coldLoad = ColdLoadBenchmark(
        classifierColdLoad = computeLatencyStats(listOf(120.0, 118.0, 121.0)),
        detectorColdLoad = computeLatencyStats(listOf(60.0, 62.0, 61.0)),
        repetitions = 5,
        note = "release() before each repetition",
    ),
    classifierStage = computeStageBenchmark("classifier_260x260", 10, List(100) { stage(false) }),
    detectorStageBySize = mapOf(
        "1920x1080" to computeStageBenchmark("detector_1920x1080", 10, List(100) { stage(true) }),
    ),
    endToEnd = endToEnd(BenchmarkPath.ACCEPTED, detectorExecuted = true),
    endToEndByPath = mapOf(
        BenchmarkPath.ACCEPTED to endToEnd(BenchmarkPath.ACCEPTED, detectorExecuted = true),
        BenchmarkPath.REJECTED to endToEnd(BenchmarkPath.REJECTED, detectorExecuted = false),
        BenchmarkPath.MISMATCH to endToEnd(BenchmarkPath.MISMATCH, detectorExecuted = false),
    ),
    cpuUtilization = CpuUtilization(900.0, 1000.0, 0.9, "main", true, null),
    memorySamples = listOf(
        MemorySample("before_measured_loop", 210_000, 90_000, 40_000, 1_757_800_000_000, true, null),
    ),
    notes = listOf("emulator-only, not a Galaxy A10"),
)

class BenchmarkStatsTest {

    @Test
    fun statisticsAreComputedOverTheFullSample() {
        val stats = computeLatencyStats(listOf(1.0, 2.0, 3.0, 4.0))
        assertEquals(4, stats.n)
        assertEquals(2.5, stats.meanMs)
        assertEquals(2.5, stats.medianMs)      // even n: mean of the two middles
        assertEquals(1.0, stats.minMs)
        assertEquals(4.0, stats.maxMs)
        // Sample standard deviation (n-1): sqrt(5/3)
        assertTrue(kotlin.math.abs(stats.stdDevMs - 1.2909944) < 1e-6)
    }

    @Test
    fun percentilesUseNearestRankConsistently() {
        val stats = computeLatencyStats((1..100).map { it.toDouble() })
        assertEquals(51.0, stats.p50Ms)   // round(0.50 * 99) = 50 -> sorted[50]
        assertEquals(90.0, stats.p90Ms)   // round(0.90 * 99) = 89
        assertEquals(95.0, stats.p95Ms)
        assertEquals(99.0, stats.p99Ms)
        assertTrue(stats.p50Ms <= stats.p90Ms && stats.p90Ms <= stats.p95Ms && stats.p95Ms <= stats.p99Ms)
    }

    @Test
    fun singleSampleDoesNotProduceNaNOrInfinity() {
        val stats = computeLatencyStats(listOf(7.0))
        assertEquals(0.0, stats.stdDevMs, "n=1 variance must be 0, not NaN")
        for (v in listOf(stats.meanMs, stats.medianMs, stats.minMs, stats.maxMs, stats.p50Ms, stats.p99Ms)) {
            assertFalse(v.isNaN() || v.isInfinite())
        }
    }

    @Test
    fun runCountsSurviveAggregation() {
        val runs = List(100) { stage(true) }
        val bench = computeStageBenchmark("detector", 10, runs)
        assertEquals(10, bench.warmupRuns)
        assertEquals(100, bench.measuredRuns)
        assertEquals(100, bench.endToEnd.n)
        assertEquals(100, bench.rawRuns.size)
    }

    @Test
    fun outputTransferIsReportedAndStaysInsideDetectorInference() {
        val runs = List(100) { stage(true) }
        val bench = computeStageBenchmark("detector", 10, runs)
        assertEquals(100, bench.outputTransfer.n)
        assertTrue(bench.outputTransfer.meanMs > 0.0, "output transfer was never measured")
        // It is a SUB-COMPONENT: it must never exceed the detector total it is
        // part of, or the two are being double-counted somewhere.
        assertTrue(
            bench.outputTransfer.meanMs <= bench.detectorInference.meanMs,
            "outputTransfer (${bench.outputTransfer.meanMs}) exceeded " +
                "detectorInference (${bench.detectorInference.meanMs})",
        )

        // A skipped detector transfers nothing.
        val skipped = computeStageBenchmark("skipped", 10, List(10) { stage(false) })
        assertEquals(0.0, skipped.outputTransfer.maxMs)
    }

    @Test
    fun mergingClassifierAndDetectorTimingsKeepsBothStages() {
        val merged = stage(false).mergeWithDetector(stage(true))
        assertTrue(merged.detectorExecuted)
        assertEquals(140.0, merged.detectorInferenceMs)
        assertEquals(21.0, merged.classifierInferenceMs, "classifier stage was lost in the merge")
        // Decode/orientation/preprocess are paid by both models, and reported as such.
        assertEquals(6.0, merged.imageDecodeMs)
        assertEquals(1.6, merged.orientationCorrectionMs)
        // The detector's transfer cost survives the merge; the classifier has none.
        assertEquals(6.0, merged.outputTransferMs)
    }

    @Test
    fun emptyStageLatencyCarriesOnlyATotal() {
        val empty = emptyStageLatency(12.5)
        assertEquals(12.5, empty.totalMs)
        assertFalse(empty.detectorExecuted)
        assertEquals(0.0, empty.detectorInferenceMs)
    }
}

class BenchmarkExportSchemaTest {

    @Test
    fun schemaVersionIsFour() {
        assertEquals(4, BENCHMARK_EXPORT_SCHEMA_VERSION)
        assertEquals(4, sampleExport().schemaVersion)
    }

    @Test
    fun everyPathRecordsTheCorrectedStageOrder() {
        val export = sampleExport()
        for ((path, e2e) in export.endToEndByPath) {
            assertEquals(CORRECTED_STAGE_ORDER, e2e.observedStageOrder, "wrong stage order for $path")
            // The old order must be gone, not merely reordered around.
            assertFalse(e2e.observedStageOrder.contains("detect"))
            assertFalse(e2e.observedStageOrder.contains("classify"))
            val classifyAt = e2e.observedStageOrder.indexOfFirst { it.startsWith("classifierPreprocess") }
            val routeAt = e2e.observedStageOrder.indexOf("routingDecision")
            val detectAt = e2e.observedStageOrder.indexOfFirst { it.startsWith("detectorPreprocess") }
            assertTrue(classifyAt < routeAt && routeAt < detectAt, "classify must precede routing, which must precede detection")
        }
    }

    @Test
    fun failClosedPathsRecordZeroDetectorExecutions() {
        val export = sampleExport()
        for (path in listOf(BenchmarkPath.REJECTED, BenchmarkPath.MISMATCH)) {
            val e2e = export.endToEndByPath.getValue(path)
            assertEquals(0, e2e.detectorExecutedCount, "$path executed the detector")
            assertEquals(e2e.measuredRuns, e2e.detectorSkippedCount)
        }
        val accepted = export.endToEndByPath.getValue(BenchmarkPath.ACCEPTED)
        assertEquals(accepted.measuredRuns, accepted.detectorExecutedCount)
        assertEquals(0, accepted.detectorSkippedCount)
    }

    @Test
    fun routingIsAMeasuredStageNotAConstantZero() {
        val runs = List(100) { stage(true, routingMs = 0.02 + it * 0.0001) }
        val bench = computeStageBenchmark("e2e", 10, runs)
        assertTrue(bench.routing.meanMs > 0.0, "routingMs was never measured")
        assertTrue(bench.routing.maxMs >= bench.routing.minMs)
    }

    @Test
    fun jsonRoundTripsWithoutLoss() {
        val export = sampleExport()
        val json = formatBenchmarkExportJson(export)
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString(BenchmarkExport.serializer(), json)

        assertEquals(export.schemaVersion, parsed.schemaVersion)
        assertEquals(export.endToEndByPath.keys, parsed.endToEndByPath.keys)
        assertEquals(
            export.endToEndByPath.getValue(BenchmarkPath.REJECTED).detectorExecutedCount,
            parsed.endToEndByPath.getValue(BenchmarkPath.REJECTED).detectorExecutedCount,
        )
        assertEquals(export.modelArtifacts.map { it.sha256 }, parsed.modelArtifacts.map { it.sha256 })
        assertEquals(export.imageManifest.map { it.sha256 }, parsed.imageManifest.map { it.sha256 })
        assertEquals(export.deviceEnvironment.buildIdentifier, parsed.deviceEnvironment.buildIdentifier)
        assertTrue(parsed.classifierStage!!.rawRuns.none { it.detectorExecuted })
    }

    @Test
    fun jsonCarriesNoNaNOrInfinityAndNoNullStats() {
        val json = formatBenchmarkExportJson(sampleExport())
        for (bad in listOf("NaN", "Infinity", "-Infinity")) {
            assertFalse(json.contains("\"$bad\"") || json.contains(": $bad"), "export contained $bad")
        }
    }

    @Test
    fun csvIsWellFormedAndLabelsEveryPath() {
        val csv = formatBenchmarkExportCsv(sampleExport())
        for (path in listOf(BenchmarkPath.ACCEPTED, BenchmarkPath.REJECTED, BenchmarkPath.MISMATCH)) {
            assertTrue(csv.contains("end_to_end_$path"), "CSV is missing the $path block")
            assertTrue(csv.contains("detector_executed,"), "CSV omits the detector-executed counter")
        }
        assertTrue(csv.contains("orientation_correction"), "CSV omits the orientation stage")
        assertTrue(csv.contains("output_transfer"), "CSV omits the output-transfer stage")
        assertFalse(csv.contains("NaN") || csv.contains("Infinity"))

        // Every row that claims to be a stats row must have the full column count.
        val header = "stage,n,meanMs,medianMs,stdDevMs,minMs,maxMs,p50Ms,p90Ms,p95Ms,p99Ms"
        val expectedColumns = header.split(",").size
        var inStats = false
        for (line in csv.lines()) {
            when {
                line == header -> inStats = true
                line.isBlank() -> inStats = false
                inStats -> assertEquals(
                    expectedColumns, line.split(",").size,
                    "malformed stats row: $line",
                )
            }
        }
    }

    @Test
    fun everyPathCarriesAMeasuredRoutingStage() {
        val export = sampleExport()
        for ((path, e2e) in export.endToEndByPath) {
            val breakdown = e2e.stageBreakdown
            assertTrue(breakdown != null, "$path has no stage breakdown")
            assertTrue(breakdown!!.routing.meanMs > 0.0, "$path routing is a constant zero")
            assertEquals(e2e.measuredRuns, breakdown.routing.n)
            // A skipped detector must report no detector inference time at all.
            if (e2e.detectorExecutedCount == 0) {
                assertEquals(0.0, breakdown.detectorInference.maxMs)
            } else {
                assertTrue(breakdown.detectorInference.meanMs > 0.0)
            }
        }
    }

    @Test
    fun perPathStageBreakdownSurvivesJsonAndAppearsInCsv() {
        val export = sampleExport()
        val parsed = Json { ignoreUnknownKeys = true }
            .decodeFromString(BenchmarkExport.serializer(), formatBenchmarkExportJson(export))
        val routing = parsed.endToEndByPath.getValue(BenchmarkPath.ACCEPTED).stageBreakdown!!.routing
        assertEquals(
            export.endToEndByPath.getValue(BenchmarkPath.ACCEPTED).stageBreakdown!!.routing.meanMs,
            routing.meanMs,
        )
        val csv = formatBenchmarkExportCsv(export)
        for (path in export.endToEndByPath.keys) {
            assertTrue(csv.contains("end_to_end_${path}_stages"), "CSV omits $path stage breakdown")
        }
        assertTrue(csv.contains("\nrouting,"), "CSV omits the routing stage row")
    }

    @Test
    fun v2ReadersStillFindTheEndToEndField() {
        // Compatibility: `endToEnd` was the v2 field name and still carries a
        // single, un-blended path — never a mixture of detector and no-detector runs.
        val export = sampleExport()
        assertEquals(BenchmarkPath.ACCEPTED, export.endToEnd.path)
        assertEquals(export.endToEnd.measuredRuns, export.endToEnd.detectorExecutedCount)
    }

    @Test
    fun omittedClassifierStageSerializesAsNullNotAsZeros() {
        val export = sampleExport().copy(classifierStage = null)
        val json = formatBenchmarkExportJson(export)
        assertTrue(json.contains("\"classifierStage\": null"))
        val csv = formatBenchmarkExportCsv(export)
        assertTrue(csv.contains("unavailable,see notes"))
    }
}
