@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.nkwabyte.cropdiseasedetection.common.helpers

import com.nkwabyte.cropdiseasedetection.BuildKonfig
import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_COLD_LOAD_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_MEASURED_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_WARMUP_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkExport
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkImage
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkImageManifestEntry
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkImageSet
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkResult
import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.ColdLoadBenchmark
import com.nkwabyte.cropdiseasedetection.common.model.CpuUtilization
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelCatalog
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelSpec
import com.nkwabyte.cropdiseasedetection.common.model.DetectionOutputLayout
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.DeviceEnvironment
import com.nkwabyte.cropdiseasedetection.common.model.EndToEndBenchmark
import com.nkwabyte.cropdiseasedetection.common.model.MemorySample
import com.nkwabyte.cropdiseasedetection.common.model.ModelArtifact
import com.nkwabyte.cropdiseasedetection.common.model.OrientedImageSize
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkPath
import com.nkwabyte.cropdiseasedetection.common.model.emptyStageLatency
import com.nkwabyte.cropdiseasedetection.common.pipeline.DetectionPipeline
import com.nkwabyte.cropdiseasedetection.common.pipeline.ObjectDetectorEngine
import com.nkwabyte.cropdiseasedetection.common.pipeline.PipelineOutcome
import com.nkwabyte.cropdiseasedetection.common.pipeline.RoutingDecision
import com.nkwabyte.cropdiseasedetection.common.model.StageBenchmark
import com.nkwabyte.cropdiseasedetection.common.model.StageLatencyMs
import com.nkwabyte.cropdiseasedetection.common.model.computeLatencyStats
import com.nkwabyte.cropdiseasedetection.common.model.computeStageBenchmark
import com.nkwabyte.cropdiseasedetection.common.model.formatBenchmarkCsv
import com.nkwabyte.cropdiseasedetection.common.model.formatBenchmarkExportCsv
import com.nkwabyte.cropdiseasedetection.common.model.formatBenchmarkExportJson
import com.nkwabyte.cropdiseasedetection.bridge.ExecuTorchBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.Foundation.thermalState
import com.nkwabyte.cropdiseasedetection.common.utils.formatDecimals
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoThermalState
import platform.Foundation.create
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.convert
import platform.posix.memcpy
import kotlin.math.roundToInt

