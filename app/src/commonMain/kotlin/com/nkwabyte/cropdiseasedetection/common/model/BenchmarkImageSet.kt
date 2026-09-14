package com.nkwabyte.cropdiseasedetection.common.model

/**
 * The fixed, checksum-locked photographs the extended benchmark runs against.
 *
 * These exist because the deterministic synthetic striped images the benchmark
 * used before are rejected by the classifier, so the ONLY routing path they can
 * exercise is out-of-distribution. Accepted Corn / Pepper / Tomato and the
 * deliberate selected-crop mismatch cannot be measured at all without real crop
 * photographs, and without those there is no accepted-path latency on any
 * device — which is precisely what claim C005 needs.
 *
 * Provenance: three images from the ML project's held-out detector test split
 * (`data/yolo/test/images`) and one non-crop leaf from its out-of-distribution
 * sample set (`data/ood_external_samples/cassava`). They are shipped as app
 * assets so BOTH platforms measure the identical bytes; the SHA-256 recorded
 * here is verified at run time and written into the export's image manifest, so
 * a silently swapped fixture cannot pass unnoticed.
 *
 * @param assetName file name under the platform's benchmark asset location.
 * @param expectedCrop the crop the classifier is expected to accept, or null
 *        for the out-of-distribution fixture. This is an EXPECTATION used to
 *        label the path, not an assertion — the benchmark records what the
 *        classifier actually decided.
 * @param sha256 the locked checksum of the shipped bytes.
 */
data class BenchmarkImage(
    val id: String,
    val assetName: String,
    val expectedCrop: SupportedCrop?,
    val sha256: String,
    val groundTruthNote: String,
)

object BenchmarkImageSet {

    val CORN = BenchmarkImage(
        id = "corn",
        assetName = "corn.jpg",
        expectedCrop = SupportedCrop.CORN,
        sha256 = "6b829037a1c1026dabee932f81253ea3277cceb4aa1d52eedad202ab67f6318d",
        groundTruthNote = "ML held-out test split; ground-truth detector class 0",
    )

    val PEPPER = BenchmarkImage(
        id = "pepper",
        assetName = "pepper.jpg",
        expectedCrop = SupportedCrop.PEPPER,
        sha256 = "b2dc772ab5a232e2e49a609f1afe264cb21d1678c00494300b4e437f5d4ec9ba",
        groundTruthNote = "ML held-out test split; ground-truth detector class 8",
    )

    val TOMATO = BenchmarkImage(
        id = "tomato",
        assetName = "tomato.jpg",
        expectedCrop = SupportedCrop.TOMATO,
        sha256 = "b5c7118b1dfa9dbc7ec8f11bb3f8be50f3236b7b243b248797e66624f71ac26d",
        groundTruthNote = "ML held-out test split; ground-truth detector class 16",
    )

    val OUT_OF_DISTRIBUTION = BenchmarkImage(
        id = "ood_cassava",
        assetName = "ood_cassava.jpg",
        expectedCrop = null,
        sha256 = "59b0bddc1f353936e1319f20e1281534116fbb99c28a45bdbcd9386f8af7156f",
        groundTruthNote = "ML OOD sample set; cassava leaf — a near-miss non-crop, not a random scene",
    )

    /** Every fixture, in the order the benchmark exercises them. */
    val all: List<BenchmarkImage> = listOf(CORN, PEPPER, TOMATO, OUT_OF_DISTRIBUTION)

    /** Directory the assets live under on each platform. */
    const val ASSET_DIR = "benchmark"
}
