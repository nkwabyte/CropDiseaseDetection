package com.nkwabyte.cropdiseasedetection.common.helpers

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.os.Process
import android.util.Log
import androidx.core.graphics.scale
import com.nkwabyte.cropdiseasedetection.common.utils.ImageOrientation
import com.nkwabyte.cropdiseasedetection.BuildKonfig
import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_COLD_LOAD_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_EXTENDED_MEASURED_RUNS
import com.nkwabyte.cropdiseasedetection.common.model.BENCHMARK_EXTENDED_WARMUP_RUNS
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
import com.nkwabyte.cropdiseasedetection.common.model.BenchmarkPath
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.common.pipeline.BOX_COORDINATE_SPACE
import com.nkwabyte.cropdiseasedetection.common.pipeline.DetectionPipeline
import com.nkwabyte.cropdiseasedetection.common.pipeline.ObjectDetectorEngine
import com.nkwabyte.cropdiseasedetection.common.pipeline.PipelineOutcome
import com.nkwabyte.cropdiseasedetection.common.pipeline.RoutingDecision
import com.nkwabyte.cropdiseasedetection.common.model.StageLatencyMs
import com.nkwabyte.cropdiseasedetection.common.model.computeLatencyStats
import com.nkwabyte.cropdiseasedetection.common.model.computeStageBenchmark
import com.nkwabyte.cropdiseasedetection.common.model.formatBenchmarkCsv
import com.nkwabyte.cropdiseasedetection.common.model.formatBenchmarkExportCsv
import com.nkwabyte.cropdiseasedetection.common.model.formatBenchmarkExportJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