actual class ObjectDetector actual constructor() : KoinComponent {
    private val settingsManager: SettingsManager by inject()
    private val bridge = ExecuTorchBridge.shared()

    actual val isLoaded: Boolean
        get() = _isLoaded
    private var _isLoaded = false

    /** The spec the currently loaded model was built from — detect() decodes
     *  against this, and a settings change is detected by comparing ids. */
    private var _loadedSpec: DetectionModelSpec? = null

    actual val isClassifierLoaded: Boolean
        get() = _isClassifierLoaded
    private var _isClassifierLoaded = false

    private var _lastExtendedBenchmarkFiles: List<String> = emptyList()
    private var _lastLatencyBenchmarkFile: String? = null

    /** Documents-directory paths of the JSON and CSV the last extended run wrote.
     *  The Settings screen's share sheet hands these exact files to the user, so
     *  the artifact that gets cited is the one the harness itself produced. */
    actual val lastExtendedBenchmarkFiles: List<String>
        get() = _lastExtendedBenchmarkFiles

    actual val lastLatencyBenchmarkFile: String?
        get() = _lastLatencyBenchmarkFile

    /**
     * Loads the detector the user selected, replacing the loaded one if the
     * selection changed. Safe and cheap to call before every detection: it only
     * reloads when the model actually differs.
     */
    actual suspend fun loadModel() {
        val spec = DetectionModelCatalog.byId(settingsManager.getDetectionModel())
        if (_isLoaded && _loadedSpec?.id == spec.id) return

        val modelName = spec.assetName.removeSuffix(".pte")
        val path = NSBundle.mainBundle.pathForResource(modelName, ofType = "pte")
        if (path == null) {
            println("[iOS-ObjectDetector] ERROR: '$modelName.pte' not found in bundle")
            _isLoaded = false
            _loadedSpec = null
            return
        }
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
        val sizeBytes = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
        _isLoaded = bridge.loadDetectionModelAtPath(path)
        _loadedSpec = if (_isLoaded) spec else null
        if (_isLoaded) {
            println("[iOS-ObjectDetector] Detection model loaded")
            println("[iOS-ObjectDetector]   File         : $modelName.pte  (${spec.displayName})")
            println("[iOS-ObjectDetector]   Path         : $path")
            println("[iOS-ObjectDetector]   Size         : ${formatMb(sizeBytes)} MB ($sizeBytes bytes)")
            println("[iOS-ObjectDetector]   Task         : object_detection  |  Layout: ${spec.layout}")
            println("[iOS-ObjectDetector]   Input        : ${spec.inputSize}×${spec.inputSize} RGB  |  Normalize: pixel/255  |  ${if (spec.letterbox) "letterboxed" else "stretched"}")
            println("[iOS-ObjectDetector]   Classes      : ${spec.numClasses}  |  NMS: ${spec.applyNms}")
            println("[iOS-ObjectDetector]   Backend      : XNNPACK (ExecuTorch 1.3.1)")
        } else {
            println("[iOS-ObjectDetector] ERROR: ExecuTorch failed to load '$modelName.pte'")
        }
    }

    actual suspend fun loadClassifierModel() {
        val modelName = CLASSIFIER_RESOURCE
        val path = NSBundle.mainBundle.pathForResource(modelName, ofType = "pte")
        if (path == null) {
            println("[iOS-ObjectDetector] ERROR: '$modelName.pte' not found in bundle")
            return
        }
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
        val sizeBytes = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
        _isClassifierLoaded = bridge.loadClassifierModelAtPath(path)
        if (_isClassifierLoaded) {
            println("[iOS-ObjectDetector] Classifier model loaded")
            println("[iOS-ObjectDetector]   File         : $modelName.pte")
            println("[iOS-ObjectDetector]   Path         : $path")
            println("[iOS-ObjectDetector]   Size         : ${formatMb(sizeBytes)} MB ($sizeBytes bytes)")
            println("[iOS-ObjectDetector]   Architecture : EfficientNet-B2  |  Task: image_classification")
            println("[iOS-ObjectDetector]   Input        : 260×260 RGB  |  Normalize: ImageNet")
            println("[iOS-ObjectDetector]   Classes      : ${CROP_CLASSES.size} (${CROP_CLASSES.joinToString()})  |  rejection: argmax=='Other' or below the confidence floor")
            println("[iOS-ObjectDetector]   Backend      : XNNPACK (ExecuTorch 1.3.1)")
        } else {
            println("[iOS-ObjectDetector] ERROR: ExecuTorch failed to load '$modelName.pte'")
        }
    }

    // -------------------------------------------------------------------------------
    // Public inference entry points. Both delegate their postprocessing to the
    // private helpers below (postprocessClassification / decodeAndFilterDetections)
    // so the *WithStageTiming benchmark variants further down can time the Kotlin
    // side of the pipeline without touching what gets computed — classify() and
    // detect() are byte-for-byte the same algorithm as before this file was
    // reorganized.
    // -------------------------------------------------------------------------------

    // Each bridge call boxes the model's ENTIRE raw output tensor into individual
    // NSNumber objects — 226,800 of them for YOLO26's [1, 27, 8400], and again in
    // unletterboxBoxes — all autoreleased. Kotlin/Native worker threads have no
    // autorelease pool of their own, so without an explicit one those objects are
    // never reclaimed: a single scan leaks tens of MB, and a benchmark loop of a
    // few hundred calls reaches gigabytes and is killed by jetsam. Draining per
    // call is what keeps that bounded.
    actual fun classify(imageBytes: ByteArray): ClassificationResult? =
        classifyStageTimed(imageBytes).first

    actual fun detect(imageBytes: ByteArray): List<DetectionResult> =
        detectStageTimed(imageBytes).first

    /**
     * Post-orientation pixel dimensions, read through the bridge so the value
     * comes from the same `UIImage` orientation handling the preprocessing path
     * uses. Android computes the equivalent from the EXIF tag plus a bounds-only
     * decode; both report the dimensions of the upright image, which is what
     * makes the two platforms' results comparable.
     */
    actual fun orientedImageSize(imageBytes: ByteArray): OrientedImageSize? {
        val nsData = imageBytes.toNSData() ?: return null
        val raw = bridge.orientedImageSizeWithImageData(nsData)
        if (raw.size < 4) return null
        val width = (raw[0] as NSNumber).intValue
        val height = (raw[1] as NSNumber).intValue
        if (width <= 0 || height <= 0) return null
        return OrientedImageSize(
            widthPx = width,
            heightPx = height,
            exifOrientation = (raw[2] as NSNumber).intValue,
            orientationApplied = (raw[3] as NSNumber).doubleValue > 0.5,
        )
    }

    // -------------------------------------------------------------------------------
    // Postprocessing helpers extracted from classify()/detect(). Each does exactly
    // what the corresponding inline code used to do — nothing here changes
    // behavior, it only gives the step a name and a boundary a timer can be placed
    // around.
    // -------------------------------------------------------------------------------

    private fun postprocessClassification(logits: FloatArray): ClassificationResult {
        val n = CROP_CLASSES.size
        val maxLogit = logits.max()
        val exps = FloatArray(n) { kotlin.math.exp((logits[it] - maxLogit).toDouble()).toFloat() }
        val sum = exps.sum()
        val probs = FloatArray(n) { exps[it] / sum }

        val maxIdx = probs.indices.maxByOrNull { probs[it] } ?: 0
        val maxProb = probs[maxIdx]

        // Two rejection mechanisms that fail differently: the learned "Other" class
        // catches the non-crop species it was trained on, the confidence floor still
        // catches confidently-wrong predictions on species it has never seen.
        val threshold = settingsManager.getClassifierThreshold()
        val label = if (maxIdx == OTHER_INDEX || maxProb < threshold) "unknown" else CROP_CLASSES[maxIdx]
        // Deliberately NOT logged per call. postprocessClassification() runs inside
        // the region classifyStageTimed() measures, so a println here lands in the
        // middle of a timed span and inflates classifierInferenceMs — badly, when
        // stdout is piped over a device console during a 100-run benchmark, which
        // is also enough extra wall-clock to get the app suspended and killed
        // mid-run. The per-request outcome is still logged once, by
        // DetectionViewModel, outside any timed region.
        return ClassificationResult(
            label = label,
            confidence = maxProb,
            isAccepted = label != "unknown"
        )
    }

    /**
     * Unchanged decode/threshold/NMS logic; only the container changed from a
     * boxed `List<NSNumber>` to a primitive `FloatArray`. Class indexing, box
     * transformations, thresholds and NMS behaviour are identical.
     */
    private fun decodeAndFilterDetections(rawOutput: FloatArray, spec: DetectionModelSpec): List<DetectionResult> {
        val stride = spec.numClasses + 4
        val n = rawOutput.size / stride
        if (n <= 0) return emptyList()

        val detectionThreshold = settingsManager.getDetectionThreshold()
        val boxScale = if (spec.normalizedBoxes) 640f else 1f
        val preliminary = mutableListOf<DetectionResult>()

        for (i in 0 until n) {
            var maxScore = 0f
            var classId = -1
            var cx = 0f; var cy = 0f; var w = 0f; var h = 0f

            when (spec.layout) {
                // [1, 4 + numClasses, n] — a prediction's fields are n apart.
                DetectionOutputLayout.YOLO_ATTRIBUTE_MAJOR -> {
                    for (j in 0 until spec.numClasses) {
                        val score = rawOutput[(j + 4) * n + i]
                        if (score > maxScore) { maxScore = score; classId = j }
                    }
                    cx = rawOutput[0 * n + i]
                    cy = rawOutput[1 * n + i]
                    w  = rawOutput[2 * n + i]
                    h  = rawOutput[3 * n + i]
                }
                // [1, numQueries, 4 + numClasses] — one contiguous row per query.
                DetectionOutputLayout.DETR_QUERY_MAJOR -> {
                    val row = i * stride
                    for (j in 0 until spec.numClasses) {
                        val score = rawOutput[row + 4 + j]
                        if (score > maxScore) { maxScore = score; classId = j }
                    }
                    cx = rawOutput[row + 0]
                    cy = rawOutput[row + 1]
                    w  = rawOutput[row + 2]
                    h  = rawOutput[row + 3]
                }
            }

            if (maxScore > detectionThreshold && classId >= 0) {
                val sx = cx * boxScale; val sy = cy * boxScale
                val sw = w * boxScale;  val sh = h * boxScale
                preliminary.add(DetectionResult(
                    classIndex = classId,
                    score = maxScore,
                    box = floatArrayOf(sx - sw / 2f, sy - sh / 2f, sx + sw / 2f, sy + sh / 2f),
                    className = AppConstants.CLASS_LABELS.getOrElse(classId) { "Unknown" }
                ))
            }
        }

        // RT-DETR's query head already emits one box per object; running NMS over it
        // would merge distinct detections that legitimately overlap.
        return if (spec.applyNms) {
            nonMaxSuppression(preliminary, settingsManager.getIouThreshold())
        } else {
            preliminary
        }
    }

    // -------------------------------------------------------------------------------
    // Quick developer-button benchmark (unchanged from the original instrumentation).
    // -------------------------------------------------------------------------------

    actual suspend fun runLatencyBenchmark(): List<BenchmarkResult> =
        withContext(Dispatchers.Default) { runLatencyBenchmarkOnCurrentThread() }

    private suspend fun runLatencyBenchmarkOnCurrentThread(): List<BenchmarkResult> {
        val device = UIDevice.currentDevice
        val deviceInfo = "Apple ${device.model} (iOS ${device.systemVersion})"
        val results = mutableListOf<BenchmarkResult>()

        if (_isClassifierLoaded) {
            val rawLatencies = bridge.runLatencyBenchmarkForClassifierWithWarmupRuns(
                BENCHMARK_WARMUP_RUNS.toLong(),
                measuredRuns = BENCHMARK_MEASURED_RUNS.toLong()
            )
            if (rawLatencies.isNotEmpty()) {
                val latencies = rawLatencies.map { (it as NSNumber).doubleValue }
                results.add(
                    BenchmarkResult(
                        modelName = CLASSIFIER_RESOURCE,
                        stage = "classifier",
                        deviceInfo = deviceInfo,
                        warmupRuns = BENCHMARK_WARMUP_RUNS,
                        measuredRuns = BENCHMARK_MEASURED_RUNS,
                        stats = computeLatencyStats(latencies),
                        rawLatenciesMs = latencies
                    )
                )
            } else {
                println("[iOS-ObjectDetector] Classifier benchmark returned no measurements")
            }
        }

        val spec = _loadedSpec
        if (_isLoaded && spec != null) {
            // Detection latency depends on the pre-resize capture resolution too, so
            // sweep resolutions a real camera capture could plausibly hand in, rather
            // than just the model's fixed input size.
            for ((w, h) in BENCHMARK_IMAGE_SIZES) {
                val rawLatencies = bridge.runLatencyBenchmarkForDetectorWithInputSize(
                    spec.inputSize.toLong(),
                    isLetterbox = spec.letterbox,
                    imageWidth = w.toLong(),
                    imageHeight = h.toLong(),
                    warmupRuns = BENCHMARK_WARMUP_RUNS.toLong(),
                    measuredRuns = BENCHMARK_MEASURED_RUNS.toLong()
                )
                if (rawLatencies.isEmpty()) {
                    println("[iOS-ObjectDetector] Detector benchmark returned no measurements at ${w}x${h}")
                    continue
                }
                val latencies = rawLatencies.map { (it as NSNumber).doubleValue }
                results.add(
                    BenchmarkResult(
                        modelName = "${spec.assetName}@${w}x${h}",
                        stage = "detector",
                        deviceInfo = deviceInfo,
                        warmupRuns = BENCHMARK_WARMUP_RUNS,
                        measuredRuns = BENCHMARK_MEASURED_RUNS,
                        stats = computeLatencyStats(latencies),
                        rawLatenciesMs = latencies
                    )
                )
            }
        }

        if (results.isNotEmpty()) {
            writeBenchmarkCsv(results)
        }
        return results
    }

    private fun writeBenchmarkCsv(results: List<BenchmarkResult>) {
        val csv = formatBenchmarkCsv(results)
        val docsDirs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val docsDir = docsDirs.firstOrNull() as? String ?: return
        val fileName = "benchmark_" + (NSDate().timeIntervalSince1970 * 1000).toLong() + ".csv"
        val path = "$docsDir/$fileName"
        val written = (csv as NSString)
            .writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        _lastLatencyBenchmarkFile = if (written) path else null
        println("[iOS-ObjectDetector] Latency benchmark complete - wrote ${results.size} result rows to $path")
        for (r in results) {
            println(
                "[iOS-ObjectDetector]   ${r.stage}/${r.modelName}: mean=${r.stats.meanMs.formatDecimals(2)}ms " +
                    "p50=${r.stats.p50Ms.formatDecimals(2)}ms p95=${r.stats.p95Ms.formatDecimals(2)}ms (n=${r.stats.n})"
            )
        }
    }

    // -------------------------------------------------------------------------------
    // Publication-protocol extended benchmark.
    //
    // Every timed call below goes through classify()/detect(), the same private
    // postprocessing helpers they call, or the Swift bridge's *StageTimed methods
    // (which themselves call the exact same decode/orientation/preprocess/inference
    // helpers as the non-timed bridge methods — see ExecuTorchBridge.swift). This
    // function adds monotonic timers around existing calls; it does not reimplement
    // or alter what they compute. Metrics this platform/run cannot measure
    // defensibly are still reported, with an explicit caveat appended to `notes` —
    // never silently omitted or invented.
    // -------------------------------------------------------------------------------

    actual suspend fun runExtendedBenchmark(
        warmupRuns: Int,
        measuredRuns: Int,
    ): BenchmarkExport = withContext(Dispatchers.Default) {
        // Runs on Dispatchers.Default, the SAME dispatcher DetectionViewModel uses
        // for real inference. This matters more on iOS than on Android: SwiftUI
        // calls this from the main actor, and a 100-run protocol on the main
        // thread would freeze the interface for the whole run and report timings
        // from a thread production never uses.
        runExtendedBenchmarkOnCurrentThread(warmupRuns, measuredRuns)
    }

    private suspend fun runExtendedBenchmarkOnCurrentThread(
        warmupRuns: Int,
        measuredRuns: Int,
    ): BenchmarkExport {
        loadClassifierModel()
        loadModel()

        tracePhase("start")
        val notes = mutableListOf<String>()
        notes.add(
            "Timing uses a monotonic clock (DispatchTime.now().uptimeNanoseconds, " +
                "exposed to Kotlin via ExecuTorchBridge.monotonicNowMs()) around the " +
                "same classify()/detect() code paths used in production; no external " +
                "tool or automation-layer round-trip time is included in any figure " +
                "in this export."
        )
        notes.add(
            "Both platforms now correct EXIF image orientation before resizing — iOS " +
                "via UIImage.fixOrientation() inside ExecuTorchBridge.preprocessCHW, " +
                "Android via common/utils/ImageOrientation.kt — so " +
                "orientationCorrectionMs is a real measurement on both. The " +
                "Android-only gap recorded as C025, where Android fed the raw " +
                "sensor-order buffer to models trained on upright images and reported " +
                "orientationCorrectionMs as a constant 0.0, is fixed."
        )

        // ---- Cold model-loading time -------------------------------------------------
        val classifierColdLoadMs = mutableListOf<Double>()
        val detectorColdLoadMs = mutableListOf<Double>()
        repeat(BENCHMARK_COLD_LOAD_RUNS) {
            release()
            val t0 = bridge.monotonicNowMs()
            loadClassifierModel()
            val t1 = bridge.monotonicNowMs()
            loadModel()
            val t2 = bridge.monotonicNowMs()
            classifierColdLoadMs.add(t1 - t0)
            detectorColdLoadMs.add(t2 - t1)
        }
        tracePhase("after_cold_load_loop")
        val coldLoad = ColdLoadBenchmark(
            classifierColdLoad = if (classifierColdLoadMs.isNotEmpty()) computeLatencyStats(classifierColdLoadMs) else null,
            detectorColdLoad = if (detectorColdLoadMs.isNotEmpty()) computeLatencyStats(detectorColdLoadMs) else null,
            repetitions = BENCHMARK_COLD_LOAD_RUNS,
            note = "Each repetition calls release() first, so this is a genuine cold " +
                "load (module/spec state cleared), not the idempotent no-op " +
                "loadModel()/loadClassifierModel() take when already loaded. " +
                "release() is only invoked this way from the benchmark path; normal " +
                "app usage never forces a mid-session cold reload.",
        )

        val spec = _loadedSpec

        // ---- Model artifacts (size + SHA-256) -----------------------------------------
        val modelArtifacts = mutableListOf<ModelArtifact>()
        val classifierPath = NSBundle.mainBundle.pathForResource(CLASSIFIER_RESOURCE, ofType = "pte")
        if (classifierPath != null) {
            val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(classifierPath, error = null)
            val sizeBytes = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
            modelArtifacts.add(
                ModelArtifact(
                    name = "classifier",
                    assetFileName = "$CLASSIFIER_RESOURCE.pte",
                    sizeBytes = sizeBytes,
                    sha256 = bridge.sha256HexOfFileAtPath(classifierPath),
                )
            )
        } else {
            notes.add("Could not locate the classifier .pte in the app bundle for hashing.")
        }
        if (spec != null) {
            val modelName = spec.assetName.removeSuffix(".pte")
            val detectorPath = NSBundle.mainBundle.pathForResource(modelName, ofType = "pte")
            if (detectorPath != null) {
                val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(detectorPath, error = null)
                val sizeBytes = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
                modelArtifacts.add(
                    ModelArtifact(
                        name = "detector (${spec.displayName})",
                        assetFileName = spec.assetName,
                        sizeBytes = sizeBytes,
                        sha256 = bridge.sha256HexOfFileAtPath(detectorPath),
                    )
                )
            } else {
                notes.add("Could not locate the detector .pte in the app bundle for hashing.")
            }
        } else {
            notes.add("No detector model was loaded; the detector model artifact and all detector stage benchmarks are omitted (not zero-filled).")
        }

        // ---- Deterministic benchmark images + manifest (generated by the Swift ----
        // ---- bridge so both quick and extended benchmarks share one generator) -----
        val classifierImageData = bridge.syntheticJpegDataWithWidth(260, height = 260)
        val detectorImagesBySize: Map<Pair<Int, Int>, NSData?> =
            BENCHMARK_IMAGE_SIZES.associateWith { pair -> bridge.syntheticJpegDataWithWidth(pair.first.toLong(), height = pair.second.toLong()) }

        val imageManifest = mutableListOf<BenchmarkImageManifestEntry>()
        if (classifierImageData != null) {
            imageManifest.add(
                BenchmarkImageManifestEntry(
                    id = "classifier_260x260",
                    widthPx = 260,
                    heightPx = 260,
                    sizeBytes = classifierImageData.length.toLong(),
                    sha256 = bridge.sha256HexOfData(classifierImageData),
                    source = "synthetic_striped_pattern",
                )
            )
        } else {
            notes.add("Could not generate the classifier synthetic benchmark image.")
        }
        for ((w, h) in BENCHMARK_IMAGE_SIZES) {
            val data = detectorImagesBySize.getValue(w to h)
            if (data != null) {
                imageManifest.add(
                    BenchmarkImageManifestEntry(
                        id = "detector_${w}x${h}",
                        widthPx = w,
                        heightPx = h,
                        sizeBytes = data.length.toLong(),
                        sha256 = bridge.sha256HexOfData(data),
                        source = "synthetic_striped_pattern",
                    )
                )
            } else {
                notes.add("Could not generate the detector synthetic benchmark image at ${w}x${h}.")
            }
        }
        notes.add(
            "All benchmark images are deterministic synthetic striped patterns " +
                "generated by ExecuTorchBridge.syntheticJpegData, not photographs. " +
                "Legitimate for latency-only timing (compute cost is driven by " +
                "tensor shape, not pixel content) but NOT used for any accuracy or " +
                "output-agreement claim here — the cross-platform output-agreement " +
                "procedure uses real checksum-locked images instead."
        )

        tracePhase("after_artifacts_and_images")
        // ---- Classifier stage benchmark -----------------------------------------------
        val classifierRuns = mutableListOf<StageLatencyMs>()
        if (classifierImageData != null) {
            // The pool drains after each iteration's StageLatencyMs is already
            // computed, so the reclaim cost never lands inside a measured span.
            repeat(warmupRuns) { classifyStageTimed(classifierImageData) }
            repeat(measuredRuns) { classifierRuns.add(classifyStageTimed(classifierImageData).second) }
        }
        val classifierStage = if (classifierRuns.isEmpty()) {
            // A zero-filled StageBenchmark is indistinguishable from an
            // immeasurably fast classifier. Omit it and say why instead.
            notes.add(
                "Classifier model was not loaded or the benchmark image could not be " +
                    "generated; classifierStage is omitted entirely rather than " +
                    "zero-filled, and no end-to-end routing path could run."
            )
            null
        } else {
            computeStageBenchmark("classifier_260x260", warmupRuns, classifierRuns)
        }

        tracePhase("after_classifier_stage")
        // ---- Detector stage benchmark per resolution -----------------------------------
        val detectorStageBySize = mutableMapOf<String, StageBenchmark>()
        if (_isLoaded && spec != null) {
            for ((w, h) in BENCHMARK_IMAGE_SIZES) {
                val data = detectorImagesBySize.getValue(w to h) ?: continue
                repeat(warmupRuns) { detectStageTimed(data) }
                val runs = mutableListOf<StageLatencyMs>()
                repeat(measuredRuns) { runs.add(detectStageTimed(data).second) }
                if (runs.isNotEmpty()) {
                    detectorStageBySize["${w}x${h}"] = computeStageBenchmark("detector_${w}x${h}", warmupRuns, runs)
                }
                tracePhase("detector_stage_${w}x${h}_done")
            }
        } else {
            notes.add("No detector model was loaded; detector stage benchmarks are omitted entirely.")
        }

        tracePhase("after_detector_stages")
        // ---- End-to-end benchmark: the CORRECTED production pipeline -----------------
        // DetectionPipeline is the same object DetectionViewModel.detect() drives:
        // ensure classifier loaded -> decode + EXIF orientation -> classifier
        // stages -> routing decision -> (only if routed) ensure detector loaded ->
        // detector stages -> exact class-id filtering.
        val pipeline = DetectionPipeline(ObjectDetectorEngine(this))
        val (e2eWidth, e2eHeight) = BENCHMARK_IMAGE_SIZES.first()
        val e2eImageData = detectorImagesBySize.getValue(e2eWidth to e2eHeight)
        val e2eImageId = "detector_${e2eWidth}x${e2eHeight}"
        val e2eBytes = e2eImageData?.toByteArray()

        val correctedStageOrder = listOf(
            "ensureClassifierLoaded(idempotent)",
            "decode+exifOrientation",
            "classifierPreprocess+inference+postprocess",
            "routingDecision",
            "ensureDetectorLoaded(idempotent, routed only)",
            "detectorPreprocess+inference+postprocess(routed only)",
            "classIdFiltering(routed only)",
        )

        suspend fun measurePath(
            pathId: String,
            bytes: ByteArray,
            imageId: String,
            selectedCropId: String?,
        ): EndToEndBenchmark {
            repeat(warmupRuns) { pipeline.run(bytes, selectedCropId) }
            val latencies = mutableListOf<Double>()
            var successCount = 0
            var failureCount = 0
            var detectorExecuted = 0
            var detectorSkipped = 0
            val failureMessages = mutableListOf<String>()
            // The pipeline's own per-stage timings for these same runs — the only
            // place the routing stage can actually be measured.
            val stageRuns = mutableListOf<StageLatencyMs>()
            repeat(measuredRuns) {
                try {
                    val t0 = bridge.monotonicNowMs()
                    val result = pipeline.run(bytes, selectedCropId, instrumented = true)
                    val t1 = bridge.monotonicNowMs()
                    latencies.add(t1 - t0)
                    if (result.detectorExecuted) detectorExecuted++ else detectorSkipped++
                    result.timing?.let { stageRuns.add(it) }
                    if (result.outcome == PipelineOutcome.CLASSIFIER_ERROR ||
                        result.outcome == PipelineOutcome.DETECTOR_ERROR
                    ) {
                        failureCount++
                        if (failureMessages.size < 20) {
                            failureMessages.add(result.errorMessage ?: result.outcome.name)
                        }
                    } else {
                        successCount++
                    }
                } catch (e: Exception) {
                    failureCount++
                    if (failureMessages.size < 20) failureMessages.add(e.message ?: "unknown error")
                }
            }
            return EndToEndBenchmark(
                observedStageOrder = correctedStageOrder,
                representativeImageId = imageId,
                stats = computeLatencyStats(latencies.ifEmpty { listOf(0.0) }),
                offlineSuccessCount = successCount,
                offlineFailureCount = failureCount,
                failureMessages = failureMessages,
                warmupRuns = warmupRuns,
                measuredRuns = measuredRuns,
                path = pathId,
                detectorExecutedCount = detectorExecuted,
                detectorSkippedCount = detectorSkipped,
                stageBreakdown = if (stageRuns.isEmpty()) {
                    null
                } else {
                    computeStageBenchmark("end_to_end_$pathId", warmupRuns, stageRuns)
                },
            )
        }

        // ---- The five routing paths, each against its own locked fixture --------
        // Identical manifest and structure to Android, so the two platforms
        // measure the same bytes through the same paths. Each fixture is
        // checksum-verified before use; a mismatch omits its paths rather than
        // measuring unknown bytes.
        val endToEndByPath = mutableMapOf<String, EndToEndBenchmark>()
        val fixtures = mutableMapOf<String, ByteArray>()
        for (image in BenchmarkImageSet.all) {
            val bytes = loadBenchmarkImage(image)
            if (bytes == null) {
                notes.add("Benchmark fixture '${image.assetName}' is missing from the bundle; every path that uses it is omitted, not zero-filled.")
                continue
            }
            val actual = bridge.sha256HexOfData(bytes.toNSData() ?: NSData())
            if (actual != image.sha256) {
                notes.add(
                    "Benchmark fixture '${image.assetName}' FAILED its checksum (expected ${image.sha256}, " +
                        "got $actual); it was NOT used and every path that depends on it is omitted."
                )
                continue
            }
            fixtures[image.id] = bytes
            val size = orientedImageSize(bytes)
            imageManifest.add(
                BenchmarkImageManifestEntry(
                    id = image.id,
                    widthPx = size?.widthPx ?: -1,
                    heightPx = size?.heightPx ?: -1,
                    sizeBytes = bytes.size.toLong(),
                    sha256 = actual,
                    source = "locked_real_photograph: ${image.groundTruthNote}",
                )
            )
        }

        for ((image, pathId) in listOf(
            BenchmarkImageSet.CORN to BenchmarkPath.ACCEPTED_CORN,
            BenchmarkImageSet.PEPPER to BenchmarkPath.ACCEPTED_PEPPER,
            BenchmarkImageSet.TOMATO to BenchmarkPath.ACCEPTED_TOMATO,
        )) {
            val bytes = fixtures[image.id] ?: continue
            endToEndByPath[pathId] = measurePath(
                pathId = pathId,
                bytes = bytes,
                imageId = image.id,
                selectedCropId = image.expectedCrop?.canonicalLabel,
            )
        }

        fixtures[BenchmarkImageSet.OUT_OF_DISTRIBUTION.id]?.let { bytes ->
            endToEndByPath[BenchmarkPath.REJECTED] = measurePath(
                pathId = BenchmarkPath.REJECTED,
                bytes = bytes,
                imageId = BenchmarkImageSet.OUT_OF_DISTRIBUTION.id,
                selectedCropId = null,
            )
        }

        fixtures[BenchmarkImageSet.CORN.id]?.let { bytes ->
            endToEndByPath[BenchmarkPath.MISMATCH] = measurePath(
                pathId = BenchmarkPath.MISMATCH,
                bytes = bytes,
                imageId = "${BenchmarkImageSet.CORN.id}_selected_tomato",
                selectedCropId = SupportedCrop.TOMATO.canonicalLabel,
            )
        }

        notes.add(
            "End-to-end paths are measured against FIXED, checksum-locked real " +
                "photographs (imageManifest entries whose source begins " +
                "'locked_real_photograph'), not synthetic patterns: accepted Corn, " +
                "accepted Pepper and accepted Tomato each use their own image and " +
                "canonical crop id; the out-of-distribution path uses a cassava " +
                "leaf; the mismatch path uses the Corn image with Tomato selected. " +
                "The same manifest is used on Android, so the two platforms measure " +
                "identical bytes."
        )

        val endToEnd = endToEndByPath[BenchmarkPath.ACCEPTED_CORN]
            ?: endToEndByPath[BenchmarkPath.ACCEPTED]
            ?: endToEndByPath[BenchmarkPath.REJECTED]
            ?: endToEndByPath.values.firstOrNull()
            ?: EndToEndBenchmark(
                observedStageOrder = correctedStageOrder,
                representativeImageId = e2eImageId,
                stats = computeLatencyStats(listOf(0.0)),
                offlineSuccessCount = 0,
                offlineFailureCount = 0,
                failureMessages = listOf("No routing path was measurable in this run"),
                warmupRuns = warmupRuns,
                measuredRuns = 0,
                path = "unmeasured",
                detectorExecutedCount = 0,
                detectorSkippedCount = 0,
            )

        notes.add(
            "endToEnd/endToEndByPath measure DetectionPipeline.run(), the same entry " +
                "point DetectionViewModel.detect() calls in the app. The classifier runs " +
                "first and gates the detector; a rejected or crop-mismatched image never " +
                "reaches detector inference, verifiable here as detectorExecutedCount == 0 " +
                "for those paths."
        )
        notes.add(
            "Latency is NOT comparable across entries of endToEndByPath: only the " +
                "accepted path includes detector inference. Do not average them together " +
                "or quote one as \"the\" end-to-end figure without its path."
        )
        notes.add(
            "\"Offline\" here means no network call occurs anywhere in classify()/" +
                "detect()/routing — all run entirely against on-device ExecuTorch " +
                "modules. offlineFailureCount counts classifier/detector error outcomes " +
                "and thrown exceptions during the timed call, not prediction accuracy."
        )

        tracePhase("after_end_to_end")
        // ---- CPU utilization (process-level proxy, not per-thread) --------------------
        val cpuUtilization = if (e2eBytes != null) {
            try {
                val cpuT0 = bridge.processCpuTimeMs()
                val wallT0 = bridge.monotonicNowMs()
                // Same corrected pipeline as the end-to-end loop.
                repeat(measuredRuns) { pipeline.run(fixtures[BenchmarkImageSet.CORN.id] ?: e2eBytes, null) }
                val cpuT1 = bridge.processCpuTimeMs()
                val wallT1 = bridge.monotonicNowMs()
                val cpuMs = cpuT1 - cpuT0
                val wallMs = wallT1 - wallT0
                CpuUtilization(
                    threadCpuTimeMs = cpuMs,
                    wallClockMs = wallMs,
                    utilizationRatio = if (wallMs > 0) cpuMs / wallMs else 0.0,
                    measuredOnThread = "process (getrusage RUSAGE_SELF — not per-thread)",
                    available = true,
                    note = "iOS reports PROCESS-level user+system CPU time via " +
                        "getrusage() (ExecuTorchBridge.processCpuTimeMs()), not a " +
                        "per-thread figure like Android's Debug.threadCpuTimeNanos() " +
                        "— the two numbers are not directly comparable across " +
                        "platforms in this export.",
                )
            } catch (e: Exception) {
                CpuUtilization(0.0, 0.0, 0.0, "unknown", false, "CPU utilization sampling threw: ${e.message}")
            }
        } else {
            CpuUtilization(0.0, 0.0, 0.0, "unknown", false, "Skipped: no end-to-end benchmark image available.")
        }

        // ---- Memory samples (outside the timed loop) -----------------------------------
        val memorySamples = mutableListOf<MemorySample>()
        memorySamples.add(sampleMemory("before_measured_loop"))
        memorySamples.add(sampleMemory("after_measured_loop"))
        notes.add(
            "Memory samples are resident-memory snapshots taken immediately before " +
                "and after the measured loop, never inside a timed call, so sampling " +
                "itself never perturbs the reported latency numbers — but for the " +
                "same reason they cannot capture a transient peak during a single " +
                "inference call."
        )

        // ---- Device environment ---------------------------------------------------------
        val deviceEnvironment = collectDeviceEnvironment()
        deviceEnvironment.thermalStatusNote?.let { notes.add(it) }
        deviceEnvironment.installedAppSizeNote?.let { notes.add(it) }
        if (deviceEnvironment.isEmulator) {
            notes.add(
                "This export was produced on the iOS SIMULATOR " +
                    "(${deviceEnvironment.model}), NOT a physical iPhone 15 Pro Max " +
                    "or any physical device. Do not cite any figure in this export " +
                    "as physical-device performance."
            )
        }

        val export = BenchmarkExport(
            generatedAtEpochMs = (NSDate().timeIntervalSince1970 * 1000).toLong(),
            deviceEnvironment = deviceEnvironment,
            modelArtifacts = modelArtifacts,
            imageManifest = imageManifest,
            coldLoad = coldLoad,
            classifierStage = classifierStage,
            detectorStageBySize = detectorStageBySize,
            endToEnd = endToEnd,
            endToEndByPath = endToEndByPath,
            cpuUtilization = cpuUtilization,
            memorySamples = memorySamples,
            notes = notes,
        )

        writeExtendedBenchmarkExport(export)
        return export
    }

    // -------------------------------------------------------------------------------
    // The stage-timed inference implementations.
    //
    // These are not mirrors of classify()/detect() — they ARE classify() and
    // detect(); the public functions delegate to them. Decode/orientation/
    // preprocess/inference timing comes from the Swift bridge's *StageTimed
    // methods, which call the exact same native helpers as the non-timed bridge
    // methods, and the Kotlin-side postprocessing is timed with the same
    // monotonic clock. One implementation, so production and benchmark cannot
    // measure different code.
    // -------------------------------------------------------------------------------

    // Every bridge call boxes the model's ENTIRE raw output tensor into individual
    // NSNumber objects — 226,800 of them for YOLO26's [1, 27, 8400] output, and
    // unletterboxBoxes writes a second set — all autoreleased. Kotlin/Native worker
    // threads carry no autorelease pool of their own, so nothing ever reclaims
    // them: one scan leaks tens of MB, and a benchmark of a few hundred calls
    // reaches gigabytes and is killed by jetsam ("Terminated due to memory issue"),
    // which is what happened on the iPhone 15 Pro Max.
    //
    // These two functions are the single choke point every caller goes through —
    // production classify()/detect(), DetectionPipeline via ObjectDetectorEngine,
    // and the benchmark stage loops — so draining here bounds all of them. They are
    // also the only place it CAN go: Kotlin forbids calling a suspend function
    // inside autoreleasepool {} (KT-50786), which rules out the benchmark loops and
    // anything calling pipeline.run(). The pool drains after the inner function has
    // already produced its StageLatencyMs, so no measured span includes the reclaim.
    actual fun classifyStageTimed(imageBytes: ByteArray): Pair<ClassificationResult?, StageLatencyMs> {
        val nsData = imageBytes.toNSData()
            ?: return null to emptyStageLatency(0.0)
        return classifyStageTimed(nsData)
    }

    actual fun detectStageTimed(imageBytes: ByteArray): Pair<List<DetectionResult>, StageLatencyMs> {
        val nsData = imageBytes.toNSData()
            ?: return emptyList<DetectionResult>() to emptyStageLatency(0.0)
        return detectStageTimed(nsData)
    }

    private fun classifyStageTimed(imageData: NSData): Pair<ClassificationResult?, StageLatencyMs> =
        autoreleasepool { classifyStageTimedInPool(imageData) }

    private fun classifyStageTimedInPool(imageData: NSData): Pair<ClassificationResult?, StageLatencyMs> {
        val tStart = bridge.monotonicNowMs()
        if (!_isClassifierLoaded) {
            val tEnd = bridge.monotonicNowMs()
            return null to emptyStageLatency(tEnd - tStart)
        }

        val raw = bridge.runClassificationStageTimedWithImageData(imageData)
        val decodeMs = (raw[0] as NSNumber).doubleValue
        val orientationMs = (raw[1] as NSNumber).doubleValue
        val preprocessMs = (raw[2] as NSNumber).doubleValue
        val inferenceMs = (raw[3] as NSNumber).doubleValue
        val available = (raw[4] as NSNumber).doubleValue > 0.5

        val result = if (available && raw.size > 5) {
            val logits = FloatArray(CROP_CLASSES.size) { (raw[5 + it] as NSNumber).floatValue }
            postprocessClassification(logits)
        } else {
            null
        }
        val tEnd = bridge.monotonicNowMs()

        return result to StageLatencyMs(
            imageDecodeMs = decodeMs,
            orientationCorrectionMs = orientationMs,
            preprocessMs = preprocessMs,
            classifierInferenceMs = inferenceMs,
            routingMs = 0.0,
            detectorInferenceMs = 0.0,
            postprocessNmsMs = 0.0,
            totalMs = tEnd - tStart,
        )
    }

    // The pool lives on the NSData overloads, not the ByteArray ones, because the
    // benchmark loops call these directly — wrapping only the ByteArray entry
    // points left the 100-run stage loops draining nothing, which is why resident
    // memory still climbed through the detector stages while a 500-call soak
    // (which goes through the ByteArray path) plateaued. Every caller funnels
    // through here: production, DetectionPipeline, and the benchmark.
    private fun detectStageTimed(imageData: NSData): Pair<List<DetectionResult>, StageLatencyMs> =
        autoreleasepool { detectStageTimedInPool(imageData) }

    private fun detectStageTimedInPool(imageData: NSData): Pair<List<DetectionResult>, StageLatencyMs> {
        val tStart = bridge.monotonicNowMs()
        val spec = _loadedSpec
        if (!_isLoaded || spec == null) {
            val tEnd = bridge.monotonicNowMs()
            return emptyList<DetectionResult>() to emptyStageLatency(tEnd - tStart).copy(detectorExecuted = true)
        }

        val raw = if (spec.letterbox) {
            bridge.runDetectionStageTimedBufferWithImageData(imageData)
        } else {
            bridge.runDetectionStretchedStageTimedBufferWithImageData(imageData, inputSize = spec.inputSize.toLong())
        }

        val decodeMs = raw.double("imageDecodeMs")
        val orientationMs = raw.double("orientationMs")
        val preprocessMs = raw.double("preprocessMs")
        val forwardMs = raw.double("inferenceMs")
        val bridgeTransferMs = raw.double("outputTransferMs")
        val available = raw.double("available") > 0.5

        // The NSData -> FloatArray conversion is a mandatory part of getting a
        // usable output, so it is timed and folded into the detector total the
        // same way Android folds getDataAsFloatArray() into its own. It is also
        // reported on its own as outputTransferMs.
        val tConv0 = bridge.monotonicNowMs()
        val floats = if (available) {
            (raw["output"] as? NSData)?.toFloatArrayOrNull(expectedCount = raw.int("count"))
        } else {
            null
        }
        val tConv1 = bridge.monotonicNowMs()
        val outputTransferMs = bridgeTransferMs + (tConv1 - tConv0)

        if (floats == null) {
            if (available) {
                // available == true but the buffer did not validate: truncated,
                // mis-sized, or not a whole number of Float32s. Fail loudly rather
                // than decode garbage into detections.
                println(
                    "[iOS-ObjectDetector] Detector output buffer failed validation " +
                        "(count=${raw.int("count")}, bytes=${(raw["output"] as? NSData)?.length}); " +
                        "treating this run as producing no detections."
                )
            }
            val tEnd = bridge.monotonicNowMs()
            return emptyList<DetectionResult>() to StageLatencyMs(
                imageDecodeMs = decodeMs,
                orientationCorrectionMs = orientationMs,
                preprocessMs = preprocessMs,
                classifierInferenceMs = 0.0,
                routingMs = 0.0,
                detectorInferenceMs = forwardMs + outputTransferMs,
                postprocessNmsMs = 0.0,
                totalMs = tEnd - tStart,
                detectorExecuted = true,
                outputTransferMs = outputTransferMs,
            )
        }

        val tPost0 = bridge.monotonicNowMs()
        val detections = decodeAndFilterDetections(floats, spec)
        val tPost1 = bridge.monotonicNowMs()
        val tEnd = bridge.monotonicNowMs()

        return detections to StageLatencyMs(
            imageDecodeMs = decodeMs,
            orientationCorrectionMs = orientationMs,
            preprocessMs = preprocessMs,
            classifierInferenceMs = 0.0,
            routingMs = 0.0,
            // Comparable with Android: model forward PLUS the mandatory
            // materialization of a usable primitive buffer.
            detectorInferenceMs = forwardMs + outputTransferMs,
            postprocessNmsMs = tPost1 - tPost0,
            totalMs = tEnd - tStart,
            detectorExecuted = true,
            outputTransferMs = outputTransferMs,
        )
    }

    // -------------------------------------------------------------------------------
    // Diagnostics for the raw-output transport change (schema v4). Not used by
    // the app or by the benchmark; driven by DEBUG-only launch arguments.
    // -------------------------------------------------------------------------------

    /**
     * Proves the `[NSNumber]` -> `NSData` transport change is lossless.
     *
     * Both representations come from ONE forward pass, so a mismatch can only be
     * the transport, never model nondeterminism. Compares raw floats bit-for-bit
     * (via `toRawBits`, so NaN and -0.0 are compared exactly, not by `==`), then
     * decodes BOTH through the same `decodeAndFilterDetections` and compares the
     * resulting detections — count, class ids, scores and boxes.
     */
    fun runRawOutputEquivalenceCheck(imageBytes: ByteArray): String {
        val spec = _loadedSpec ?: return "EQUIVALENCE: FAIL — no detector loaded"
        val nsData = imageBytes.toNSData() ?: return "EQUIVALENCE: FAIL — could not wrap image bytes"

        val probe = bridge.runDetectionEquivalenceProbeWithImageData(
            nsData, isLetterbox = spec.letterbox, inputSize = spec.inputSize.toLong()
        )
        if (probe.double("available") <= 0.5) return "EQUIVALENCE: FAIL — probe unavailable"

        val declaredCount = probe.int("count")
        val boxed = probe["boxed"] as? List<*> ?: return "EQUIVALENCE: FAIL — no boxed output"
        val buffer = probe["buffer"] as? NSData ?: return "EQUIVALENCE: FAIL — no buffer output"
        val fromBuffer = buffer.toFloatArrayOrNull(expectedCount = declaredCount)
            ?: return "EQUIVALENCE: FAIL — buffer failed validation (count=$declaredCount, bytes=${buffer.length})"

        if (boxed.size != declaredCount || fromBuffer.size != declaredCount) {
            return "EQUIVALENCE: FAIL — element count mismatch " +
                "(declared=$declaredCount boxed=${boxed.size} buffer=${fromBuffer.size})"
        }

        var mismatches = 0
        var maxAbsDiff = 0.0f
        var firstMismatch = -1
        for (i in 0 until declaredCount) {
            val a = (boxed[i] as NSNumber).floatValue
            val b = fromBuffer[i]
            if (a.toRawBits() != b.toRawBits()) {
                mismatches++
                if (firstMismatch < 0) firstMismatch = i
                val d = kotlin.math.abs(a - b)
                if (d > maxAbsDiff) maxAbsDiff = d
            }
        }

        val boxedFloats = FloatArray(declaredCount) { (boxed[it] as NSNumber).floatValue }
        val viaBoxed = decodeAndFilterDetections(boxedFloats, spec)
        val viaBuffer = decodeAndFilterDetections(fromBuffer, spec)

        val detectionsMatch = viaBoxed.size == viaBuffer.size &&
            viaBoxed.indices.all { i ->
                val x = viaBoxed[i]; val y = viaBuffer[i]
                x.classIndex == y.classIndex &&
                    x.score.toRawBits() == y.score.toRawBits() &&
                    x.box.indices.all { k -> x.box[k].toRawBits() == y.box[k].toRawBits() }
            }

        val verdict = if (mismatches == 0 && detectionsMatch) "PASS" else "FAIL"
        return "EQUIVALENCE: $verdict — elements=$declaredCount " +
            "bitExactFloats=${declaredCount - mismatches}/$declaredCount " +
            "maxAbsDiff=$maxAbsDiff firstMismatchIndex=$firstMismatch " +
            "detections boxed=${viaBoxed.size} buffer=${viaBuffer.size} identical=$detectionsMatch " +
            "classIds=${viaBuffer.map { it.classIndex }}"
    }

    /**
     * Memory soak: [calls] detector invocations after a warm-up, reporting
     * resident memory at intervals. Resident memory must plateau; linear growth
     * is the regression this guards against (the boxed transport grew ~11.7 MB
     * per call and reached ~1.6 GB before jetsam killed the process).
     */
    fun runDetectorMemorySoak(imageBytes: ByteArray, calls: Int, warmup: Int): String {
        val lines = mutableListOf<String>()
        fun residentMb(): Double = bridge.residentMemoryBytes().toDouble() / (1024.0 * 1024.0)

        lines.add("SOAK start residentMB=${residentMb().formatDecimals(1)}")
        repeat(warmup) { detectStageTimed(imageBytes) }
        val afterWarmup = residentMb()
        lines.add("SOAK post-warmup(${warmup}) residentMB=${afterWarmup.formatDecimals(1)}")

        var peak = afterWarmup
        for (i in 1..calls) {
            detectStageTimed(imageBytes)
            val mb = residentMb()
            if (mb > peak) peak = mb
            if (i % 50 == 0) lines.add("SOAK call=$i residentMB=${mb.formatDecimals(1)}")
        }
        val finalMb = residentMb()
        lines.add("SOAK peak residentMB=${peak.formatDecimals(1)}")
        lines.add("SOAK final residentMB=${finalMb.formatDecimals(1)}")
        lines.add(
            "SOAK growthAfterWarmupMB=${(finalMb - afterWarmup).formatDecimals(1)} " +
                "perCallKB=${((finalMb - afterWarmup) * 1024.0 / calls).formatDecimals(1)}"
        )
        return lines.joinToString("\n")
    }

    // -------------------------------------------------------------------------------
    // Resource + provenance collection helpers for the extended benchmark.
    // -------------------------------------------------------------------------------

    /**
     * Coarse progress + resident-memory trace at phase boundaries only (never
     * per inference call, and never inside a timed span). Added after the
     * extended benchmark was killed by jetsam on a physical device with no
     * jetsam report available to say where the memory went.
     */

    private fun tracePhase(label: String) {
        val mb = bridge.residentMemoryBytes().toDouble() / (1024.0 * 1024.0)
        println("[iOS-Bench] phase=$label residentMB=${mb.formatDecimals(1)}")
    }

    private fun sampleMemory(label: String): MemorySample {
        return try {
            val residentBytes = bridge.residentMemoryBytes()
            val residentKb = (residentBytes / 1024uL).toLong()
            MemorySample(
                label = label,
                totalPssKb = residentKb.toInt(),
                nativeHeapAllocatedKb = 0L,
                javaHeapAllocatedKb = 0L,
                timestampEpochMs = (NSDate().timeIntervalSince1970 * 1000).toLong(),
                available = true,
                note = "On iOS, totalPssKb actually holds mach_task_basic_info." +
                    "resident_size (RSS-like, via ExecuTorchBridge.residentMemoryBytes())" +
                    " — iOS exposes no PSS-equivalent to app code, unlike Android's " +
                    "ActivityManager-based PSS. nativeHeapAllocatedKb/javaHeapAllocatedKb " +
                    "are Android-only concepts and are always 0 here, not measurements. " +
                    "Do not directly compare this figure to the Android totalPssKb " +
                    "value without accounting for the difference.",
            )
        } catch (e: Exception) {
            MemorySample(
                label = label, totalPssKb = 0, nativeHeapAllocatedKb = 0, javaHeapAllocatedKb = 0,
                timestampEpochMs = (NSDate().timeIntervalSince1970 * 1000).toLong(), available = false,
                note = "Memory sampling threw: ${e.message}",
            )
        }
    }

    private fun collectDeviceEnvironment(): DeviceEnvironment {
        val device = UIDevice.currentDevice
        val isSimulator = bridge.isRunningOnSimulator()
        val totalRamBytes = try {
            NSProcessInfo.processInfo.physicalMemory.toLong()
        } catch (e: Exception) {
            null
        }

        var batteryLevelPercent: Int? = null
        var isCharging: Boolean? = null
        try {
            device.batteryMonitoringEnabled = true
            val rawLevel = device.batteryLevel
            if (rawLevel >= 0f) {
                batteryLevelPercent = (rawLevel * 100).roundToInt()
            }
            isCharging = when (device.batteryState) {
                UIDeviceBatteryState.UIDeviceBatteryStateCharging, UIDeviceBatteryState.UIDeviceBatteryStateFull -> true
                UIDeviceBatteryState.UIDeviceBatteryStateUnplugged -> false
                else -> null
            }
        } catch (e: Exception) {
            // Leave both null — battery state genuinely unavailable on this device/run.
        }

        val thermalStatus = try {
            when (NSProcessInfo.processInfo.thermalState) {
                NSProcessInfoThermalState.NSProcessInfoThermalStateNominal -> "nominal"
                NSProcessInfoThermalState.NSProcessInfoThermalStateFair -> "fair"
                NSProcessInfoThermalState.NSProcessInfoThermalStateSerious -> "serious"
                NSProcessInfoThermalState.NSProcessInfoThermalStateCritical -> "critical"
                else -> "unknown"
            }
        } catch (e: Exception) {
            null
        }
        val thermalStatusNote = if (isSimulator) {
            "thermalStatus is reported by ProcessInfo, but this run is on the iOS Simulator: there is no real thermal sensor behind it — treat it as not meaningful."
        } else {
            null
        }

        val buildIdentifier = BuildKonfig.BUILD_GIT_SHA

        val infoDict = NSBundle.mainBundle.infoDictionary
        val appVersionName = (infoDict?.get("CFBundleShortVersionString") as? String) ?: "unknown"
        val appVersionCode = (infoDict?.get("CFBundleVersion") as? String)?.toLongOrNull() ?: -1L

        val installedAppSizeBytes = try {
            bridge.installedAppSizeBytes()
        } catch (e: Exception) {
            null
        }
        val installedAppSizeNote = "installedAppSizeBytes is the sum of file sizes under the " +
            "app's own bundle (ExecuTorchBridge.installedAppSizeBytes()), not the " +
            "figure Settings > General > iPhone Storage reports: it excludes " +
            "on-device install-time optimizations, App Store thinning already " +
            "applied, and any data written since install — a best-effort proxy, " +
            "like the Android APK-file-size figure it sits next to."

        return DeviceEnvironment(
            platform = "ios",
            manufacturer = "Apple",
            model = bridge.deviceModelIdentifier().ifBlank { device.model },
            osVersion = device.systemVersion,
            apiLevelOrOsBuild = "iOS ${device.systemVersion}",
            abis = listOf(bridge.cpuArchitecture()),
            isEmulator = isSimulator,
            totalRamBytes = totalRamBytes,
            batteryLevelPercent = batteryLevelPercent,
            isCharging = isCharging,
            thermalStatus = thermalStatus,
            thermalStatusNote = thermalStatusNote,
            buildIdentifier = buildIdentifier,
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            installedAppSizeBytes = installedAppSizeBytes,
            installedAppSizeNote = installedAppSizeNote,
        )
    }

    private fun writeExtendedBenchmarkExport(export: BenchmarkExport) {
        val docsDirs = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val docsDir = docsDirs.firstOrNull() as? String ?: return
        val timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong()
        // Deliberately a different filename pattern ("extended_benchmark_*") from the
        // quick button's "benchmark_*.csv" so neither overwrites or is confused with
        // the other, and any previously captured evidence is left untouched.
        val jsonPath = "$docsDir/extended_benchmark_$timestamp.json"
        val csvPath = "$docsDir/extended_benchmark_$timestamp.csv"
        val jsonWritten = (formatBenchmarkExportJson(export) as NSString)
            .writeToFile(jsonPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        val csvWritten = (formatBenchmarkExportCsv(export) as NSString)
            .writeToFile(csvPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        // Only report paths that actually exist, so the share sheet can never
        // offer a file that was never written.
        _lastExtendedBenchmarkFiles = buildList {
            if (jsonWritten) add(jsonPath)
            if (csvWritten) add(csvPath)
        }
        println(
            "[iOS-ObjectDetector] Extended benchmark complete - wrote $jsonPath and $csvPath " +
                "(device=${export.deviceEnvironment.manufacturer} ${export.deviceEnvironment.model}, " +
                "isEmulator=${export.deviceEnvironment.isEmulator}, endToEnd mean=" +
                "${export.endToEnd.stats.meanMs.formatDecimals(2)}ms p95=${export.endToEnd.stats.p95Ms.formatDecimals(2)}ms)"
        )
        for (note in export.notes) {
            println("[iOS-ObjectDetector]   NOTE: $note")
        }
    }

    actual fun loadBenchmarkImage(image: BenchmarkImage): ByteArray? {
        // Shipped as bundle resources so both platforms measure identical bytes.
        val name = image.assetName.substringBeforeLast('.')
        val ext = image.assetName.substringAfterLast('.')
        val path = NSBundle.mainBundle.pathForResource(name, ofType = ext)
            ?: run {
                println("[iOS-ObjectDetector] Benchmark fixture '${image.assetName}' not found in bundle")
                return null
            }
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return data.toByteArray()
    }

    actual fun release() {
        bridge.releaseModels()
        _isLoaded = false
        _loadedSpec = null
        _isClassifierLoaded = false
    }
}

// ---- helpers ----------------------------------------------------------------

// The 4-class classifier with a learned "Other" class. It matches the 3-class
// model on crop accuracy (97.54%) while rejecting 98.7% of non-crop images
// against the old 40.8% at a 0.55 threshold — see the project's
// docs/10_classifier_ood_adoption.md. Rejection is by argmax, with the
// confidence floor kept on top of it.
// Resolutions a real camera capture could plausibly hand the detector; decode +
// preprocessing cost scales with input resolution, so detector latency is swept
// across these rather than measured at just the model's fixed input size.
private val BENCHMARK_IMAGE_SIZES = listOf(1920 to 1080, 1280 to 960, 640 to 480)

private const val CLASSIFIER_RESOURCE = "crop_classifier_ood"
private val CROP_CLASSES = arrayOf("Corn", "Pepper", "Tomato", "Other")
private const val OTHER_INDEX = 3

/** Reads a Double out of the bridge's result dictionary, 0.0 when absent. */
private fun Map<Any?, *>.double(key: String): Double = (this[key] as? NSNumber)?.doubleValue ?: 0.0

/** Reads an Int out of the bridge's result dictionary, -1 when absent. */
private fun Map<Any?, *>.int(key: String): Int = (this[key] as? NSNumber)?.intValue ?: -1

/**
 * Decodes a contiguous native-byte-order Float32 buffer into a [FloatArray].
 *
 * Returns null — never a partially-filled or reinterpreted array — when the
 * buffer is malformed: a byte length that is not a multiple of 4, a length that
 * disagrees with the element count the bridge declared, or a null data pointer.
 * The copy target is a Kotlin FloatArray, which is naturally aligned, and
 * `memcpy` imposes no alignment requirement on the source, so no alignment
 * assumption is made about the incoming buffer. Nothing here retains the NSData
 * beyond the copy.
 */
private fun NSData.toFloatArrayOrNull(expectedCount: Int): FloatArray? {
    val byteCount = this.length.toLong()
    if (byteCount < 0L || byteCount % 4L != 0L) return null
    val count = (byteCount / 4L).toInt()
    if (expectedCount >= 0 && expectedCount != count) return null
    if (count == 0) return FloatArray(0)
    val source = this.bytes ?: return null
    val out = FloatArray(count)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), source, byteCount.convert())
    }
    return out
}

