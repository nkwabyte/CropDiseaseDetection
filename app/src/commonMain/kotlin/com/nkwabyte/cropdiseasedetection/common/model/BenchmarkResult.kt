package com.nkwabyte.cropdiseasedetection.common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// =============================================================================
// Run-size constants
// =============================================================================

/**
 * Defaults for the quick in-app "Run Latency Benchmark" developer button.
 * This is a fast sanity check (does the pipeline run, is the CSV written, is
 * the order of magnitude right) — NOT the controlled experiment the
 * publication protocol requires. See BENCHMARK_EXTENDED_* below for that.
 */
const val BENCHMARK_WARMUP_RUNS = 10
const val BENCHMARK_MEASURED_RUNS = 50

/**
 * Publication-protocol minimums: at least 10 discarded warm-up runs and at
 * least 100 measured runs per condition. Used by [runExtendedBenchmarkDefaults]
 * callers (the "Run Extended Benchmark (Publication Protocol)" action and any
 * external automation driving the controlled emulator/physical-device
 * experiment).
 */
const val BENCHMARK_EXTENDED_WARMUP_RUNS = 10
const val BENCHMARK_EXTENDED_MEASURED_RUNS = 100

/**
 * Number of forced-cold model-loading repetitions. Each repetition calls
 * release() first so the subsequent load is a genuine "not already loaded"
 * cold path, not the idempotent no-op that loadModel()/loadClassifierModel()
 * take when the requested spec is already resident.
 */
const val BENCHMARK_COLD_LOAD_RUNS = 5

/**
 * Bump on any breaking field change to BenchmarkExport.
 *
 * v3 (2026-09-14) — the extended benchmark now measures the CORRECTED
 * production pipeline (classify -> route -> detect) instead of the old
 * detect-then-classify order. Breaking changes from v2:
 *   - [StageLatencyMs] gains `detectorExecuted`, recorded per run.
 *   - [EndToEndBenchmark] gains `path`, `detectorExecutedCount` and
 *     `detectorSkippedCount`, and its `observedStageOrder` now names the real
 *     corrected stages.
 *   - [BenchmarkExport] gains `endToEndByPath`, holding one
 *     [EndToEndBenchmark] per routing path (accepted / rejected / mismatch).
 *     The single `endToEnd` field is retained, and holds the accepted path, so
 *     a v2 reader still finds the field it expects.
 * Migration: v2 consumers reading only `endToEnd`, `classifierStage`,
 * `detectorStageBySize`, `coldLoad`, `cpuUtilization`, `memorySamples`,
 * `modelArtifacts`, `imageManifest` and `deviceEnvironment` continue to work
 * unchanged; the added fields are additive. Consumers that asserted the v2
 * `observedStageOrder` value ["loadModel(idempotent)", "detect", "classify"]
 * must be updated — that order was the defect, not the contract.
 * The quick-benchmark CSV ([BenchmarkResult]/[formatBenchmarkCsv]) is
 * unversioned and unchanged.
 *
 * v4 (2026-09-14) — additive. [StageLatencyMs] and [StageBenchmark] gain
 * `outputTransfer`: the cost of materializing the detector's raw output into a
 * usable contiguous primitive buffer. It is a SUB-COMPONENT of
 * `detectorInference`, not an addition to it, and is reported separately only
 * so the transfer can be seen on its own. A v3 reader that ignores the new
 * field still reads a correct, comparable `detectorInference`.
 *
 * IMPORTANT for cross-version comparison: iOS `detectorInference` figures from
 * BEFORE v4 are NOT comparable with v4 and later. Until then the iOS bridge
 * boxed the entire raw output tensor into ~226,800 NSNumber objects inside the
 * measured span; v4 replaced that with a contiguous Float32 buffer. Android was
 * never affected — it has always read the output via
 * `Tensor.getDataAsFloatArray()`. See worklog ENTRY 014.
 */
const val BENCHMARK_EXPORT_SCHEMA_VERSION = 4

/** Named pipeline stages this project's benchmark instrumentation times individually. */
object BenchmarkStage {
    const val COLD_MODEL_LOAD = "cold_model_load"
    const val IMAGE_DECODE = "image_decode"
    const val ORIENTATION_CORRECTION = "orientation_correction"
    const val PREPROCESS = "preprocess"
    const val CLASSIFIER_INFERENCE = "classifier_inference"
    const val ROUTING = "routing"
    const val DETECTOR_INFERENCE = "detector_inference"
    const val POSTPROCESS_NMS = "postprocess_nms"
    const val END_TO_END = "end_to_end"
}