actual class ObjectDetector actual constructor() : KoinComponent {
    private val context: Context by inject()
    private val settingsManager: com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager by inject()
    private var _module: Module? = null
    private var _classifierModule: Module? = null

    /** The spec the currently loaded module was built from — detect() decodes
     *  against this, and a settings change is detected by comparing ids. */
    private var _loadedSpec: DetectionModelSpec? = null

    private var _lastExtendedBenchmarkFiles: List<String> = emptyList()
    private var _lastLatencyBenchmarkFile: String? = null

    actual val lastExtendedBenchmarkFiles: List<String>
        get() = _lastExtendedBenchmarkFiles

    actual val lastLatencyBenchmarkFile: String?
        get() = _lastLatencyBenchmarkFile

    actual val isLoaded: Boolean
        get() = _module != null

    actual val isClassifierLoaded: Boolean
        get() = _classifierModule != null

    /**
     * Loads the detector the user selected, replacing the loaded one if the
     * selection changed. Safe and cheap to call before every detection: it only
     * touches the filesystem when the model actually differs.
     */
    actual suspend fun loadModel() {
        val spec = DetectionModelCatalog.byId(settingsManager.getDetectionModel())
        if (_module != null && _loadedSpec?.id == spec.id) return

        _module?.destroy()
        _module = null
        _loadedSpec = null

        val modelPath = assetFilePath(context, spec.assetName)
        val file = File(modelPath)
        _module = Module.load(modelPath)
        _loadedSpec = spec
        val sizeMb = file.length() / (1024f * 1024f)
        Log.d("ObjectDetector", "ExecuTorch detection model '${spec.assetName}' (${spec.displayName}) loaded successfully. Path: ${file.absolutePath}, Size: ${"%.2f".format(sizeMb)} MB, Layout: ${spec.layout}, NMS: ${spec.applyNms}")
    }

    actual suspend fun loadClassifierModel() {
        if (_classifierModule == null) {
            val modelPath = assetFilePath(context, CLASSIFIER_ASSET)
            val file = File(modelPath)
            _classifierModule = Module.load(modelPath)
            val sizeMb = file.length() / (1024f * 1024f)
            Log.d("ObjectDetector", "ExecuTorch classifier model '$CLASSIFIER_ASSET' loaded successfully. Path: ${file.absolutePath}, Size: ${"%.2f".format(sizeMb)} MB, Classes: ${CROP_CLASSES.joinToString()}")
        }
    }

    // -------------------------------------------------------------------------------
    // Public inference entry points.
    //
    // Each is a one-line delegation to the stage-timed implementation further
    // down, which is the only copy of the algorithm. Neither does any crop
    // routing: the two-stage gating lives in commonMain's DetectionPipeline /
    // CropRoutingPolicy so both platforms and the benchmark share one decision.
    // -------------------------------------------------------------------------------

    actual fun classify(imageBytes: ByteArray): ClassificationResult? =
        classifyStageTimed(imageBytes).first

    actual fun detect(imageBytes: ByteArray): List<DetectionResult> =
        detectStageTimed(imageBytes).first

    actual fun orientedImageSize(imageBytes: ByteArray): OrientedImageSize? =
        ImageOrientation.orientedSize(imageBytes)

    // -------------------------------------------------------------------------------
    // Stage helpers extracted from classify()/detect(). Each does exactly what the
    // corresponding inline code used to do — nothing here changes behavior, it only
    // gives each step a name and a boundary a timer can be placed around.
    // -------------------------------------------------------------------------------

    /**
     * The one decode path for both models. Applies EXIF orientation so the
     * classifier and the detector always receive the same visually upright
     * image, and so Android matches what iOS's `UIImage.fixOrientation()` has
     * always done — see common/utils/ImageOrientation.kt and the C025 worklog
     * entry. Returns null only when the bytes cannot be decoded at all.
     */
    private fun decodeUprightOrNull(imageBytes: ByteArray): ImageOrientation.Decoded? =
        ImageOrientation.decodeUpright(imageBytes)

    private fun runClassifierInference(module: Module, floatArray: FloatArray): FloatArray? {
        val inputTensor = Tensor.fromBlob(floatArray, longArrayOf(1, 3, 260, 260))
        val outputTensors = module.forward(EValue.from(inputTensor))
        if (outputTensors == null || outputTensors.isEmpty()) return null
        val outputTensor = outputTensors[0].toTensor()
        val outputArray = outputTensor.getDataAsFloatArray()
        if (outputArray == null || outputArray.size < CROP_CLASSES.size) return null
        return outputArray
    }

    private fun postprocessClassification(outputArray: FloatArray): ClassificationResult {
        val probs = softmax(outputArray, CROP_CLASSES.size)
        var maxProb = -1f
        var maxIdx = -1
        for (i in probs.indices) {
            if (probs[i] > maxProb) {
                maxProb = probs[i]
                maxIdx = i
            }
        }
        // Two rejection mechanisms that fail differently: the learned "Other" class
        // catches the non-crop species it was trained on, the confidence floor still
        // catches confidently-wrong predictions on species it has never seen.
        val threshold = settingsManager.getClassifierThreshold()
        val label = if (maxIdx == OTHER_INDEX || maxProb < threshold) "unknown" else CROP_CLASSES[maxIdx]
        return ClassificationResult(
            label = label,
            confidence = maxProb,
            isAccepted = label != "unknown"
        )
    }

    private data class DetectorPreprocessResult(
        val bitmap: Bitmap,
        val scale: Float,
        val padLeft: Float,
        val padTop: Float,
    )

    private fun preprocessDetectorBitmap(bitmap: Bitmap, spec: DetectionModelSpec, size: Int): DetectorPreprocessResult {
        // YOLO was trained with letterboxing, so stretching to 640×640 distorts the
        // aspect ratio and costs detections. RT-DETR is exported the way Ultralytics
        // runs it — LetterBox(auto=false, scaleFill=true), i.e. a plain stretch — so
        // padding it would be the mismatch instead.
        return if (spec.letterbox) {
            val (letterboxed, meta) = letterboxBitmap(bitmap, size)
            DetectorPreprocessResult(letterboxed, meta[0], meta[1], meta[2])
        } else {
            DetectorPreprocessResult(bitmap.scale(size, size), 1f, 0f, 0f)
        }
    }

    /** Detector output plus the split between the forward pass and materializing
     *  a usable primitive array — the same boundary iOS reports, so the two
     *  platforms' detectorInference figures cover the same work. */
    private data class DetectorInferenceResult(
        val outputArray: FloatArray,
        val outputShape: LongArray,
        val forwardMs: Double,
        val outputTransferMs: Double,
    )

    private fun runDetectorInference(module: Module, floatArray: FloatArray, size: Int): DetectorInferenceResult? {
        val inputTensor = Tensor.fromBlob(floatArray, longArrayOf(1, 3, size.toLong(), size.toLong()))
        val t0 = System.nanoTime()
        val outputTensors = module.forward(EValue.from(inputTensor))
        val t1 = System.nanoTime()
        if (outputTensors == null || outputTensors.isEmpty()) return null
        val outputTensor = outputTensors[0].toTensor()
        // getDataAsFloatArray() is Android's materialization step: it is the
        // counterpart of iOS's tensor -> buffer copy and belongs in the same
        // bucket, so it is timed separately and folded into the same total.
        val outputArray = outputTensor.getDataAsFloatArray() ?: return null
        val outputShape = outputTensor.shape()
        val t2 = System.nanoTime()
        return DetectorInferenceResult(
            outputArray = outputArray,
            outputShape = outputShape,
            forwardMs = (t1 - t0) / 1_000_000.0,
            outputTransferMs = (t2 - t1) / 1_000_000.0,
        )
    }

    private fun decodeDetections(
        outputArray: FloatArray,
        outputShape: LongArray,
        spec: DetectionModelSpec,
        size: Int,
        scale: Float,
        padLeft: Float,
        padTop: Float,
        origWidth: Int,
        origHeight: Int,
        threshold: Float,
    ): List<DetectionResult> {
        val preliminaryDetections = mutableListOf<DetectionResult>()

        when (spec.layout) {
            // [1, 4 + numClasses, numPredictions] — attribute-major, so a given
            // prediction's fields are numPredictions apart.
            DetectionOutputLayout.YOLO_ATTRIBUTE_MAJOR -> {
                val numClasses = outputShape[1].toInt() - 4
                val numPredictions = outputShape[2].toInt()

                for (i in 0 until numPredictions) {
                    var maxScore = 0f
                    var classId = -1
                    for (j in 0 until numClasses) {
                        val score = outputArray[(j + 4) * numPredictions + i]
                        if (score > maxScore) {
                            maxScore = score
                            classId = j
                        }
                    }
                    if (maxScore > threshold && classId >= 0) {
                        preliminaryDetections.add(
                            toDetection(
                                classId, maxScore,
                                outputArray[0 * numPredictions + i],
                                outputArray[1 * numPredictions + i],
                                outputArray[2 * numPredictions + i],
                                outputArray[3 * numPredictions + i],
                                spec, size, scale, padLeft, padTop, origWidth, origHeight
                            )
                        )
                    }
                }
            }

            // [1, numQueries, 4 + numClasses] — query-major, one contiguous row per
            // query, and boxes normalized to 0..1.
            DetectionOutputLayout.DETR_QUERY_MAJOR -> {
                val numQueries = outputShape[1].toInt()
                val stride = outputShape[2].toInt()
                val numClasses = stride - 4

                for (i in 0 until numQueries) {
                    val row = i * stride
                    var maxScore = 0f
                    var classId = -1
                    for (j in 0 until numClasses) {
                        val score = outputArray[row + 4 + j]
                        if (score > maxScore) {
                            maxScore = score
                            classId = j
                        }
                    }
                    if (maxScore > threshold && classId >= 0) {
                        preliminaryDetections.add(
                            toDetection(
                                classId, maxScore,
                                outputArray[row + 0], outputArray[row + 1],
                                outputArray[row + 2], outputArray[row + 3],
                                spec, size, scale, padLeft, padTop, origWidth, origHeight
                            )
                        )
                    }
                }
            }
        }

        return preliminaryDetections
    }

    // -------------------------------------------------------------------------------
    // Quick developer-button benchmark (unchanged from the original instrumentation).
    // -------------------------------------------------------------------------------

    actual suspend fun runLatencyBenchmark(): List<BenchmarkResult> =
        withContext(Dispatchers.Default) { runLatencyBenchmarkOnCurrentThread() }

    private suspend fun runLatencyBenchmarkOnCurrentThread(): List<BenchmarkResult> {
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})"
        val results = mutableListOf<BenchmarkResult>()

        if (_classifierModule != null) {
            val imageBytes = syntheticJpegBytes(260, 260)
            val latencies = mutableListOf<Double>()
            repeat(BENCHMARK_WARMUP_RUNS) { classify(imageBytes) }
            repeat(BENCHMARK_MEASURED_RUNS) {
                val t0 = System.nanoTime()
                classify(imageBytes)
                val t1 = System.nanoTime()
                latencies.add((t1 - t0) / 1_000_000.0)
            }
            results.add(
                BenchmarkResult(
                    modelName = CLASSIFIER_ASSET,
                    stage = "classifier",
                    deviceInfo = deviceInfo,
                    warmupRuns = BENCHMARK_WARMUP_RUNS,
                    measuredRuns = BENCHMARK_MEASURED_RUNS,
                    stats = computeLatencyStats(latencies),
                    rawLatenciesMs = latencies
                )
            )
        }

        val spec = _loadedSpec
        if (_module != null && spec != null) {
            // Detection latency depends on the pre-resize image resolution too (decode +
            // letterbox/scale cost scales with it), so sweep resolutions a real camera
            // capture could plausibly hand in, rather than just the model's fixed input size.
            for ((w, h) in BENCHMARK_IMAGE_SIZES) {
                val imageBytes = syntheticJpegBytes(w, h)
                val latencies = mutableListOf<Double>()
                repeat(BENCHMARK_WARMUP_RUNS) { detect(imageBytes) }
                repeat(BENCHMARK_MEASURED_RUNS) {
                    val t0 = System.nanoTime()
                    detect(imageBytes)
                    val t1 = System.nanoTime()
                    latencies.add((t1 - t0) / 1_000_000.0)
                }
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

    // -------------------------------------------------------------------------------
    // Publication-protocol extended benchmark.
    //
    // Every timed call below goes through classify()/detect() or the same private
    // stage-helpers they call — this function adds System.nanoTime() timers around
    // existing calls, it does not reimplement or alter what they compute. Metrics
    // this platform/run cannot measure defensibly are still reported, with an
    // explicit caveat appended to `notes` — never silently omitted or invented.
    // -------------------------------------------------------------------------------

    actual suspend fun runExtendedBenchmark(
        warmupRuns: Int,
        measuredRuns: Int,
    ): BenchmarkExport = withContext(Dispatchers.Default) {
        // Runs on Dispatchers.Default, the SAME dispatcher DetectionViewModel uses
        // for real inference — so the numbers describe the thread production
        // actually runs on, and a caller on the UI thread (either platform's
        // Settings screen) cannot freeze the interface for the length of a
        // 100-run protocol. `measuredOnThread` in the export records which
        // thread it landed on.
        runExtendedBenchmarkOnCurrentThread(warmupRuns, measuredRuns)
    }

    private suspend fun runExtendedBenchmarkOnCurrentThread(
        warmupRuns: Int,
        measuredRuns: Int,
    ): BenchmarkExport {
        // Idempotent no-ops if already loaded — makes device/model metadata and cold
        // load timing meaningful even if this is called before anything else has run.
        loadClassifierModel()
        loadModel()

        val notes = mutableListOf<String>()
        notes.add(
            "Timing uses monotonic clocks (System.nanoTime()) around the same " +
                "classify()/detect() code paths used in production; no external " +
                "tool or automation-layer round-trip time is included in any " +
                "figure in this export."
        )

        // ---- Cold model-loading time ------------------------------------------------
        val classifierColdLoadMs = mutableListOf<Double>()
        val detectorColdLoadMs = mutableListOf<Double>()
        repeat(BENCHMARK_COLD_LOAD_RUNS) {
            release()
            val t0 = System.nanoTime()
            loadClassifierModel()
            val t1 = System.nanoTime()
            loadModel()
            val t2 = System.nanoTime()
            classifierColdLoadMs.add((t1 - t0) / 1_000_000.0)
            detectorColdLoadMs.add((t2 - t1) / 1_000_000.0)
        }
        val coldLoad = ColdLoadBenchmark(
            classifierColdLoad = if (classifierColdLoadMs.isNotEmpty()) computeLatencyStats(classifierColdLoadMs) else null,
            detectorColdLoad = if (detectorColdLoadMs.isNotEmpty()) computeLatencyStats(detectorColdLoadMs) else null,
            repetitions = BENCHMARK_COLD_LOAD_RUNS,
            note = "Each repetition calls release() first, so this is a genuine cold " +
                "load (module and spec state cleared), not the idempotent no-op " +
                "loadModel()/loadClassifierModel() take when the requested model " +
                "is already resident. release() is only invoked this way from the " +
                "benchmark path — normal app usage never forces a mid-session " +
                "cold reload. CAVEAT: these figures are sub-millisecond, which is " +
                "far too fast to have read a 30 MB / 9 MB .pte off storage — " +
                "ExecuTorch's Module.load() is mmap-backed and lazy, so this " +
                "measures only program-header setup. Method initialisation and the " +
                "page-in of the weights are deferred to the first forward() and " +
                "therefore land inside the first inference, not here. Do not cite " +
                "this as 'time until the model is ready to infer'; after the first " +
                "repetition the file is also warm in the page cache.",
        )

        // ---- Memory: opening bracket ------------------------------------------------
        // Taken here, before any measured loop runs, so the pair below genuinely
        // brackets the measured work. Taking both samples together at the end (as
        // an earlier revision did) made the delta identically zero and the
        // accompanying note false.
        val memorySamples = mutableListOf<MemorySample>()
        memorySamples.add(sampleMemory("before_measured_loop"))

        val spec = _loadedSpec
        val classifierModule = _classifierModule
        val detectorModule = _module

        // ---- Model artifacts (size + SHA-256) ---------------------------------------
        val modelArtifacts = mutableListOf<ModelArtifact>()
        try {
            val classifierPath = File(assetFilePath(context, CLASSIFIER_ASSET))
            modelArtifacts.add(
                ModelArtifact(
                    name = "classifier",
                    assetFileName = CLASSIFIER_ASSET,
                    sizeBytes = classifierPath.length(),
                    sha256 = sha256Of(classifierPath),
                )
            )
        } catch (e: Exception) {
            notes.add("Could not hash the classifier model file: ${e.message}")
        }
        if (spec != null) {
            try {
                val detectorPath = File(assetFilePath(context, spec.assetName))
                modelArtifacts.add(
                    ModelArtifact(
                        name = "detector (${spec.displayName})",
                        assetFileName = spec.assetName,
                        sizeBytes = detectorPath.length(),
                        sha256 = sha256Of(detectorPath),
                    )
                )
            } catch (e: Exception) {
                notes.add("Could not hash the detector model file: ${e.message}")
            }
        } else {
            notes.add("No detector model was loaded; the detector model artifact and all detector stage benchmarks are omitted (not zero-filled).")
        }

        // ---- Deterministic benchmark images + manifest ------------------------------
        val classifierImageBytes = syntheticJpegBytes(260, 260)
        val detectorImagesBySize = BENCHMARK_IMAGE_SIZES.associateWith { pair -> syntheticJpegBytes(pair.first, pair.second) }
        val imageManifest = mutableListOf<BenchmarkImageManifestEntry>()
        imageManifest.add(
            BenchmarkImageManifestEntry(
                id = "classifier_260x260",
                widthPx = 260,
                heightPx = 260,
                sizeBytes = classifierImageBytes.size.toLong(),
                sha256 = sha256Of(classifierImageBytes),
                source = "synthetic_striped_pattern",
            )
        )
        for ((w, h) in BENCHMARK_IMAGE_SIZES) {
            val bytes = detectorImagesBySize.getValue(w to h)
            imageManifest.add(
                BenchmarkImageManifestEntry(
                    id = "detector_${w}x${h}",
                    widthPx = w,
                    heightPx = h,
                    sizeBytes = bytes.size.toLong(),
                    sha256 = sha256Of(bytes),
                    source = "synthetic_striped_pattern",
                )
            )
        }
        notes.add(
            "All benchmark images are deterministic synthetic striped patterns " +
                "(syntheticJpegBytes), not photographs. Legitimate for latency-only " +
                "timing since ExecuTorch's compute cost is driven by tensor shape, " +
                "not pixel content — but NOT used for any accuracy or output-" +
                "agreement claim here. The cross-platform output-agreement " +
                "procedure uses real checksum-locked images instead."
        )

        // ---- Classifier stage benchmark ----------------------------------------------
        val classifierStage = if (classifierModule == null) {
            // Reporting a zero-filled StageBenchmark here would be indistinguishable
            // from an immeasurably fast classifier. Omit it and say why instead.
            notes.add(
                "Classifier model was not loaded; classifierStage is omitted entirely " +
                    "rather than zero-filled, and no end-to-end routing path could run."
            )
            null
        } else {
            repeat(warmupRuns) { classifyStageTimed(classifierImageBytes) }
            val classifierRuns = mutableListOf<StageLatencyMs>()
            repeat(measuredRuns) { classifierRuns.add(classifyStageTimed(classifierImageBytes).second) }
            if (classifierRuns.isEmpty()) {
                notes.add("measuredRuns was 0, so classifierStage is omitted rather than zero-filled.")
                null
            } else {
                computeStageBenchmark("classifier_260x260", warmupRuns, classifierRuns)
            }
        }

        // ---- Detector stage benchmark per resolution ---------------------------------
        val detectorStageBySize = mutableMapOf<String, com.nkwabyte.cropdiseasedetection.common.model.StageBenchmark>()
        if (detectorModule != null && spec != null) {
            for ((w, h) in BENCHMARK_IMAGE_SIZES) {
                val bytes = detectorImagesBySize.getValue(w to h)
                repeat(warmupRuns) { detectStageTimed(bytes) }
                val runs = mutableListOf<StageLatencyMs>()
                repeat(measuredRuns) { runs.add(detectStageTimed(bytes).second) }
                if (runs.isNotEmpty()) {
                    detectorStageBySize["${w}x${h}"] = computeStageBenchmark("detector_${w}x${h}", warmupRuns, runs)
                }
            }
        } else {
            notes.add("No detector model was loaded; detector stage benchmarks are omitted entirely.")
        }

        // ---- End-to-end benchmark: the CORRECTED production pipeline ----------------
        // DetectionPipeline is the same object DetectionViewModel.detect() drives,
        // so this measures the shipped decision sequence rather than an idealized
        // one: ensure classifier loaded -> decode + EXIF orientation -> classifier
        // preprocess/inference/postprocess -> routing decision -> (only if routed)
        // ensure detector loaded -> detector stages -> exact class-id filtering.
        val pipeline = DetectionPipeline(ObjectDetectorEngine(this))
        val (e2eWidth, e2eHeight) = BENCHMARK_IMAGE_SIZES.first()
        val e2eImageBytes = detectorImagesBySize.getValue(e2eWidth to e2eHeight)
        val e2eImageId = "detector_${e2eWidth}x${e2eHeight}"

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
            imageBytes: ByteArray,
            imageId: String,
            selectedCropId: String?,
        ): EndToEndBenchmark {
            repeat(warmupRuns) { pipeline.run(imageBytes, selectedCropId) }
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
                    val t0 = System.nanoTime()
                    val result = pipeline.run(imageBytes, selectedCropId, instrumented = true)
                    val t1 = System.nanoTime()
                    latencies.add((t1 - t0) / 1_000_000.0)
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
                    if (failureMessages.size < 20) {
                        failureMessages.add(e.message ?: e.javaClass.simpleName)
                    }
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
        // Synthetic striped images are rejected by the classifier, so they can only
        // ever exercise the out-of-distribution path. Accepted Corn/Pepper/Tomato
        // and a deliberate crop mismatch need real photographs, which ship as app
        // assets with their SHA-256 pinned in BenchmarkImageSet. Each fixture is
        // checksum-verified here before it is measured, so a swapped or truncated
        // asset fails loudly instead of silently changing what was measured.
        val endToEndByPath = mutableMapOf<String, EndToEndBenchmark>()
        val fixtures = mutableMapOf<String, ByteArray>()
        for (image in BenchmarkImageSet.all) {
            val bytes = loadBenchmarkImage(image)
            if (bytes == null) {
                notes.add("Benchmark fixture '${image.assetName}' is missing; every path that uses it is omitted, not zero-filled.")
                continue
            }
            val actual = sha256Of(bytes)
            if (actual != image.sha256) {
                notes.add(
                    "Benchmark fixture '${image.assetName}' FAILED its checksum (expected ${image.sha256}, " +
                        "got $actual); it was NOT used and every path that depends on it is omitted."
                )
                continue
            }
            fixtures[image.id] = bytes
            imageManifest.add(
                BenchmarkImageManifestEntry(
                    id = image.id,
                    widthPx = orientedImageSize(bytes)?.widthPx ?: -1,
                    heightPx = orientedImageSize(bytes)?.heightPx ?: -1,
                    sizeBytes = bytes.size.toLong(),
                    sha256 = actual,
                    source = "locked_real_photograph: ${image.groundTruthNote}",
                )
            )
        }

        // Accepted paths: one per crop, each with its own image and its own
        // canonical crop id. Reported separately — different images through
        // different detector class ranges must never be pooled.
        for ((image, pathId) in listOf(
            BenchmarkImageSet.CORN to BenchmarkPath.ACCEPTED_CORN,
            BenchmarkImageSet.PEPPER to BenchmarkPath.ACCEPTED_PEPPER,
            BenchmarkImageSet.TOMATO to BenchmarkPath.ACCEPTED_TOMATO,
        )) {
            val bytes = fixtures[image.id] ?: continue
            endToEndByPath[pathId] = measurePath(
                pathId = pathId,
                imageBytes = bytes,
                imageId = image.id,
                selectedCropId = image.expectedCrop?.canonicalLabel,
            )
        }

        // Out-of-distribution: a cassava leaf, the near-miss case the two-stage
        // design exists to reject. The detector must never run here.
        fixtures[BenchmarkImageSet.OUT_OF_DISTRIBUTION.id]?.let { bytes ->
            endToEndByPath[BenchmarkPath.REJECTED] = measurePath(
                pathId = BenchmarkPath.REJECTED,
                imageBytes = bytes,
                imageId = BenchmarkImageSet.OUT_OF_DISTRIBUTION.id,
                selectedCropId = null,
            )
        }

        // Deliberate selected-crop mismatch: a Corn photograph with Tomato
        // selected. The detector must never run here either.
        fixtures[BenchmarkImageSet.CORN.id]?.let { bytes ->
            endToEndByPath[BenchmarkPath.MISMATCH] = measurePath(
                pathId = BenchmarkPath.MISMATCH,
                imageBytes = bytes,
                imageId = "${BenchmarkImageSet.CORN.id}_selected_tomato",
                selectedCropId = SupportedCrop.TOMATO.canonicalLabel,
            )
        }

        notes.add(
            "End-to-end paths are measured against FIXED, checksum-locked real " +
                "photographs (see imageManifest entries whose source begins " +
                "'locked_real_photograph'), not synthetic patterns: accepted Corn, " +
                "accepted Pepper and accepted Tomato each use their own image and " +
                "their own canonical crop id; the out-of-distribution path uses a " +
                "cassava leaf; the mismatch path uses the Corn image with Tomato " +
                "selected. A fixture failing its checksum is skipped and its paths " +
                "omitted rather than measured against unknown bytes."
        )

        // The v2 `endToEnd` field is retained for existing readers. It carries
        // whichever path this run could measure, and `path` says which one — it is
        // never a blend of paths with and without detector inference.
        val endToEnd = endToEndByPath[BenchmarkPath.ACCEPTED_CORN]
            ?: endToEndByPath[BenchmarkPath.ACCEPTED]
            ?: endToEndByPath[BenchmarkPath.REJECTED]
            ?: endToEndByPath.values.firstOrNull()
            ?: EndToEndBenchmark(
                observedStageOrder = correctedStageOrder,
                representativeImageId = e2eImageId,
                stats = computeLatencyStats(listOf(0.0)),
                offlineSuccessCount = 0,
                offlineFailureCount = measuredRuns,
                failureMessages = listOf("No routing path was measurable in this run"),
                warmupRuns = warmupRuns,
                measuredRuns = 0,
                path = "unmeasured",
                detectorExecutedCount = 0,
                detectorSkippedCount = 0,
            )

        notes.add(
            "endToEnd/endToEndByPath measure DetectionPipeline.run(), the same entry " +
                "point DetectionViewModel.detect() calls in the app. The classifier " +
                "runs first and gates the detector; a rejected or crop-mismatched " +
                "image never reaches detector inference, which is verifiable in this " +
                "export as detectorExecutedCount == 0 for those paths."
        )
        notes.add(
            "Latency is NOT comparable across entries of endToEndByPath: only the " +
                "accepted path includes detector inference. Do not average them " +
                "together or quote one as \"the\" end-to-end figure without its path."
        )
        notes.add(
            "endToEnd.stats and the classifierStage/detectorStageBySize per-stage " +
                "breakdowns come from separate measured loops (both calling the " +
                "identical stage-timed functions classify()/detect() delegate to in " +
                "production), so their absolute numbers can differ by normal run-to-" +
                "run jitter — do not expect the per-stage means to sum exactly to the " +
                "end-to-end mean."
        )
        notes.add(
            "\"Offline\" here means no network call occurs anywhere in classify()/" +
                "detect()/routing — all run entirely against on-device ExecuTorch " +
                "modules. offlineFailureCount counts classifier/detector error " +
                "outcomes and thrown exceptions during the timed call, not prediction " +
                "accuracy or correctness."
        )

        // ---- CPU utilization (single-thread proxy) -----------------------------------
        val cpuUtilization = try {
            val threadT0 = Debug.threadCpuTimeNanos()
            val wallT0 = System.nanoTime()
            // Same corrected pipeline as the end-to-end loop, so the CPU proxy
            // describes the work the app actually does.
            repeat(measuredRuns) { pipeline.run(fixtures[BenchmarkImageSet.CORN.id] ?: e2eImageBytes, null) }
            val threadT1 = Debug.threadCpuTimeNanos()
            val wallT1 = System.nanoTime()
            if (threadT0 < 0 || threadT1 < 0) {
                CpuUtilization(
                    threadCpuTimeMs = 0.0,
                    wallClockMs = (wallT1 - wallT0) / 1_000_000.0,
                    utilizationRatio = 0.0,
                    measuredOnThread = Thread.currentThread().name,
                    available = false,
                    note = "Debug.threadCpuTimeNanos() returned a negative value on this device/run; per-thread CPU time is not available here.",
                )
            } else {
                val threadMs = (threadT1 - threadT0) / 1_000_000.0
                val wallMs = (wallT1 - wallT0) / 1_000_000.0
                CpuUtilization(
                    threadCpuTimeMs = threadMs,
                    wallClockMs = wallMs,
                    utilizationRatio = if (wallMs > 0) threadMs / wallMs else 0.0,
                    measuredOnThread = Thread.currentThread().name,
                    available = true,
                    note = "Single-thread (the calling thread) CPU-time proxy via " +
                        "Debug.threadCpuTimeNanos(), not a whole-process CPU% — the " +
                        "ratio can exceed 1.0 under multi-threaded XNNPACK kernels, " +
                        "which is expected, not an error.",
                )
            }
        } catch (e: Exception) {
            CpuUtilization(
                threadCpuTimeMs = 0.0, wallClockMs = 0.0, utilizationRatio = 0.0,
                measuredOnThread = Thread.currentThread().name, available = false,
                note = "CPU utilization sampling threw: ${e.message}",
            )
        }

        // ---- Memory: closing bracket -------------------------------------------------
        memorySamples.add(sampleMemory("after_measured_loop"))
        notes.add(
            "Memory samples are PSS snapshots bracketing the whole measured phase: " +
                "'before_measured_loop' is taken after cold-load timing but before " +
                "any stage, per-path or CPU loop runs, and 'after_measured_loop' " +
                "after all of them complete. Neither is taken inside a timed call, " +
                "so sampling never perturbs the reported latency numbers — but for " +
                "the same reason they cannot capture a transient peak during a " +
                "single inference call, and the delta covers every measured loop " +
                "together rather than any one of them."
        )

        // ---- Device environment -------------------------------------------------------
        val deviceEnvironment = collectDeviceEnvironment()
        deviceEnvironment.thermalStatusNote?.let { notes.add(it) }
        deviceEnvironment.installedAppSizeNote?.let { notes.add(it) }
        if (deviceEnvironment.isEmulator) {
            notes.add(
                "This export was produced on an ANDROID EMULATOR " +
                    "(${deviceEnvironment.manufacturer} ${deviceEnvironment.model}), " +
                    "NOT a Samsung Galaxy A10 or any physical device. Do not cite " +
                    "any figure in this export as physical-device performance."
            )
        }

        val export = BenchmarkExport(
            generatedAtEpochMs = System.currentTimeMillis(),
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
    // detect(); the public functions are one-line delegations to them. Keeping a
    // single implementation is deliberate: the previous split let the production
    // path and the benchmark path drift, which is exactly how the detect-before-
    // classify defect survived in the benchmark's stage order for so long. The
    // added cost to production is a handful of System.nanoTime() calls per request.
    // -------------------------------------------------------------------------------

    /**
     * The single implementation of the classifier path: decode, EXIF
     * orientation correction, preprocess, inference, postprocess, each timed
     * with System.nanoTime(). `classify()` calls this and drops the timings;
     * the extended benchmark calls it and keeps them. There is no second copy
     * of this algorithm to drift from.
     */
    actual fun classifyStageTimed(imageBytes: ByteArray): Pair<ClassificationResult?, StageLatencyMs> {
        val tStart = System.nanoTime()
        val module = _classifierModule

        val tDecode0 = System.nanoTime()
        val decoded = decodeUprightOrNull(imageBytes)
        val tDecode1 = System.nanoTime()
        // decodeUpright times EXIF parsing + transformation internally and
        // reports it separately, so it is not double-counted in imageDecodeMs.
        val orientationMs = decoded?.orientationCorrectionMs ?: 0.0
        val decodeMs = ((tDecode1 - tDecode0) / 1_000_000.0 - orientationMs).coerceAtLeast(0.0)

        if (module == null || decoded == null) {
            val tEnd = System.nanoTime()
            return null to StageLatencyMs(
                imageDecodeMs = decodeMs,
                orientationCorrectionMs = orientationMs,
                preprocessMs = 0.0,
                classifierInferenceMs = 0.0,
                routingMs = 0.0,
                detectorInferenceMs = 0.0,
                postprocessNmsMs = 0.0,
                totalMs = (tEnd - tStart) / 1_000_000.0,
                detectorExecuted = false,
            )
        }
        val bitmap = decoded.bitmap

        val tPre0 = System.nanoTime()
        val resizedBitmap = bitmap.scale(260, 260)
        val floatArray = bitmapToFloat32Array(resizedBitmap, TORCHVISION_NORM_MEAN_RGB, TORCHVISION_NORM_STD_RGB)
        val tPre1 = System.nanoTime()

        val tInfer0 = System.nanoTime()
        val outputArray = runClassifierInference(module, floatArray)
        val tInfer1 = System.nanoTime()

        val result = outputArray?.let { postprocessClassification(it) }
        val tEnd = System.nanoTime()

        // Bitmap.scale() hands back the source when the dimensions already match,
        // so only the genuinely new bitmap is freed, and never the one still in use.
        if (resizedBitmap !== bitmap) resizedBitmap.recycle()
        if (!bitmap.isRecycled) bitmap.recycle()

        return result to StageLatencyMs(
            imageDecodeMs = decodeMs,
            orientationCorrectionMs = orientationMs,
            preprocessMs = (tPre1 - tPre0) / 1_000_000.0,
            classifierInferenceMs = (tInfer1 - tInfer0) / 1_000_000.0,
            routingMs = 0.0,
            detectorInferenceMs = 0.0,
            postprocessNmsMs = 0.0,
            totalMs = (tEnd - tStart) / 1_000_000.0,
            detectorExecuted = false,
        )
    }

    /**
     * The single implementation of the detector path, timed the same way as
     * [classifyStageTimed]. Emits detections over the full 23-class space in
     * the shared box coordinate space; crop routing is applied afterwards by
     * `CropRoutingPolicy`, never here.
     */
    actual fun detectStageTimed(imageBytes: ByteArray): Pair<List<DetectionResult>, StageLatencyMs> {
        val tStart = System.nanoTime()

        val tDecode0 = System.nanoTime()
        val decoded = decodeUprightOrNull(imageBytes)
        val tDecode1 = System.nanoTime()
        val orientationMs = decoded?.orientationCorrectionMs ?: 0.0
        val decodeMs = ((tDecode1 - tDecode0) / 1_000_000.0 - orientationMs).coerceAtLeast(0.0)

        val module = _module
        val spec = _loadedSpec
        if (decoded == null || module == null || spec == null) {
            decoded?.bitmap?.takeIf { !it.isRecycled }?.recycle()
            val tEnd = System.nanoTime()
            return emptyList<DetectionResult>() to StageLatencyMs(
                imageDecodeMs = decodeMs,
                orientationCorrectionMs = orientationMs,
                preprocessMs = 0.0,
                classifierInferenceMs = 0.0,
                routingMs = 0.0,
                detectorInferenceMs = 0.0,
                postprocessNmsMs = 0.0,
                totalMs = (tEnd - tStart) / 1_000_000.0,
                detectorExecuted = true,
            )
        }

        val bitmap = decoded.bitmap
        // These are the UPRIGHT dimensions — after EXIF correction — which is the
        // coordinate space decodeDetections() un-letterboxes back into, so boxes
        // land on the same pixels the overlay renderer draws.
        val origWidth = bitmap.width
        val origHeight = bitmap.height
        val size = spec.inputSize

        val tPre0 = System.nanoTime()
        val pre = preprocessDetectorBitmap(bitmap, spec, size)
        val floatArray = bitmapToFloat32Array(pre.bitmap, floatArrayOf(0f, 0f, 0f), floatArrayOf(1f, 1f, 1f))
        if (pre.bitmap !== bitmap) pre.bitmap.recycle()
        val tPre1 = System.nanoTime()

        val inferenceOutput = runDetectorInference(module, floatArray, size)

        if (inferenceOutput == null) {
            if (!bitmap.isRecycled) bitmap.recycle()
            val tEnd = System.nanoTime()
            return emptyList<DetectionResult>() to StageLatencyMs(
                imageDecodeMs = decodeMs,
                orientationCorrectionMs = orientationMs,
                preprocessMs = (tPre1 - tPre0) / 1_000_000.0,
                classifierInferenceMs = 0.0,
                routingMs = 0.0,
                detectorInferenceMs = 0.0,
                postprocessNmsMs = 0.0,
                totalMs = (tEnd - tStart) / 1_000_000.0,
                detectorExecuted = true,
            )
        }
        val outputArray = inferenceOutput.outputArray
        val outputShape = inferenceOutput.outputShape
        val threshold = settingsManager.getDetectionThreshold()

        val tPost0 = System.nanoTime()
        val preliminaryDetections = decodeDetections(
            outputArray, outputShape, spec, size, pre.scale, pre.padLeft, pre.padTop, origWidth, origHeight, threshold
        )
        // RT-DETR's query head already emits one box per object; running NMS over it
        // would merge distinct detections that legitimately overlap.
        val finalDetections = if (spec.applyNms) {
            nonMaxSuppression(preliminaryDetections, settingsManager.getIouThreshold())
        } else {
            preliminaryDetections
        }
        val tPost1 = System.nanoTime()
        val tEnd = System.nanoTime()

        if (!bitmap.isRecycled) bitmap.recycle()

        return finalDetections to StageLatencyMs(
            imageDecodeMs = decodeMs,
            orientationCorrectionMs = orientationMs,
            preprocessMs = (tPre1 - tPre0) / 1_000_000.0,
            classifierInferenceMs = 0.0,
            routingMs = 0.0,
            // Forward pass PLUS materializing a usable primitive array — the same
            // boundary iOS reports, so the two stay comparable.
            detectorInferenceMs = inferenceOutput.forwardMs + inferenceOutput.outputTransferMs,
            postprocessNmsMs = (tPost1 - tPost0) / 1_000_000.0,
            totalMs = (tEnd - tStart) / 1_000_000.0,
            detectorExecuted = true,
            outputTransferMs = inferenceOutput.outputTransferMs,
        )
    }

    // -------------------------------------------------------------------------------
    // Resource + provenance collection helpers for the extended benchmark.
    // -------------------------------------------------------------------------------

    private fun sampleMemory(label: String): MemorySample {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val pid = Process.myPid()
            val memInfoArray = am?.getProcessMemoryInfo(intArrayOf(pid))
            val pss = memInfoArray?.firstOrNull()?.totalPss ?: 0
            val nativeHeapKb = Debug.getNativeHeapAllocatedSize() / 1024
            val runtime = Runtime.getRuntime()
            val javaHeapKb = (runtime.totalMemory() - runtime.freeMemory()) / 1024
            MemorySample(
                label = label,
                totalPssKb = pss,
                nativeHeapAllocatedKb = nativeHeapKb,
                javaHeapAllocatedKb = javaHeapKb,
                timestampEpochMs = System.currentTimeMillis(),
                available = true,
            )
        } catch (e: Exception) {
            MemorySample(
                label = label,
                totalPssKb = 0,
                nativeHeapAllocatedKb = 0,
                javaHeapAllocatedKb = 0,
                timestampEpochMs = System.currentTimeMillis(),
                available = false,
                note = "Memory sampling threw: ${e.message}",
            )
        }
    }

    private fun collectDeviceEnvironment(): DeviceEnvironment {
        val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.PRODUCT.contains("sdk_gphone") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))

        val totalRamBytes = try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(memInfo)
            memInfo.totalMem
        } catch (e: Exception) {
            null
        }

        var batteryLevelPercent: Int? = null
        var isCharging: Boolean? = null
        try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                batteryLevelPercent = (level * 100) / scale
            }
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            // Leave both null — battery state genuinely unavailable on this device/run.
        }

        var thermalStatus: String?
        var thermalStatusNote: String?
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                thermalStatus = when (pm.currentThermalStatus) {
                    PowerManager.THERMAL_STATUS_NONE -> "none"
                    PowerManager.THERMAL_STATUS_LIGHT -> "light"
                    PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
                    PowerManager.THERMAL_STATUS_SEVERE -> "severe"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
                    else -> "unknown_value_${pm.currentThermalStatus}"
                }
                thermalStatusNote = if (isEmulator) {
                    "thermalStatus is reported by PowerManager, but this run is on an emulator: there is no real thermal sensor behind it — treat it as not meaningful."
                } else {
                    null
                }
            } catch (e: Exception) {
                thermalStatus = null
                thermalStatusNote = "PowerManager.getCurrentThermalStatus() threw: ${e.message}"
            }
        } else {
            thermalStatus = null
            thermalStatusNote = "thermalStatus unsupported_below_api_29: PowerManager.getCurrentThermalStatus() requires API 29+; this device/build reports SDK ${Build.VERSION.SDK_INT} (app minSdk is 24)."
        }

        val buildIdentifier = BuildKonfig.BUILD_GIT_SHA

        var appVersionName = "unknown"
        var appVersionCode = -1L
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            appVersionName = packageInfo.versionName ?: "unknown"
            appVersionCode = if (Build.VERSION.SDK_INT >= 28) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            // Leave defaults — package info genuinely unavailable.
        }

        var installedAppSizeBytes: Long? = null
        var installedAppSizeNote: String
        try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
            val apkFile = File(appInfo.sourceDir)
            installedAppSizeBytes = apkFile.length()
            installedAppSizeNote = "installedAppSizeBytes is the base APK file size " +
                "(ApplicationInfo.sourceDir), not true on-disk installed size: it " +
                "excludes unpacked odex/vdex/oat, any split APKs, and app-private " +
                "data. A StorageStatsManager-based figure (API 26+, requires " +
                "PACKAGE_USAGE_STATS or being the installer of record) would be " +
                "more complete but is not used here."
        } catch (e: Exception) {
            installedAppSizeNote = "Could not determine installed app size: ${e.message}"
        }

        return DeviceEnvironment(
            platform = "android",
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = Build.VERSION.RELEASE,
            apiLevelOrOsBuild = "SDK ${Build.VERSION.SDK_INT}",
            abis = Build.SUPPORTED_ABIS?.toList() ?: emptyList(),
            isEmulator = isEmulator,
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

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256Of(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun writeExtendedBenchmarkExport(export: BenchmarkExport) {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val timestamp = System.currentTimeMillis()
        // Deliberately a different filename pattern ("extended_benchmark_*") from the
        // quick button's "benchmark_*.csv" so neither overwrites or is confused with
        // the other, and any previously captured evidence is left untouched.
        val jsonFile = File(dir, "extended_benchmark_$timestamp.json")
        val csvFile = File(dir, "extended_benchmark_$timestamp.csv")
        jsonFile.writeText(formatBenchmarkExportJson(export))
        csvFile.writeText(formatBenchmarkExportCsv(export))
        _lastExtendedBenchmarkFiles = listOf(jsonFile.absolutePath, csvFile.absolutePath)
        Log.d(
            "ObjectDetector",
            "Extended benchmark complete - wrote ${jsonFile.absolutePath} and ${csvFile.absolutePath} " +
                "(device=${export.deviceEnvironment.manufacturer} ${export.deviceEnvironment.model}, " +
                "isEmulator=${export.deviceEnvironment.isEmulator}, endToEnd mean=" +
                "${"%.2f".format(export.endToEnd.stats.meanMs)}ms p95=${"%.2f".format(export.endToEnd.stats.p95Ms)}ms)"
        )
        for (note in export.notes) {
            Log.d("ObjectDetector", "  NOTE: $note")
        }
    }

    /**
     * Builds a deterministic synthetic JPEG (a striped pattern, not a photo of anything
     * real) at the given resolution. Legitimate for *latency-only* benchmarking, since
     * ExecuTorch's compute cost is driven by tensor shape, not pixel content — this is
     * not used anywhere accuracy is being measured.
     */
    private fun syntheticJpegBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint()
        val stripe = maxOf(1, width / 32)
        var x = 0
        var stripeIndex = 0
        while (x < width) {
            paint.color = if (stripeIndex % 2 == 0) {
                android.graphics.Color.rgb(60, 140, 60)
            } else {
                android.graphics.Color.rgb(160, 200, 160)
            }
            canvas.drawRect(x.toFloat(), 0f, (x + stripe).toFloat(), height.toFloat(), paint)
            x += stripe
            stripeIndex++
        }
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    private fun writeBenchmarkCsv(results: List<BenchmarkResult>) {
        val csv = formatBenchmarkCsv(results)
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        val file = File(dir, "benchmark_" + System.currentTimeMillis() + ".csv")
        file.writeText(csv)
        _lastLatencyBenchmarkFile = file.absolutePath
        Log.d("ObjectDetector", "Latency benchmark complete - wrote " + results.size + " result rows to " + file.absolutePath)
        for (r in results) {
            Log.d(
                "ObjectDetector",
                "  " + r.stage + "/" + r.modelName + ": mean=" + "%.2f".format(r.stats.meanMs) + "ms p50=" +
                    "%.2f".format(r.stats.p50Ms) + "ms p95=" + "%.2f".format(r.stats.p95Ms) + "ms (n=" + r.stats.n + ")"
            )
        }
    }

    /**
     * Converts one raw cxcywh prediction into a [DetectionResult] in stretched-640×640
     * space, which is what `drawBoundingBoxesOnBitmap(modelWidth=640, modelHeight=640)`
     * expects. Letterboxed models are un-padded back to original pixel coords first;
     * normalized boxes from a stretched input already sit in that space once scaled up.
     */
    private fun toDetection(
        classId: Int, score: Float,
        cx: Float, cy: Float, w: Float, h: Float,
        spec: DetectionModelSpec, size: Int,
        scale: Float, padLeft: Float, padTop: Float,
        origWidth: Int, origHeight: Int,
    ): DetectionResult {
        // The canvas convention the drawing code works in, independent of any
        // model's input resolution.
        val canvas = DRAW_SPACE
        val f = if (spec.normalizedBoxes) size.toFloat() else 1f
        val x1Raw = cx * f - w * f / 2f
        val y1Raw = cy * f - h * f / 2f
        val x2Raw = cx * f + w * f / 2f
        val y2Raw = cy * f + h * f / 2f

        val x1: Float; val y1: Float; val x2: Float; val y2: Float
        if (spec.letterbox) {
            x1 = (x1Raw - padLeft) / scale / origWidth * canvas
            y1 = (y1Raw - padTop) / scale / origHeight * canvas
            x2 = (x2Raw - padLeft) / scale / origWidth * canvas
            y2 = (y2Raw - padTop) / scale / origHeight * canvas
        } else {
            // A stretched input maps proportionally onto the original image, so the
            // box is already in canvas space once scaled off the input resolution.
            x1 = x1Raw / size * canvas
            y1 = y1Raw / size * canvas
            x2 = x2Raw / size * canvas
            y2 = y2Raw / size * canvas
        }

        return DetectionResult(
            classIndex = classId,
            score = score,
            box = floatArrayOf(
                x1.coerceIn(0f, canvas), y1.coerceIn(0f, canvas),
                x2.coerceIn(0f, canvas), y2.coerceIn(0f, canvas)
            ),
            className = AppConstants.CLASS_LABELS.getOrElse(classId) { "Unknown" }
        )
    }

    private fun letterboxBitmap(source: Bitmap, targetSize: Int): Pair<Bitmap, FloatArray> {
        val origW = source.width.toFloat()
        val origH = source.height.toFloat()
        val scale = minOf(targetSize / origW, targetSize / origH)
        val scaledW = (origW * scale).roundToInt()
        val scaledH = (origH * scale).roundToInt()
        val padLeft = (targetSize - scaledW) / 2f
        val padTop  = (targetSize - scaledH) / 2f

        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114))
        val scaledBitmap = source.scale(scaledW, scaledH)
        canvas.drawBitmap(scaledBitmap, padLeft, padTop, null)
        scaledBitmap.recycle()

        return Pair(result, floatArrayOf(scale, padLeft, padTop))
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        // Assets are unpacked into filesDir once, and filesDir survives app updates —
        // so a newly shipped model was invisible to anyone who had already run the
        // app. Compare the cached copy against the asset and re-unpack when they
        // differ. `.pte` is in noCompress (build.gradle.kts) so the descriptor
        // reports the real length; if it can't be read, keep whatever is cached
        // rather than rewriting 30 MB on every launch.
        val assetLength = try {
            context.assets.openFd(assetName).use { it.length }
        } catch (e: Exception) {
            Log.w("ObjectDetector", "Could not measure asset '$assetName'; keeping the cached copy", e)
            -1L
        }
        if (file.exists() && file.length() > 0 && (assetLength < 0 || file.length() == assetLength)) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
        return file.absolutePath
    }

    actual fun loadBenchmarkImage(image: BenchmarkImage): ByteArray? = try {
        context.assets.open("${BenchmarkImageSet.ASSET_DIR}/${image.assetName}").use { it.readBytes() }
    } catch (e: Exception) {
        Log.w("ObjectDetector", "Benchmark fixture '${image.assetName}' could not be read", e)
        null
    }

    actual fun release() {
        _module?.destroy()
        _module = null
        _loadedSpec = null
        _classifierModule?.destroy()
        _classifierModule = null
    }
}

