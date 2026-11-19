package com.nkwabyte.cropdiseasedetection.common.pipeline

import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cross-platform output-agreement run (docs/mobile-benchmark-evidence/
 * phase3_cross_platform_output_agreement_procedure.md).
 *
 * Every locked input image is passed through the SAME production entry point
 * the app uses ([DetectionPipeline.run]), with no crop pre-selected, at the
 * thresholds currently stored in settings. Nothing is timed and nothing is
 * filtered here: the export records exactly what a user would be shown, so it
 * can be compared image by image with the desktop reference.
 */
@Serializable
data class AgreementDetection(
    val classIndex: Int,
    val className: String?,
    val score: Float,
    /** x1, y1, x2, y2 in the pipeline's square box space ([BOX_COORDINATE_SPACE]). */
    val box: List<Float>,
)

@Serializable
data class AgreementRecord(
    val imageFile: String,
    val imageSha256: String,
    val sizeBytes: Int,
    val orientedWidth: Int?,
    val orientedHeight: Int?,
    val outcome: String,
    val classifierLabel: String?,
    val classifierConfidence: Float?,
    val classifierAccepted: Boolean?,
    val routedCrop: String?,
    val detectorExecuted: Boolean,
    val detections: List<AgreementDetection>,
    val errorMessage: String?,
)

@Serializable
data class AgreementExport(
    val schema: String = "output_agreement_v1",
    val platform: String,
    val generatedAtEpochMs: Long,
    val environment: Map<String, String>,
    val boxCoordinateSpace: Int = BOX_COORDINATE_SPACE,
    val records: List<AgreementRecord>,
    val notes: List<String>,
)

private val agreementJson = Json { prettyPrint = true; encodeDefaults = true }

suspend fun runOutputAgreementPipeline(
    detector: ObjectDetector,
    inputs: List<Pair<String, ByteArray>>,
    sha256: (ByteArray) -> String,
    platform: String,
    environment: Map<String, String>,
    nowEpochMs: Long,
): String {
    detector.loadClassifierModel()
    detector.loadModel()
    val pipeline = DetectionPipeline(ObjectDetectorEngine(detector))
    val records = inputs.map { (name, bytes) ->
        val r = pipeline.run(bytes, selectedCropId = null)
        AgreementRecord(
            imageFile = name,
            imageSha256 = sha256(bytes),
            sizeBytes = bytes.size,
            orientedWidth = r.orientedImage?.widthPx,
            orientedHeight = r.orientedImage?.heightPx,
            outcome = r.outcome.name,
            classifierLabel = r.classification?.label,
            classifierConfidence = r.classification?.confidence,
            classifierAccepted = r.classification?.isAccepted,
            routedCrop = r.routedCrop?.canonicalLabel,
            detectorExecuted = r.detectorExecuted,
            detections = r.routedResults.map {
                AgreementDetection(it.classIndex, it.className, it.score, it.box.toList())
            },
            errorMessage = r.errorMessage,
        )
    }
    val export = AgreementExport(
        platform = platform,
        generatedAtEpochMs = nowEpochMs,
        environment = environment,
        records = records,
        notes = listOf(
            "Each image was run once through DetectionPipeline.run(imageBytes, selectedCropId = null), " +
                "the production entry point, so routing follows the classifier's own crop.",
            "detections are the routed (crop-restricted) results after the detector's confidence " +
                "threshold and NMS, i.e. what the app displays.",
            "imageSha256 is computed from the exact bytes handed to the pipeline.",
        ),
    )
    return agreementJson.encodeToString(export)
}
