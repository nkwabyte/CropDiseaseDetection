package com.nkwabyte.cropdiseasedetection.common.helpers

import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_EXTENDED_MEASURED_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_EXTENDED_WARMUP_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkExport
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkResult
import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.OrientedImageSize
import com.nkwabyte.cropdiseasedetection.common.model.StageLatencyMs

expect class ObjectDetector() {
    val isLoaded: Boolean
    val isClassifierLoaded: Boolean
    suspend fun loadModel()
    suspend fun loadClassifierModel()
    /**
     * Runs the crop-type classifier. Thin wrapper over [classifyStageTimed] —
     * there is exactly one implementation of the algorithm, and this drops the
     * timings.
     */
    fun classify(imageBytes: ByteArray): ClassificationResult?

    /**
     * Runs the detector over the whole 23-class label space. Callers must route
     * the results through
     * [com.nkwabyte.cropdiseasedetection.common.pipeline.CropRoutingPolicy];
     * this function deliberately does no crop filtering of its own. Thin wrapper
     * over [detectStageTimed].
     */
    fun detect(imageBytes: ByteArray): List<DetectionResult>

    /**
     * The classifier, with per-stage monotonic timings (decode, EXIF
     * orientation correction, preprocessing, inference). This is the single
     * implementation: [classify] calls it and discards the timings, and the
     * extended benchmark calls it and keeps them, so production and benchmark
     * can never measure different code.
     */
    fun classifyStageTimed(imageBytes: ByteArray): Pair<ClassificationResult?, StageLatencyMs>

    /** The detector counterpart of [classifyStageTimed]. */
    fun detectStageTimed(imageBytes: ByteArray): Pair<List<DetectionResult>, StageLatencyMs>

    /**
     * Reads the image's dimensions AFTER EXIF orientation would be applied,
     * without running any model. These are the authoritative dimensions of the
     * pixels the models see; the encoded width/height differ whenever the EXIF
     * orientation transposes the image (values 5-8).
     */
    fun orientedImageSize(imageBytes: ByteArray): OrientedImageSize?

    /**
     * Runs the on-device latency benchmark (warmup + measured repetitions, synthetic
     * images) for whichever of the classifier/detector are currently loaded, and
     * writes a CSV of the results to platform-appropriate app storage.
     *
     * Requires [loadModel] and/or [loadClassifierModel] to have already been called;
     * a stage that isn't loaded is skipped rather than throwing. This is the quick
     * developer sanity check — see [runExtendedBenchmark] for the publication
     * protocol's controlled experiment.
     */
    suspend fun runLatencyBenchmark(): List<BenchmarkResult>

    /**
     * Runs the full publication-protocol benchmark: cold model-loading time;
     * per-stage classifier/detector timing (image decode, preprocessing,
     * inference, routing, postprocessing/NMS); complete end-to-end latency
     * matching the app's real call order; mean/median/stddev/p90/p95/p99/min/max;
     * best-effort memory, CPU and thermal sampling; model hashes and sizes;
     * installed app size; offline success/failure counts; build identifier;
     * device details; and an input-image manifest with checksums.
     *
     * All timing uses monotonic clocks (System.nanoTime() on Android,
     * CFAbsoluteTimeGetCurrent() on iOS) around the SAME [classify]/[detect]
     * code paths used in production — this function adds instrumentation
     * around existing calls, it does not alter what they compute. Metrics
     * this platform/run cannot measure defensibly are still reported, with an
     * explicit caveat in [BenchmarkExport.notes] — never silently omitted or
     * backfilled with an invented value. This function does NOT substitute
     * any external tool's request/round-trip duration for these numbers.
     *
     * [warmupRuns]/[measuredRuns] default to the publication protocol's
     * minimums (>=10 warm-up, >=100 measured); pass smaller values only for
     * interactive testing of the instrumentation itself, never to produce a
     * result set intended to be cited.
     *
     * Requires [loadModel] and [loadClassifierModel] to succeed at least once
     * during this call (it will call them itself if needed); leaves both
     * models loaded when it returns.
     */
    suspend fun runExtendedBenchmark(
        warmupRuns: Int = BENCHMARK_EXTENDED_WARMUP_RUNS,
        measuredRuns: Int = BENCHMARK_EXTENDED_MEASURED_RUNS,
    ): BenchmarkExport

    /**
     * Absolute paths of the files the most recent [runExtendedBenchmark] call
     * wrote — the JSON export first, then the CSV. Empty before the first run,
     * and empty if writing failed.
     *
     * Exposed because the caller is the only part of the system that can
     * actually do something with the files: on iOS the Settings screen offers
     * them through a share sheet, and on Android the runbook's `adb pull`
     * instructions need the exact filenames. Reporting them from here keeps one
     * writer of those files rather than having the UI re-serialize the export
     * and produce a second, near-identical artifact.
     */
    val lastExtendedBenchmarkFiles: List<String>

    /** Absolute path of the CSV the most recent [runLatencyBenchmark] wrote,
     *  or null before the first run or if writing failed. */
    val lastLatencyBenchmarkFile: String?

    fun release()
}