// =============================================================================
// Latency statistics
// =============================================================================

/**
 * Latency statistics for one set of timed samples, all in milliseconds.
 *
 * [p50Ms], [p90Ms], [p95Ms], [p99Ms] use nearest-rank percentiles (see
 * [computeLatencyStats]) — the same method for every percentile, so they are
 * directly comparable to each other. [medianMs] is the classic statistical
 * median (the average of the two middle values when [n] is even) and is
 * reported as its own field because it can differ from [p50Ms] by a fraction
 * of one sample step; both are kept rather than assuming they are
 * interchangeable.
 */
@Serializable
data class LatencyStats(
    val n: Int,
    val meanMs: Double,
    val medianMs: Double,
    val stdDevMs: Double,
    val minMs: Double,
    val maxMs: Double,
    val p50Ms: Double,
    val p90Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
)

/** Legacy flat per-model/per-stage row used by the quick "Run Latency Benchmark" button. */
@Serializable
data class BenchmarkResult(
    val modelName: String,
    val stage: String,
    val deviceInfo: String,
    val warmupRuns: Int,
    val measuredRuns: Int,
    val stats: LatencyStats,
    val rawLatenciesMs: List<Double>,
)

/**
 * Computes [LatencyStats] over a set of samples. Requires at least one
 * sample. Percentiles use nearest-rank (no interpolation between samples),
 * consistently across p50/p90/p95/p99.
 */
