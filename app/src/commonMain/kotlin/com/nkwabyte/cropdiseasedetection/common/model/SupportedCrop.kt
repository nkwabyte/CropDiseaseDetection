package com.nkwabyte.cropdiseasedetection.common.model

import com.nkwabyte.cropdiseasedetection.common.AppConstants

/**
 * The three crops the shipped detector was trained on, each paired with the
 * exact detector class-id range it owns.
 *
 * This is the mobile counterpart of the ML project's
 * `src/classifier/config.py::CROP_TO_YOLO_CLASSES`, and it is the ONLY place the
 * crop -> class-id mapping exists in the app. Routing is done on these integer
 * ids, never on class names, display labels or localized strings: the crop names
 * the user sees are translated (Corn is "Maïs" in French and "Aburo" in Twi), so
 * any name-based match silently stops working outside English.
 *
 * @param canonicalLabel the classifier's own output label — matches the
 *        `CROP_CLASSES` array in both platforms' ObjectDetector, and is what
 *        gets persisted/compared. Never show this directly; show a
 *        `stringResource` instead.
 * @param classIdRange   inclusive detector class ids owned by this crop.
 */
enum class SupportedCrop(
    val canonicalLabel: String,
    val classIdRange: IntRange,
) {
    CORN("Corn", 0..4),
    PEPPER("Pepper", 5..14),
    TOMATO("Tomato", 15..22);

    /** The allow-list a detection's `classIndex` is tested against. */
    val classIds: Set<Int> = classIdRange.toSet()

    companion object {
        /** Number of detector classes the 23-class label map must contain. */
        const val DETECTOR_CLASS_COUNT = 23

        /**
         * Resolves a canonical identifier to a crop. Accepts the classifier's
         * labels ("Corn") and this enum's own names ("CORN"), case-insensitively,
         * and nothing else — in particular it does NOT accept translated or
         * user-visible text, which is what keeps routing locale-independent.
         *
         * Returns null for "Other", "unknown", blank and every unrecognized value,
         * so an unmapped input fails closed at the call site rather than
         * defaulting to a crop.
         */
        fun fromCanonicalId(id: String?): SupportedCrop? {
            val trimmed = id?.trim().orEmpty()
            if (trimmed.isEmpty()) return null
            return entries.firstOrNull {
                it.canonicalLabel.equals(trimmed, ignoreCase = true) ||
                    it.name.equals(trimmed, ignoreCase = true)
            }
        }

        /** The crop owning a detector class id, or null if the id is out of range. */
        fun forClassId(classId: Int): SupportedCrop? =
            entries.firstOrNull { classId in it.classIdRange }

        /**
         * Fails fast if the 23-class label list and the crop ranges have drifted
         * apart. Called from [AppConstants]' initializer so a bad edit cannot
         * reach a device, and asserted directly by `SupportedCropTest`.
         */
        fun validateClassIdCoverage(labels: List<String>) {
            require(labels.size == DETECTOR_CLASS_COUNT) {
                "AppConstants.CLASS_LABELS must contain exactly $DETECTOR_CLASS_COUNT entries " +
                    "aligned with detector class indices, found ${labels.size}"
            }
            val covered = entries.flatMap { it.classIds }.toSet()
            require(covered == labels.indices.toSet()) {
                "SupportedCrop class-id ranges must cover exactly 0..${DETECTOR_CLASS_COUNT - 1} " +
                    "with no gaps or overlaps, got $covered"
            }
            // Every label's own prefix must agree with the range it falls in, so a
            // reordered label list is caught here rather than mis-routing at runtime.
            labels.forEachIndexed { index, label ->
                val owner = forClassId(index)
                require(owner != null && label.startsWith(owner.canonicalLabel)) {
                    "CLASS_LABELS[$index] = \"$label\" does not belong to the crop that owns " +
                        "class id $index (${forClassId(index)?.canonicalLabel})"
                }
            }
        }
    }
}
