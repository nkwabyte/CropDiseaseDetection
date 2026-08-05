package com.nkwabyte.cropdiseasedetection.common.model

/**
 * How a detector's raw output tensor is laid out, and therefore how it has to be
 * decoded. The two families the app ships differ in more than weights, so the
 * decode path is selected per model rather than assumed.
 */
enum class DetectionOutputLayout {
    /**
     * YOLO26 — `[1, 4 + numClasses, numPredictions]`, attribute-major. Boxes are
     * cxcywh in input-resolution pixels, scores are per-class with no objectness,
     * and overlapping boxes must be suppressed with NMS.
     */
    YOLO_ATTRIBUTE_MAJOR,

    /**
     * RT-DETR — `[1, numQueries, 4 + numClasses]`, query-major. Boxes are cxcywh
     * normalized to 0..1 and the transformer head emits one box per query, so the
     * model is NMS-free: suppressing overlaps here would delete real detections.
     */
    DETR_QUERY_MAJOR,
}

/**
 * Everything the inference code needs to run a detector, so that adding a model
 * is a data change rather than a new branch in both platforms' detect().
 *
 * @param id            persisted in settings; do not change once shipped
 * @param assetName     the `.pte` in androidMain/assets and the iOS bundle
 * @param inputSize     square input resolution the model was exported at
 * @param letterbox     true pads to preserve aspect ratio (YOLO's preprocessing),
 *                      false stretches to fill — RT-DETR is exported the way
 *                      Ultralytics runs it, `LetterBox(auto=false, scaleFill=true)`
 * @param normalizedBoxes  true when box coordinates come back in 0..1
 * @param applyNms      false for query-based heads that already emit unique boxes
 * @param available     false keeps a model visible in settings but unselectable,
 *                      for architectures that are trained but not converged
 */
data class DetectionModelSpec(
    val id: String,
    val displayName: String,
    val assetName: String,
    val inputSize: Int,
    val numClasses: Int,
    val layout: DetectionOutputLayout,
    val letterbox: Boolean,
    val normalizedBoxes: Boolean,
    val applyNms: Boolean,
    val available: Boolean = true,
) {
    /**
     * How the model reads in a picker. Every model is listed so the user can see what
     * the app is working towards, with the ones that have no shippable weights marked
     * rather than hidden.
     */
    val listLabel: String
        get() = if (available) displayName else "$displayName (Unavailable)"
}

/**
 * The detectors the app can load. Accuracy figures are mAP@0.5 on the Ghana Crop
 * Disease v2 test split — see the training project's
 * `outputs/benchmarks/comparison_table.md`.
 */
object DetectionModelCatalog {

    /** Deployment default: 9.3 MB for mAP 0.290. The capacity sweep found the
     *  dataset, not the model, to be the ceiling — accuracy per MB falls 7.7x
     *  going to yolo26m, so the nano variant stays the baseline. */
    val Yolo26 = DetectionModelSpec(
        id = "YOLO26",
        displayName = "YOLO26n",
        assetName = "crop_disease_yolo26.pte",
        inputSize = 640,
        numClasses = 23,
        layout = DetectionOutputLayout.YOLO_ATTRIBUTE_MAJOR,
        letterbox = true,
        normalizedBoxes = false,
        applyNms = true,
    )

    /** The accuracy option: mAP 0.345, but 128 MB and a transformer head. */
    val RtDetr = DetectionModelSpec(
        id = "RTDETR",
        displayName = "RT-DETR-L",
        assetName = "crop_disease_rtdetr.pte",
        inputSize = 640,
        numClasses = 23,
        layout = DetectionOutputLayout.DETR_QUERY_MAJOR,
        letterbox = false,
        normalizedBoxes = true,
        applyNms = false,
    )

    /** Trained but not converged — no checkpoint reached a usable mAP, so these
     *  are listed for transparency and cannot be selected. */
    val FasterRcnn = DetectionModelSpec(
        id = "FasterRCNN",
        displayName = "Faster R-CNN",
        assetName = "",
        inputSize = 640,
        numClasses = 23,
        layout = DetectionOutputLayout.YOLO_ATTRIBUTE_MAJOR,
        letterbox = true,
        normalizedBoxes = false,
        applyNms = true,
        available = false,
    )

    val VisionTransformer = DetectionModelSpec(
        id = "VisionTransformer",
        displayName = "Vision Transformer",
        assetName = "",
        inputSize = 640,
        numClasses = 23,
        layout = DetectionOutputLayout.YOLO_ATTRIBUTE_MAJOR,
        letterbox = true,
        normalizedBoxes = false,
        applyNms = true,
        available = false,
    )

    val default = Yolo26

    val all = listOf(Yolo26, RtDetr, FasterRcnn, VisionTransformer)

    val selectable = all.filter { it.available }

    /** Falls back to the baseline for an unknown id, and for one that is listed
     *  but not shippable — a stored preference must never leave the app with no
     *  detector loaded. */
    fun byId(id: String?): DetectionModelSpec =
        all.firstOrNull { it.id == id && it.available } ?: default
}