fun computeLatencyStats(latenciesMs: List<Double>): LatencyStats {
    require(latenciesMs.isNotEmpty()) { "Cannot compute latency stats over an empty sample set" }
    val sorted = latenciesMs.sorted()
    val n = sorted.size
    val mean = sorted.sum() / n
    val median = if (n % 2 == 0) {
        (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
    } else {
        sorted[n / 2]
    }
    val variance = if (n > 1) {
        sorted.sumOf { (it - mean) * (it - mean) } / (n - 1)
    } else {
        0.0
    }
    val stdDev = kotlin.math.sqrt(variance)
    fun nearestRank(p: Double): Double {
        if (n == 1) return sorted[0]
        val rank = kotlin.math.round(p * (n - 1)).toInt().coerceIn(0, n - 1)
        return sorted[rank]
    }
    return LatencyStats(
        n = n,
        meanMs = mean,
        medianMs = median,
        stdDevMs = stdDev,
        minMs = sorted.first(),
        maxMs = sorted.last(),
        p50Ms = nearestRank(0.50),
        p90Ms = nearestRank(0.90),
        p95Ms = nearestRank(0.95),
        p99Ms = nearestRank(0.99),
    )
}

// =============================================================================
// Stage-level breakdown
// =============================================================================

/**
 * One measured run's per-stage timings, in milliseconds, from monotonic clocks.
 *
 * [detectorExecuted] is part of the measurement, not metadata: in the corrected
 * pipeline a rejected or mismatched image never reaches the detector, so a run
 * with `detectorExecuted = false` legitimately has `detectorInferenceMs = 0.0`.
 * Without this flag those zeros are indistinguishable from a detector that ran
 * immeasurably fast, and averaging the two together silently understates
 * detector latency.
 */
@Serializable
data class StageLatencyMs(
    val imageDecodeMs: Double,
    val orientationCorrectionMs: Double,
    val preprocessMs: Double,
    val classifierInferenceMs: Double,
    val routingMs: Double,
    val detectorInferenceMs: Double,
    val postprocessNmsMs: Double,
    val totalMs: Double,
    val detectorExecuted: Boolean = false,
    /**
     * Cost of turning the detector's raw model output into a usable contiguous
     * primitive buffer — on iOS the tensor -> Float buffer -> NSData copies plus
     * the NSData -> FloatArray copy, on Android `getDataAsFloatArray()`.
     *
     * Already INCLUDED in [detectorInferenceMs]; reported separately so the
     * transfer is visible without being double-counted. Both platforms draw the
     * boundary the same way, so `detectorInferenceMs` stays comparable.
     */
    val outputTransferMs: Double = 0.0,
)

/** An all-zero sample carrying only a total — used when a stage breakdown is
 *  genuinely unavailable (e.g. the classifier threw before any stage ran). */
fun emptyStageLatency(totalMs: Double): StageLatencyMs = StageLatencyMs(
    imageDecodeMs = 0.0,
    orientationCorrectionMs = 0.0,
    preprocessMs = 0.0,
    classifierInferenceMs = 0.0,
    routingMs = 0.0,
    detectorInferenceMs = 0.0,
    postprocessNmsMs = 0.0,
    totalMs = totalMs,
    detectorExecuted = false,
    outputTransferMs = 0.0,
)

/**
 * Combines a classifier run's stage timings with a detector run's into one
 * end-to-end sample. Decode/orientation/preprocess are summed because both
 * stages genuinely perform them (each model has its own input size), which is
 * a real cost of the two-stage design and is reported rather than hidden.
 */
fun StageLatencyMs.mergeWithDetector(detector: StageLatencyMs): StageLatencyMs = copy(
    imageDecodeMs = imageDecodeMs + detector.imageDecodeMs,
    orientationCorrectionMs = orientationCorrectionMs + detector.orientationCorrectionMs,
    preprocessMs = preprocessMs + detector.preprocessMs,
    detectorInferenceMs = detector.detectorInferenceMs,
    postprocessNmsMs = detector.postprocessNmsMs,
    totalMs = totalMs + detector.totalMs,
    detectorExecuted = true,
    outputTransferMs = detector.outputTransferMs,
)

/** Aggregated statistics across N runs, one [LatencyStats] per named stage. */
@Serializable
data class StageBenchmark(
    val label: String,
    val warmupRuns: Int,
    val measuredRuns: Int,
    val imageDecode: LatencyStats,
    val orientationCorrection: LatencyStats,
    val preprocess: LatencyStats,
    val classifierInference: LatencyStats,
    val routing: LatencyStats,
    val detectorInference: LatencyStats,
    val postprocessNms: LatencyStats,
    val endToEnd: LatencyStats,
    /** Sub-component of [detectorInference]; see [StageLatencyMs.outputTransferMs]. */
    val outputTransfer: LatencyStats,
    val rawRuns: List<StageLatencyMs>,
)

/** Builds a [StageBenchmark] from raw per-run [StageLatencyMs] samples. */
fun computeStageBenchmark(label: String, warmupRuns: Int, runs: List<StageLatencyMs>): StageBenchmark {
    require(runs.isNotEmpty()) { "Cannot compute a stage benchmark over zero measured runs" }
    return StageBenchmark(
        label = label,
        warmupRuns = warmupRuns,
        measuredRuns = runs.size,
        imageDecode = computeLatencyStats(runs.map { it.imageDecodeMs }),
        orientationCorrection = computeLatencyStats(runs.map { it.orientationCorrectionMs }),
        preprocess = computeLatencyStats(runs.map { it.preprocessMs }),
        classifierInference = computeLatencyStats(runs.map { it.classifierInferenceMs }),
        routing = computeLatencyStats(runs.map { it.routingMs }),
        detectorInference = computeLatencyStats(runs.map { it.detectorInferenceMs }),
        postprocessNms = computeLatencyStats(runs.map { it.postprocessNmsMs }),
        endToEnd = computeLatencyStats(runs.map { it.totalMs }),
        outputTransfer = computeLatencyStats(runs.map { it.outputTransferMs }),
        rawRuns = runs,
    )
}

// =============================================================================
// Resource metrics
// =============================================================================

/**
 * CPU-utilization proxy. [threadCpuTimeMs] is a monotonic per-thread CPU-time
 * counter (Android: Debug.threadCpuTimeNanos()) sampled around the same span
 * as [wallClockMs], on the single thread the benchmark loop ran on.
 * [utilizationRatio] = threadCpuTimeMs / wallClockMs and CAN exceed 1.0 when
 * the inference runtime schedules work across multiple threads (e.g.
 * ExecuTorch/XNNPACK kernels) — that is expected, not an error, and is
 * exactly why this is documented as a "proxy" rather than a true
 * whole-process CPU-utilization percentage.
 */
@Serializable
data class CpuUtilization(
    val threadCpuTimeMs: Double,
    val wallClockMs: Double,
    val utilizationRatio: Double,
    val measuredOnThread: String,
    val available: Boolean,
    val note: String? = null,
)

/**
 * A single memory sample. Always taken OUTSIDE the tight timed loop (once
 * before the warm-up runs, once after the last measured run) so sampling
 * itself never perturbs the latency numbers sitting alongside it. This means
 * it cannot capture a transient peak during inference — see [note].
 */
@Serializable
data class MemorySample(
    val label: String,
    val totalPssKb: Int,
    val nativeHeapAllocatedKb: Long,
    val javaHeapAllocatedKb: Long,
    val timestampEpochMs: Long,
    val available: Boolean,
    val note: String? = null,
)

// =============================================================================
// Provenance: models, images, device/build
// =============================================================================

@Serializable
data class ModelArtifact(
    val name: String,
    val assetFileName: String,
    val sizeBytes: Long,
    val sha256: String,
)

@Serializable
data class BenchmarkImageManifestEntry(
    val id: String,
    val widthPx: Int,
    val heightPx: Int,
    val sizeBytes: Long,
    val sha256: String,
    val source: String,
)

/**
 * Identifies which device/build/environment produced a given [BenchmarkExport].
 *
 * [thermalStatus] is only populated on API 29+ (PowerManager
 * .getCurrentThermalStatus()); this app's minSdk is 24, so below API 29 it is
 * reported as null with [thermalStatusNote] explaining why. On an emulator it
 * is ALSO flagged unreliable regardless of API level, because there is no
 * real thermal sensor behind the reported value.
 *
 * [installedAppSizeBytes] is a best-effort figure (see [installedAppSizeNote]
 * for exactly what it does and does not include on this API level).
 */
@Serializable
data class DeviceEnvironment(
    val platform: String,
    val manufacturer: String,
    val model: String,
    val osVersion: String,
    val apiLevelOrOsBuild: String,
    val abis: List<String>,
    val isEmulator: Boolean,
    val totalRamBytes: Long?,
    val batteryLevelPercent: Int?,
    val isCharging: Boolean?,
    val thermalStatus: String?,
    val thermalStatusNote: String?,
    val buildIdentifier: String,
    val appVersionName: String,
    val appVersionCode: Long,
    val installedAppSizeBytes: Long?,
    val installedAppSizeNote: String?,
)

// =============================================================================
// End-to-end / cold-load
// =============================================================================

/** Which routing path an [EndToEndBenchmark] measured. Latency for these is
 *  NOT comparable: only [ACCEPTED] includes detector inference. */
object BenchmarkPath {
    /** Classifier accepts a supported crop -> detector runs -> results routed. */
    const val ACCEPTED = "accepted_supported_crop"

    /** Classifier rejects (learned `Other` / below floor) -> detector skipped. */
    const val REJECTED = "rejected_out_of_distribution"

    /** Classifier accepts a crop the user did not select -> detector skipped. */
    const val MISMATCH = "selected_crop_mismatch"

    /** Accepted paths, one per crop, measured against the locked real-image
     *  fixtures. Reported separately: they are different images through
     *  different detector class ranges and must never be pooled. */
    const val ACCEPTED_CORN = "accepted_corn"
    const val ACCEPTED_PEPPER = "accepted_pepper"
    const val ACCEPTED_TOMATO = "accepted_tomato"

    /** The per-crop accepted paths, for consumers that need to iterate them. */
    val acceptedCropPaths = listOf(ACCEPTED_CORN, ACCEPTED_PEPPER, ACCEPTED_TOMATO)
}

/**
 * A full-pipeline benchmark of the CORRECTED production order, as
 * `DetectionPipeline.run()` executes it: ensure classifier loaded -> decode +
 * EXIF orientation -> classifier preprocess/inference/postprocess -> routing
 * decision -> (only if routed) ensure detector loaded -> detector
 * preprocess/inference/postprocess -> exact class-id filtering.
 *
 * One instance per [BenchmarkPath]. [detectorExecutedCount] and
 * [detectorSkippedCount] are the machine-checkable evidence that the
 * fail-closed paths really did skip the detector: for [BenchmarkPath.REJECTED]
 * and [BenchmarkPath.MISMATCH] the executed count must be 0.
 */
@Serializable
data class EndToEndBenchmark(
    val observedStageOrder: List<String>,
    val representativeImageId: String,
    val stats: LatencyStats,
    val offlineSuccessCount: Int,
    val offlineFailureCount: Int,
    val failureMessages: List<String>,
    val warmupRuns: Int,
    val measuredRuns: Int,
    val path: String = BenchmarkPath.ACCEPTED,
    val detectorExecutedCount: Int = 0,
    val detectorSkippedCount: Int = 0,
    /**
     * Per-stage breakdown of this path's own measured runs, from the same
     * instrumented pipeline calls [stats] times. This is where `routing` is a
     * real measurement: the routing decision only exists in the composed
     * pipeline, so it cannot appear in the classifier-only or detector-only
     * stage benchmarks. Null when the path produced no instrumented samples.
     */
    val stageBreakdown: StageBenchmark? = null,
)

@Serializable
data class ColdLoadBenchmark(
    val classifierColdLoad: LatencyStats?,
    val detectorColdLoad: LatencyStats?,
    val repetitions: Int,
    val note: String,
)

// =============================================================================
// Top-level export bundle
// =============================================================================

/**
 * Everything the publication protocol asks for, in one bundle. [notes] is
 * where every metric this platform/run cannot measure defensibly gets
 * flagged explicitly, in prose, rather than silently omitted or backfilled
 * with a fake value — e.g. "this export is from the Medium_Phone emulator,
 * NOT a Galaxy A10 or any physical device", "thermalStatus unsupported below
 * API 29", "installedAppSizeBytes is the base APK file size, not true
 * installed size". Consumers of this export MUST read [notes] before citing
 * any figure in it.
 */
@Serializable
data class BenchmarkExport(
    val schemaVersion: Int = BENCHMARK_EXPORT_SCHEMA_VERSION,
    val generatedAtEpochMs: Long,
    val deviceEnvironment: DeviceEnvironment,
    val modelArtifacts: List<ModelArtifact>,
    val imageManifest: List<BenchmarkImageManifestEntry>,
    val coldLoad: ColdLoadBenchmark,
    /** Null when the classifier could not be benchmarked at all — an omitted
     *  stage, never a zero-filled one. See [notes] for the reason. */
    val classifierStage: StageBenchmark?,
    val detectorStageBySize: Map<String, StageBenchmark>,
    /** The accepted-crop path, kept under its v2 name so existing readers work. */
    val endToEnd: EndToEndBenchmark,
    /** Every measured routing path keyed by [BenchmarkPath]; includes the
     *  accepted path, so this is the complete set and [endToEnd] is a subset. */
    val endToEndByPath: Map<String, EndToEndBenchmark> = emptyMap(),
    val cpuUtilization: CpuUtilization,
    val memorySamples: List<MemorySample>,
    val notes: List<String>,
)

private val benchmarkExportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

/** Serializes a [BenchmarkExport] to pretty-printed JSON. */
fun formatBenchmarkExportJson(export: BenchmarkExport): String =
    benchmarkExportJson.encodeToString(BenchmarkExport.serializer(), export)

/**
 * Flattens a [BenchmarkExport] into a single CSV with clearly labeled
 * sections (metadata, device environment, model artifacts, image manifest,
 * cold load, classifier stage, detector stage per resolution, end-to-end,
 * CPU, memory, notes). Prefer [formatBenchmarkExportJson] for machine
 * consumption; this CSV is for quick spreadsheet inspection.
 */
fun formatBenchmarkExportCsv(export: BenchmarkExport): String {
    val sb = StringBuilder()

    fun statsRow(name: String, s: LatencyStats) {
        sb.append(
            listOf(
                name, s.n, s.meanMs, s.medianMs, s.stdDevMs, s.minMs, s.maxMs,
                s.p50Ms, s.p90Ms, s.p95Ms, s.p99Ms
            ).joinToString(",") { csvEscape(it.toString()) }
        )
        sb.append('\n')
    }

    sb.append("section,key,value\n")
    sb.append("meta,schemaVersion,${export.schemaVersion}\n")
    sb.append("meta,generatedAtEpochMs,${export.generatedAtEpochMs}\n")

    val env = export.deviceEnvironment
    sb.append("device,platform,${csvEscape(env.platform)}\n")
    sb.append("device,manufacturer,${csvEscape(env.manufacturer)}\n")
    sb.append("device,model,${csvEscape(env.model)}\n")
    sb.append("device,osVersion,${csvEscape(env.osVersion)}\n")
    sb.append("device,apiLevelOrOsBuild,${csvEscape(env.apiLevelOrOsBuild)}\n")
    sb.append("device,abis,${csvEscape(env.abis.joinToString(";"))}\n")
    sb.append("device,isEmulator,${env.isEmulator}\n")
    sb.append("device,totalRamBytes,${env.totalRamBytes ?: "unavailable"}\n")
    sb.append("device,batteryLevelPercent,${env.batteryLevelPercent ?: "unavailable"}\n")
    sb.append("device,isCharging,${env.isCharging ?: "unavailable"}\n")
    sb.append("device,thermalStatus,${env.thermalStatus ?: "unavailable"}\n")
    sb.append("device,thermalStatusNote,${csvEscape(env.thermalStatusNote ?: "")}\n")
    sb.append("device,buildIdentifier,${csvEscape(env.buildIdentifier)}\n")
    sb.append("device,appVersionName,${csvEscape(env.appVersionName)}\n")
    sb.append("device,appVersionCode,${env.appVersionCode}\n")
    sb.append("device,installedAppSizeBytes,${env.installedAppSizeBytes ?: "unavailable"}\n")
    sb.append("device,installedAppSizeNote,${csvEscape(env.installedAppSizeNote ?: "")}\n")

    sb.append("\nmodel_artifacts\nname,assetFileName,sizeBytes,sha256\n")
    for (m in export.modelArtifacts) {
        sb.append(listOf(m.name, m.assetFileName, m.sizeBytes.toString(), m.sha256).joinToString(",") { csvEscape(it) })
        sb.append('\n')
    }

    sb.append("\nimage_manifest\nid,widthPx,heightPx,sizeBytes,sha256,source\n")
    for (img in export.imageManifest) {
        sb.append(
            listOf(img.id, img.widthPx.toString(), img.heightPx.toString(), img.sizeBytes.toString(), img.sha256, img.source)
                .joinToString(",") { csvEscape(it) }
        )
        sb.append('\n')
    }

    sb.append("\ncold_load\nrepetitions,note\n")
    sb.append("${export.coldLoad.repetitions},${csvEscape(export.coldLoad.note)}\n")
    sb.append("\nstage,n,meanMs,medianMs,stdDevMs,minMs,maxMs,p50Ms,p90Ms,p95Ms,p99Ms\n")
    export.coldLoad.classifierColdLoad?.let { statsRow("cold_load_classifier", it) }
    export.coldLoad.detectorColdLoad?.let { statsRow("cold_load_detector", it) }

    val classifierStage = export.classifierStage
    if (classifierStage == null) {
        sb.append("\nclassifier_stage_benchmark\nunavailable,see notes\n")
    } else {
        sb.append("\nclassifier_stage_benchmark (warmup=${classifierStage.warmupRuns}, measured=${classifierStage.measuredRuns})\n")
        sb.append("stage,n,meanMs,medianMs,stdDevMs,minMs,maxMs,p50Ms,p90Ms,p95Ms,p99Ms\n")
        statsRow("image_decode", classifierStage.imageDecode)
        statsRow("orientation_correction", classifierStage.orientationCorrection)
        statsRow("preprocess", classifierStage.preprocess)
        statsRow("classifier_inference", classifierStage.classifierInference)
        statsRow("routing", classifierStage.routing)
        statsRow("end_to_end_classifier_only", classifierStage.endToEnd)
    }

    for ((sizeKey, stage) in export.detectorStageBySize) {
        sb.append("\ndetector_stage_benchmark_$sizeKey (warmup=${stage.warmupRuns}, measured=${stage.measuredRuns})\n")
        sb.append("stage,n,meanMs,medianMs,stdDevMs,minMs,maxMs,p50Ms,p90Ms,p95Ms,p99Ms\n")
        statsRow("image_decode", stage.imageDecode)
        statsRow("orientation_correction", stage.orientationCorrection)
        statsRow("preprocess", stage.preprocess)
        statsRow("detector_inference", stage.detectorInference)
        statsRow("output_transfer (subset of detector_inference)", stage.outputTransfer)
        statsRow("postprocess_nms", stage.postprocessNms)
        statsRow("end_to_end_detector_only", stage.endToEnd)
    }

    // One block per measured routing path. Latency across paths is NOT
    // comparable — only the accepted path includes detector inference — so each
    // block carries its path id and its detector executed/skipped counts.
    val paths = export.endToEndByPath.ifEmpty { mapOf(export.endToEnd.path to export.endToEnd) }
    for ((pathId, e2e) in paths) {
        sb.append("\nend_to_end_$pathId (warmup=${e2e.warmupRuns}, measured=${e2e.measuredRuns}, image=${csvEscape(e2e.representativeImageId)}, order=${csvEscape(e2e.observedStageOrder.joinToString("->"))})\n")
        sb.append("stage,n,meanMs,medianMs,stdDevMs,minMs,maxMs,p50Ms,p90Ms,p95Ms,p99Ms\n")
        statsRow("end_to_end_$pathId", e2e.stats)
        e2e.stageBreakdown?.let { br ->
            sb.append("\nend_to_end_${pathId}_stages\n")
            sb.append("stage,n,meanMs,medianMs,stdDevMs,minMs,maxMs,p50Ms,p90Ms,p95Ms,p99Ms\n")
            statsRow("image_decode", br.imageDecode)
            statsRow("orientation_correction", br.orientationCorrection)
            statsRow("preprocess", br.preprocess)
            statsRow("classifier_inference", br.classifierInference)
            statsRow("routing", br.routing)
            statsRow("detector_inference", br.detectorInference)
            statsRow("output_transfer (subset of detector_inference)", br.outputTransfer)
            statsRow("postprocess_nms", br.postprocessNms)
            statsRow("pipeline_total", br.endToEnd)
        }
        sb.append("\nend_to_end_${pathId}_counts\noutcome,count\n")
        sb.append("success,${e2e.offlineSuccessCount}\n")
        sb.append("failure,${e2e.offlineFailureCount}\n")
        sb.append("detector_executed,${e2e.detectorExecutedCount}\n")
        sb.append("detector_skipped,${e2e.detectorSkippedCount}\n")
        for (msg in e2e.failureMessages) {
            sb.append("failure_message,${csvEscape(msg)}\n")
        }
    }

    val cpu = export.cpuUtilization
    sb.append("\ncpu_utilization\nthreadCpuTimeMs,wallClockMs,utilizationRatio,measuredOnThread,available,note\n")
    sb.append(
        listOf(
            cpu.threadCpuTimeMs.toString(), cpu.wallClockMs.toString(), cpu.utilizationRatio.toString(),
            cpu.measuredOnThread, cpu.available.toString(), cpu.note ?: ""
        ).joinToString(",") { csvEscape(it) }
    )
    sb.append('\n')

    sb.append("\nmemory_samples\nlabel,totalPssKb,nativeHeapAllocatedKb,javaHeapAllocatedKb,timestampEpochMs,available,note\n")
    for (mem in export.memorySamples) {
        sb.append(
            listOf(
                mem.label, mem.totalPssKb.toString(), mem.nativeHeapAllocatedKb.toString(),
                mem.javaHeapAllocatedKb.toString(), mem.timestampEpochMs.toString(), mem.available.toString(), mem.note ?: ""
            ).joinToString(",") { csvEscape(it) }
        )
        sb.append('\n')
    }

    sb.append("\nnotes\n")
    for ((i, note) in export.notes.withIndex()) {
        sb.append("${i + 1},${csvEscape(note)}\n")
    }

    return sb.toString()
}

// =============================================================================
// Legacy quick-benchmark CSV (unchanged shape, still used by the dev button)
// =============================================================================

fun formatBenchmarkCsv(results: List<BenchmarkResult>): String {
    val sb = StringBuilder()
    sb.append("modelName,stage,deviceInfo,warmupRuns,measuredRuns,n,meanMs,medianMs,stdDevMs,minMs,maxMs,p50Ms,p90Ms,p95Ms,p99Ms\n")
    for (r in results) {
        sb.append(
            listOf(
                r.modelName, r.stage, r.deviceInfo, r.warmupRuns.toString(), r.measuredRuns.toString(),
                r.stats.n.toString(), r.stats.meanMs.toString(), r.stats.medianMs.toString(), r.stats.stdDevMs.toString(),
                r.stats.minMs.toString(), r.stats.maxMs.toString(), r.stats.p50Ms.toString(), r.stats.p90Ms.toString(),
                r.stats.p95Ms.toString(), r.stats.p99Ms.toString()
            ).joinToString(",") { csvEscape(it) }
        )
        sb.append('\n')
    }
    sb.append("\nraw_latencies_ms\nmodelName,stage,runIndex,latencyMs\n")
    for (r in results) {
        for ((i, v) in r.rawLatenciesMs.withIndex()) {
            sb.append(listOf(r.modelName, r.stage, i.toString(), v.toString()).joinToString(",") { csvEscape(it) })
            sb.append('\n')
        }
    }
    return sb.toString()
}

private fun csvEscape(value: String): String {
    return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}
