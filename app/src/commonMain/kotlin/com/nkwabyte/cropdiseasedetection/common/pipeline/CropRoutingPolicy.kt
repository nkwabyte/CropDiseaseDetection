package com.nkwabyte.cropdiseasedetection.common.pipeline

import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop

/**
 * Why the detector was or was not allowed to run for one request.
 *
 * Every non-[RoutingDecision.Accepted] value is a fail-closed outcome: the
 * detector is not loaded and not executed. They are kept distinct rather than
 * collapsed into one "no result" so the UI can say what actually happened —
 * an out-of-distribution photo, a crop the user did not select, and a
 * classifier that failed to produce a result are three different problems with
 * three different user actions.
 */
sealed interface RoutingDecision {

    /** The detector may run, restricted to [crop]'s class ids. */
    data class Accepted(
        val crop: SupportedCrop,
        val confidence: Float,
    ) : RoutingDecision {
        val allowedClassIds: Set<Int> get() = crop.classIds
    }

    /**
     * The classifier produced no usable result (model not loaded, decode
     * failure, malformed output tensor). Distinct from [Rejected]: nothing is
     * known about the image, so calling it "not a crop" would be a lie.
     */
    data class ClassifierUnavailable(val reason: String) : RoutingDecision

    /**
     * The classifier ran and rejected the image — argmax landed on the learned
     * `Other` class, or the top probability fell below the confidence floor.
     */
    data class Rejected(
        val label: String,
        val confidence: Float,
    ) : RoutingDecision

    /**
     * The classifier accepted a crop, but the user had explicitly selected a
     * different one. Fails closed: running the detector here would answer a
     * question the user did not ask, and filtering to the selected crop's ids
     * would report diseases of a crop that is not in the photo.
     */
    data class SelectionMismatch(
        val predicted: SupportedCrop,
        val selected: SupportedCrop,
        val confidence: Float,
    ) : RoutingDecision
}

/**
 * The shipped two-stage architecture's first stage, as a pure function.
 *
 * This mirrors `CropClassifier.predict()` + `CROP_TO_YOLO_CLASSES` in the ML
 * project (`docs/06_two_stage_pipeline.md` section 3): classify, reject or map
 * to a class-id allow-list, and only then let the detector run. Keeping it
 * pure and in commonMain is deliberate — before this existed the decision was
 * spread across `DetectionViewModel.detect()` (which ran the detector first and
 * used the classifier only as an after-the-fact flag) and a
 * `className.contains(selectedCrop)` substring filter that could not be tested
 * and did not survive translation.
 */
object CropRoutingPolicy {

    /**
     * Decides whether the detector runs, and against which class ids.
     *
     * @param classification the first stage's output, or null when the
     *        classifier could not be run at all.
     * @param selectedCropId the user's chosen crop as a CANONICAL id (see
     *        [SupportedCrop.fromCanonicalId]) — never a display string. Blank
     *        or null means "no preference", in which case the classifier's own
     *        crop is used as the route.
     */
    fun decide(
        classification: ClassificationResult?,
        selectedCropId: String?,
    ): RoutingDecision {
        if (classification == null) {
            return RoutingDecision.ClassifierUnavailable(
                "Classifier produced no result for this image"
            )
        }

        if (!classification.isAccepted) {
            return RoutingDecision.Rejected(
                label = classification.label,
                confidence = classification.confidence,
            )
        }

        // An "accepted" label that does not normalize to one of the three
        // supported crops means the classifier and this policy disagree about the
        // label set. Fail closed rather than guessing a route.
        val predicted = SupportedCrop.fromCanonicalId(classification.label)
            ?: return RoutingDecision.ClassifierUnavailable(
                "Classifier accepted an unmapped label \"${classification.label}\""
            )

        val selected = SupportedCrop.fromCanonicalId(selectedCropId)
        if (selected != null && selected != predicted) {
            return RoutingDecision.SelectionMismatch(
                predicted = predicted,
                selected = selected,
                confidence = classification.confidence,
            )
        }

        return RoutingDecision.Accepted(
            crop = predicted,
            confidence = classification.confidence,
        )
    }

    /**
     * Keeps only detections whose class index is in the routed crop's id set.
     *
     * Matching is on [DetectionResult.classIndex] — the integer the detector
     * emitted — so a class name that merely looks similar to another crop's
     * cannot slip through, and a renamed or translated label cannot change
     * which boxes survive.
     */
    fun filterToRoute(
        results: List<DetectionResult>,
        allowedClassIds: Set<Int>,
    ): List<DetectionResult> = results.filter { it.classIndex in allowedClassIds }
}
