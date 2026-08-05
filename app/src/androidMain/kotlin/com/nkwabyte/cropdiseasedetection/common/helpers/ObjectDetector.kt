package com.nkwabyte.cropdiseasedetection.common.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelCatalog
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelSpec
import com.nkwabyte.cropdiseasedetection.common.model.DetectionOutputLayout
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

actual class ObjectDetector actual constructor() : KoinComponent {
    private val context: Context by inject()
    private val settingsManager: com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager by inject()
    private var _module: Module? = null
    private var _classifierModule: Module? = null

    /** The spec the currently loaded module was built from — detect() decodes
     *  against this, and a settings change is detected by comparing ids. */
    private var _loadedSpec: DetectionModelSpec? = null

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

    actual fun classify(imageBytes: ByteArray): ClassificationResult? {
        val module = _classifierModule ?: return null
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return null

        val resizedBitmap = bitmap.scale(260, 260)
        
        val floatArray = bitmapToFloat32Array(
            resizedBitmap,
            TORCHVISION_NORM_MEAN_RGB,
            TORCHVISION_NORM_STD_RGB
        )
        
        val inputTensor = Tensor.fromBlob(
            floatArray,
            longArrayOf(1, 3, 260, 260)
        )

        val outputTensors = module.forward(EValue.from(inputTensor))
        if (outputTensors == null || outputTensors.isEmpty()) {
            return null
        }
        
        val outputTensor = outputTensors[0].toTensor()
        val outputArray = outputTensor.getDataAsFloatArray()

        if (outputArray == null || outputArray.size < CROP_CLASSES.size) {
            return null
        }

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

    actual fun detect(imageBytes: ByteArray): List<DetectionResult> {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return emptyList()
        val module = _module ?: return emptyList()
        val spec = _loadedSpec ?: return emptyList()

        val origWidth = bitmap.width
        val origHeight = bitmap.height
        val size = spec.inputSize

        // YOLO was trained with letterboxing, so stretching to 640×640 distorts the
        // aspect ratio and costs detections. RT-DETR is exported the way Ultralytics
        // runs it — LetterBox(auto=false, scaleFill=true), i.e. a plain stretch — so
        // padding it would be the mismatch instead.
        val scale: Float
        val padLeft: Float
        val padTop: Float
        val inputBitmap: Bitmap
        if (spec.letterbox) {
            val (letterboxed, meta) = letterboxBitmap(bitmap, size)
            inputBitmap = letterboxed
            scale = meta[0]; padLeft = meta[1]; padTop = meta[2]
        } else {
            inputBitmap = bitmap.scale(size, size)
            scale = 1f; padLeft = 0f; padTop = 0f
        }

        val floatArray = bitmapToFloat32Array(
            inputBitmap,
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(1f, 1f, 1f)
        )
        // Bitmap.scale() can hand back the source itself when the dimensions already
        // match, so recycling unconditionally would free a bitmap we don't own.
        if (inputBitmap !== bitmap) inputBitmap.recycle()

        val inputTensor = Tensor.fromBlob(
            floatArray,
            longArrayOf(1, 3, size.toLong(), size.toLong())
        )

        val outputTensors = module.forward(EValue.from(inputTensor))
        if (outputTensors == null || outputTensors.isEmpty()) {
            return emptyList()
        }

        val outputTensor = outputTensors[0].toTensor()
        val outputArray = outputTensor.getDataAsFloatArray()
        val outputShape = outputTensor.shape()
        val threshold = settingsManager.getDetectionThreshold()

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

        // RT-DETR's query head already emits one box per object; running NMS over it
        // would merge distinct detections that legitimately overlap.
        return if (spec.applyNms) {
            nonMaxSuppression(preliminaryDetections, settingsManager.getIouThreshold())
        } else {
            preliminaryDetections
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
private const val DRAW_SPACE = 640f

private val TORCHVISION_NORM_MEAN_RGB = floatArrayOf(0.485f, 0.456f, 0.406f)
private val TORCHVISION_NORM_STD_RGB = floatArrayOf(0.229f, 0.224f, 0.225f)

// The 4-class classifier with a learned "Other" class. It matches the 3-class
// model on crop accuracy (97.54%) while rejecting 98.7% of non-crop images
// against the old 40.8% at a 0.55 threshold — see the project's
// docs/10_classifier_ood_adoption.md. Rejection is by argmax, with the
// confidence floor kept on top of it.
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
