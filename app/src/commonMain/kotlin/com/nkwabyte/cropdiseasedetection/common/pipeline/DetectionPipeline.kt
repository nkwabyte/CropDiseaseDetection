package com.nkwabyte.cropdiseasedetection.common.pipeline

import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.OrientedImageSize
import com.nkwabyte.cropdiseasedetection.common.model.StageLatencyMs
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.common.model.emptyStageLatency
import com.nkwabyte.cropdiseasedetection.common.model.mergeWithDetector
import kotlin.time.TimeSource

/** Box coordinate space both platforms emit detections in: a square canvas,
 *  independent of the image's own pixel size and of any model's input size.
 *  Android's `toDetection()` and the iOS bridge's `unletterboxBoxes` both map
 *  into it, and the overlay renderer scales out of it. */
const val BOX_COORDINATE_SPACE = 640

/**
 * What the pipeline did, at the granularity the UI and the research record
 * both need. Every value is a fact about a stage that actually ran.
 */
enum class PipelineOutcome {
    /** Routed, detector ran, at least one in-route detection survived filtering. */
    DETECTED,

    /** Routed, detector ran, nothing above threshold in the routed classes.
     *  A supported crop with no visible disease — NOT a rejection. */
    NO_DETECTIONS,

    /** Classifier rejected the image (learned `Other`, or below the floor). */
    REJECTED_OUT_OF_DISTRIBUTION,

    /** Classifier accepted a crop the user had not selected. */
    CROP_MISMATCH,

    /** Classifier could not produce a usable result; the detector never ran. */
    CLASSIFIER_ERROR,

    /** Routing succeeded but the detector threw or could not be loaded. */
    DETECTOR_ERROR,
}

/**
 * The single structured result of one production inference request.
 *
 * The same object is produced whether the caller is the app or the extended
 * benchmark, which is what keeps the benchmark measuring the shipped path.
 */
data class DetectionPipelineResult(
    val outcome: PipelineOutcome,
    val decision: RoutingDecision,
    val classification: ClassificationResult?,
    /** True only if the detector was actually invoked for this request. */
    val detectorExecuted: Boolean,
    /** Everything the detector returned, before class-id routing. Empty when
     *  the detector did not run. */
    val rawResults: List<DetectionResult>,
    /** [rawResults] restricted to the routed crop's class ids. */
    val routedResults: List<DetectionResult>,
    /** Authoritative post-orientation pixel dimensions of the analyzed image,
     *  or null when they could not be read. */
    val orientedImage: OrientedImageSize?,
    /** Edge length of the square space [rawResults]/[routedResults] boxes live in. */
    val boxCoordinateSpace: Int = BOX_COORDINATE_SPACE,
    /** Populated only when the caller asked for instrumentation. */
    val timing: StageLatencyMs? = null,
    /** Human-readable cause for the error outcomes; null otherwise. */
    val errorMessage: String? = null,
) {
    val routedCrop: SupportedCrop?
        get() = (decision as? RoutingDecision.Accepted)?.crop

    val isSuccessful: Boolean get() = outcome == PipelineOutcome.DETECTED
}

/**
 * The inference primitives the pipeline needs, behind an interface so the
 * routing logic can be tested in commonMain without a real ExecuTorch module.
 * Implemented for production by [ObjectDetectorEngine].
 */
interface InferenceEngine {
    suspend fun ensureClassifierLoaded()
    suspend fun ensureDetectorLoaded()

    /** Runs the classifier, returning its result plus per-stage timings. */
    fun classify(imageBytes: ByteArray): Pair<ClassificationResult?, StageLatencyMs>

    /** Runs the detector, returning raw detections plus per-stage timings. */
    fun detect(imageBytes: ByteArray): Pair<List<DetectionResult>, StageLatencyMs>

    /** Post-orientation dimensions of the image, without running any model. */
    fun orientedImageSize(imageBytes: ByteArray): OrientedImageSize?
}

/**
 * The production two-stage pipeline: classify, route, and only then detect.
 *
 * The order here is the architecture the manuscript's composed-pipeline claim
 * evaluated and that `docs/06_two_stage_pipeline.md` documents. Before this
 * class existed, `DetectionViewModel.detect()` ran the detector FIRST and used
 * the classifier afterwards as a display flag, so an out-of-distribution photo
 * still paid for — and could still show — a full detector pass.
 *
 * Nothing in here reimplements inference: it calls [InferenceEngine], whose
 * production implementation delegates to the same platform helpers the app has
 * always used. Instrumentation adds monotonic timestamps only.
 */
class DetectionPipeline(private val engine: InferenceEngine) {