private fun nonMaxSuppression(
    detections: List<DetectionResult>,
    iouThreshold: Float = 0.1f
): List<DetectionResult> {
    val sortedDetections = detections.sortedByDescending { it.score }
    val finalDetections = mutableListOf<DetectionResult>()

    for (detection in sortedDetections) {
        var shouldAdd = true
        for (finalDetection in finalDetections) {
            if (detection.classIndex == finalDetection.classIndex) {
                val iou = calculateIoU(detection.box, finalDetection.box)
                if (iou > iouThreshold) {
                    shouldAdd = false
                    break
                }
            }
        }
        if (shouldAdd) {
            finalDetections.add(detection)
        }
    }

    return finalDetections
}

private fun calculateIoU(box1: FloatArray, box2: FloatArray): Float {
    val x1 = maxOf(box1[0], box2[0])
    val y1 = maxOf(box1[1], box2[1])
    val x2 = minOf(box1[2], box2[2])
    val y2 = minOf(box1[3], box2[3])

    val intersectionArea = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
    val box1Area = (box1[2] - box1[0]) * (box1[3] - box1[1])
    val box2Area = (box2[2] - box2[0]) * (box2[3] - box2[1])
    val unionArea = box1Area + box2Area - intersectionArea

    return if (unionArea > 0) intersectionArea / unionArea else 0f
}

