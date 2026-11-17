package com.nkwabyte.cropdiseasedetection.common

import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop

object AppConstants {
    /**
     * The detector's 23 classes, indexed exactly as the model emits them.
     *
     * Position IS the contract: index 0-4 are Corn, 5-14 Pepper, 15-22 Tomato,
     * and [SupportedCrop] routes on those indices. Reordering, inserting or
     * dropping an entry silently re-points every route, so the alignment is
     * asserted below at class-initialization time rather than trusted.
     */
    val CLASS_LABELS = listOf(
        "Corn Cercospora Leaf Spot",
        "Corn Common Rust",
        "Corn Healthy",
        "Corn Northern Leaf Blight",
        "Corn Streak",
        "Pepper Bacterial Spot",
        "Pepper Cercospora",
        "Pepper Early Blight",
        "Pepper Fusarium",
        "Pepper Healthy",
        "Pepper Late Blight",
        "Pepper Leaf Blight",
        "Pepper Leaf Curl",
        "Pepper Leaf Mosaic",
        "Pepper Septoria",
        "Tomato Bacterial Spot",
        "Tomato Early Blight",
        "Tomato Fusarium",
        "Tomato Healthy",
        "Tomato Late Blight",
        "Tomato Leaf Curl",
        "Tomato Mosaic",
        "Tomato Septoria",
        // add more classes as needed
    )

    init {
        // Fails fast and loudly if CLASS_LABELS and the crop class-id ranges ever
        // drift apart, rather than letting a mis-indexed label mis-route at
        // runtime. Also covered by SupportedCropTest, which catches it at build
        // time instead of launch time.
        SupportedCrop.validateClassIdCoverage(CLASS_LABELS)
    }
}