    /**
     * Runs one request end to end.
     *
     * @param selectedCropId the user's crop as a canonical id, or null/blank for
     *        "no preference". Must never be a localized display string.
     * @param instrumented when true, the returned result carries [StageLatencyMs].
     */
    suspend fun run(
        imageBytes: ByteArray,
        selectedCropId: String?,
        instrumented: Boolean = false,
    ): DetectionPipelineResult {
        val started = TimeSource.Monotonic.markNow()
        val orientedImage = runCatching { engine.orientedImageSize(imageBytes) }.getOrNull()

        // ---- Stage 1: classifier (always first, always before the detector) ----
        val classifierOutcome = runCatching {
            engine.ensureClassifierLoaded()
            engine.classify(imageBytes)
        }
        if (classifierOutcome.isFailure) {
            val message = classifierOutcome.exceptionOrNull()?.message ?: "Classifier failed"
            return failClosed(
                decision = RoutingDecision.ClassifierUnavailable(message),
                outcome = PipelineOutcome.CLASSIFIER_ERROR,
                classification = null,
                orientedImage = orientedImage,
                errorMessage = message,
                timing = if (instrumented) emptyStageLatency(elapsedMs(started)) else null,
            )
        }
        val (classification, classifierTiming) = classifierOutcome.getOrThrow()

        // ---- Routing decision ----
        val routingMark = TimeSource.Monotonic.markNow()
        val decision = CropRoutingPolicy.decide(classification, selectedCropId)
        val routingMs = routingMark.elapsedNow().inWholeNanoseconds / 1_000_000.0

        fun timingWithoutDetector(): StageLatencyMs? = if (!instrumented) null else {
            classifierTiming.copy(
                routingMs = routingMs,
                detectorExecuted = false,
                totalMs = elapsedMs(started),
            )
        }

        when (decision) {
            is RoutingDecision.ClassifierUnavailable -> return failClosed(
                decision = decision,
                outcome = PipelineOutcome.CLASSIFIER_ERROR,
                classification = classification,
                orientedImage = orientedImage,
                errorMessage = decision.reason,
                timing = timingWithoutDetector(),
            )

            is RoutingDecision.Rejected -> return failClosed(
                decision = decision,
                outcome = PipelineOutcome.REJECTED_OUT_OF_DISTRIBUTION,
                classification = classification,
                orientedImage = orientedImage,
                errorMessage = null,
                timing = timingWithoutDetector(),
            )

            is RoutingDecision.SelectionMismatch -> return failClosed(
                decision = decision,
                outcome = PipelineOutcome.CROP_MISMATCH,
                classification = classification,
                orientedImage = orientedImage,
                errorMessage = null,
                timing = timingWithoutDetector(),
            )

            is RoutingDecision.Accepted -> Unit
        }

        // ---- Stage 2: detector, restricted to the routed crop's class ids ----
        val accepted = decision as RoutingDecision.Accepted
        val detectorOutcome = runCatching {
            engine.ensureDetectorLoaded()
            engine.detect(imageBytes)
        }
        if (detectorOutcome.isFailure) {
            val message = detectorOutcome.exceptionOrNull()?.message ?: "Detector failed"
            return DetectionPipelineResult(
                outcome = PipelineOutcome.DETECTOR_ERROR,
                decision = accepted,
                classification = classification,
                // The detector was invoked; it failed. Recorded truthfully so a
                // benchmark run cannot count this as a skipped detector.
                detectorExecuted = true,
                rawResults = emptyList(),
                routedResults = emptyList(),
                orientedImage = orientedImage,
                timing = timingWithoutDetector()?.copy(detectorExecuted = true),
                errorMessage = message,
            )
        }
        val (rawResults, detectorTiming) = detectorOutcome.getOrThrow()
        val routedResults = CropRoutingPolicy.filterToRoute(rawResults, accepted.allowedClassIds)

        return DetectionPipelineResult(
            outcome = if (routedResults.isEmpty()) {
                PipelineOutcome.NO_DETECTIONS
            } else {
                PipelineOutcome.DETECTED
            },
            decision = accepted,
            classification = classification,
            detectorExecuted = true,
            rawResults = rawResults,
            routedResults = routedResults,
            orientedImage = orientedImage,
            timing = if (!instrumented) null else {
                classifierTiming.mergeWithDetector(detectorTiming).copy(
                    routingMs = routingMs,
                    detectorExecuted = true,
                    totalMs = elapsedMs(started),
                )
            },
        )
    }

    private fun failClosed(
        decision: RoutingDecision,
        outcome: PipelineOutcome,
        classification: ClassificationResult?,
        orientedImage: OrientedImageSize?,
        errorMessage: String?,
        timing: StageLatencyMs?,
    ) = DetectionPipelineResult(
        outcome = outcome,
        decision = decision,
        classification = classification,
        detectorExecuted = false,
        rawResults = emptyList(),
        routedResults = emptyList(),
        orientedImage = orientedImage,
        timing = timing,
        errorMessage = errorMessage,
    )

    private fun elapsedMs(mark: TimeSource.Monotonic.ValueTimeMark): Double =
        mark.elapsedNow().inWholeNanoseconds / 1_000_000.0
}