/** Boxes are reported in this square space; drawBoundingBoxesOnBitmap is called
 *  with modelWidth = modelHeight = 640 and maps from it to the displayed image. */
/** Float view of the shared box coordinate space, so Android and iOS emit
 *  boxes in the same units. Defined once in commonMain. */
private val DRAW_SPACE = BOX_COORDINATE_SPACE.toFloat()

private val TORCHVISION_NORM_MEAN_RGB = floatArrayOf(0.485f, 0.456f, 0.406f)
private val TORCHVISION_NORM_STD_RGB = floatArrayOf(0.229f, 0.224f, 0.225f)

// The 4-class classifier with a learned "Other" class. It matches the 3-class
// model on crop accuracy (97.54%) while rejecting 98.7% of non-crop images
// against the old 40.8% at a 0.55 threshold — see the project's
// docs/10_classifier_ood_adoption.md. Rejection is by argmax, with the
// confidence floor kept on top of it.
// Resolutions a real camera capture could plausibly hand the detector; decode +
// letterbox/scale cost scales with input resolution, so detector latency is swept
// across these rather than measured at just the model's fixed input size.
private val BENCHMARK_IMAGE_SIZES = listOf(1920 to 1080, 1280 to 960, 640 to 480)

private const val CLASSIFIER_ASSET = "crop_classifier_ood.pte"
private val CROP_CLASSES = arrayOf("Corn", "Pepper", "Tomato", "Other")
private const val OTHER_INDEX = 3

private fun softmax(logits: FloatArray, count: Int): FloatArray {
    var maxLogit = logits[0]
    for (i in 1 until count) {
        if (logits[i] > maxLogit) maxLogit = logits[i]
    }
    val exps = FloatArray(count) { kotlin.math.exp(logits[it] - maxLogit) }
    val sum = exps.sum()
    return FloatArray(count) { exps[it] / sum }
}

private fun bitmapToFloat32Array(bitmap: android.graphics.Bitmap, mean: FloatArray, std: FloatArray): FloatArray {
    val width = bitmap.width
    val height = bitmap.height
    val floatArray = FloatArray(3 * width * height)
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val rOffset = 0
    val gOffset = width * height
    val bOffset = 2 * width * height

    for (i in 0 until width * height) {
        val pixel = pixels[i]
        val r = ((pixel shr 16) and 0xFF) / 255.0f
        val g = ((pixel shr 8) and 0xFF) / 255.0f
        val b = (pixel and 0xFF) / 255.0f

        floatArray[rOffset + i] = (r - mean[0]) / std[0]
        floatArray[gOffset + i] = (g - mean[1]) / std[1]
        floatArray[bOffset + i] = (b - mean[2]) / std[2]
    }
    return floatArray
}
