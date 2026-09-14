@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.nkwabyte.cropdiseasedetection.common.helpers

import com.nkwabyte.cropdiseasedetection.BuildKonfig
import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_COLD_LOAD_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_MEASURED_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_WARMUP_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkExport
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkImageManifestEntry
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSData
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
        println("[iOS-ObjectDetector] Classifier probs: ${probs.joinToString()} -> Top: ${CROP_CLASSES[maxIdx]} (${maxProb}), Floor: $threshold, Label: $label")
        return ClassificationResult(
            label = label,
            confidence = maxProb,
            isAccepted = label != "unknown"
        )
    }

    private fun decodeAndFilterDetections(rawOutput: List<*>, spec: DetectionModelSpec): List<DetectionResult> {
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
                        val score = (rawOutput[(j + 4) * n + i] as NSNumber).floatValue
                        if (score > maxScore) { maxScore = score; classId = j }
                    }
                    cx = (rawOutput[0 * n + i] as NSNumber).floatValue
                    cy = (rawOutput[1 * n + i] as NSNumber).floatValue
                    w  = (rawOutput[2 * n + i] as NSNumber).floatValue
                    h  = (rawOutput[3 * n + i] as NSNumber).floatValue
                }
                // [1, numQueries, 4 + numClasses] — one contiguous row per query.
                DetectionOutputLayout.DETR_QUERY_MAJOR -> {
                    val row = i * stride
                    for (j in 0 until spec.numClasses) {
                        val score = (rawOutput[row + 4 + j] as NSNumber).floatValue
                        if (score > maxScore) { maxScore = score; classId = j }
                    }
                    cx = (rawOutput[row + 0] as NSNumber).floatValue
                    cy = (rawOutput[row + 1] as NSNumber).floatValue
                    w  = (rawOutput[row + 2] as NSNumber).floatValue
                    h  = (rawOutput[row + 3] as NSNumber).floatValue
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

    actual suspend fun runLatencyBenchmark(): List<BenchmarkResult> {
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
        (csv as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
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
    ): BenchmarkExport {
        loadClassifierModel()
        loadModel()

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

        // ---- Classifier stage benchmark -----------------------------------------------
        val classifierRuns = mutableListOf<StageLatencyMs>()
        if (classifierImageData != null) {
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
            }
        } else {
            notes.add("No detector model was loaded; detector stage benchmarks are omitted entirely.")
        }

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

        suspend fun measurePath(pathId: String, selectedCropId: String?): EndToEndBenchmark {
            val bytes = e2eBytes!!
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
                representativeImageId = e2eImageId,
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

        val endToEndByPath = mutableMapOf<String, EndToEndBenchmark>()
        if (e2eBytes == null) {
            notes.add(
                "Could not generate the end-to-end representative synthetic image; no " +
                    "end-to-end routing path was measured. This is an omission, not a zero."
            )
        } else {
            // Which paths this image can drive is a property of the classifier's
            // verdict on it, which the benchmark must not fake. Probe, then measure
            // only the paths that genuinely occur.
            when (val decision = pipeline.run(e2eBytes, null).decision) {
                is RoutingDecision.Accepted -> {
                    endToEndByPath[BenchmarkPath.ACCEPTED] =
                        measurePath(BenchmarkPath.ACCEPTED, decision.crop.canonicalLabel)
                    val otherCrop = SupportedCrop.entries.first { it != decision.crop }
                    endToEndByPath[BenchmarkPath.MISMATCH] =
                        measurePath(BenchmarkPath.MISMATCH, otherCrop.canonicalLabel)
                    notes.add(
                        "The benchmark image classifies as ${decision.crop.canonicalLabel}, so " +
                            "the accepted and selected-crop-mismatch paths are measured. The " +
                            "out-of-distribution rejection path is NOT measured in this export: " +
                            "forcing it would require a real non-crop photograph or a changed " +
                            "classifier threshold, and this benchmark does neither. Its absence " +
                            "is not a zero."
                    )
                }

                is RoutingDecision.Rejected -> {
                    endToEndByPath[BenchmarkPath.REJECTED] = measurePath(BenchmarkPath.REJECTED, null)
                    notes.add(
                        "The synthetic benchmark image is rejected by the classifier " +
                            "(label=\"${decision.label}\", confidence=${decision.confidence}), so " +
                            "only the out-of-distribution path is measured here — and it " +
                            "correctly never invokes the detector. The accepted and mismatch " +
                            "paths require an image the classifier accepts and are NOT measured " +
                            "in this export; their absence is not a zero."
                    )
                }

                else -> notes.add(
                    "Routing probe produced no usable decision; no end-to-end routing path " +
                        "could be measured for this run."
                )
            }
        }

        val endToEnd = endToEndByPath[BenchmarkPath.ACCEPTED]
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

        // ---- CPU utilization (process-level proxy, not per-thread) --------------------
        val cpuUtilization = if (e2eBytes != null) {
            try {
                val cpuT0 = bridge.processCpuTimeMs()
                val wallT0 = bridge.monotonicNowMs()
                // Same corrected pipeline as the end-to-end loop.
                repeat(measuredRuns) { pipeline.run(e2eBytes, null) }
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

    private fun classifyStageTimed(imageData: NSData): Pair<ClassificationResult?, StageLatencyMs> {
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

    private fun detectStageTimed(imageData: NSData): Pair<List<DetectionResult>, StageLatencyMs> {
        val tStart = bridge.monotonicNowMs()
        val spec = _loadedSpec
        if (!_isLoaded || spec == null) {
            val tEnd = bridge.monotonicNowMs()
            return emptyList<DetectionResult>() to emptyStageLatency(tEnd - tStart).copy(detectorExecuted = true)
        }

        val raw = if (spec.letterbox) {
            bridge.runDetectionStageTimedWithImageData(imageData)
        } else {
            bridge.runDetectionStretchedStageTimedWithImageData(imageData, inputSize = spec.inputSize.toLong())
        }
        val decodeMs = (raw[0] as NSNumber).doubleValue
        val orientationMs = (raw[1] as NSNumber).doubleValue
        val preprocessMs = (raw[2] as NSNumber).doubleValue
        val inferenceMs = (raw[3] as NSNumber).doubleValue
        val available = (raw[4] as NSNumber).doubleValue > 0.5

        val tPost0 = bridge.monotonicNowMs()
        val detections = if (available && raw.size > 5) {
            decodeAndFilterDetections(raw.subList(5, raw.size), spec)
        } else {
            emptyList()
        }
        val tPost1 = bridge.monotonicNowMs()
        val tEnd = bridge.monotonicNowMs()

        return detections to StageLatencyMs(
            imageDecodeMs = decodeMs,
            orientationCorrectionMs = orientationMs,
            preprocessMs = preprocessMs,
            classifierInferenceMs = 0.0,
            routingMs = 0.0,
            detectorInferenceMs = inferenceMs,
            postprocessNmsMs = tPost1 - tPost0,
            totalMs = tEnd - tStart,
            detectorExecuted = true,
        )
    }

    // -------------------------------------------------------------------------------
    // Resource + provenance collection helpers for the extended benchmark.
    // -------------------------------------------------------------------------------

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
        (formatBenchmarkExportJson(export) as NSString).writeToFile(jsonPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        (formatBenchmarkExportCsv(export) as NSString).writeToFile(csvPath, atomically = true, encoding = NSUTF8StringEncoding, error = null)
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