private fun ByteArray.toNSData(): NSData? = this.usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
}

/** The reverse of [ByteArray.toNSData] — needed so the extended benchmark can run
 *  the actual public classify()/detect() (ByteArray-based) end-to-end, on the
 *  same synthetic image bytes the Swift bridge generated as NSData. */
private fun NSData.toByteArray(): ByteArray {
    val len = this.length.toInt()
    val out = ByteArray(len)
    if (len > 0) {
        out.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length.convert())
        }
    }
    return out
}

private fun nonMaxSuppression(detections: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
    val sorted = detections.sortedByDescending { it.score }
    val result = mutableListOf<DetectionResult>()
    for (det in sorted) {
        val keep = result.none { kept ->
            det.classIndex == kept.classIndex && iou(det.box, kept.box) > iouThreshold
        }
        if (keep) result.add(det)
    }
    return result
}

private fun iou(a: FloatArray, b: FloatArray): Float {
    val x1 = maxOf(a[0], b[0]); val y1 = maxOf(a[1], b[1])
    val x2 = minOf(a[2], b[2]); val y2 = minOf(a[3], b[3])
    val inter = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
    val union = (a[2]-a[0])*(a[3]-a[1]) + (b[2]-b[0])*(b[3]-b[1]) - inter
    return if (union > 0f) inter / union else 0f
}

private fun formatMb(bytes: Long): String {
    val whole = bytes / (1024L * 1024L)
    val frac = (bytes % (1024L * 1024L)) * 100L / (1024L * 1024L)
    return "$whole.${frac.toString().padStart(2, '0')}"
}
