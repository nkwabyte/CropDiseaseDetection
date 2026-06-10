package com.nkwabyte.cropdiseasedetection.common.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.ClassificationResult
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

    actual val isLoaded: Boolean
        get() = _module != null

    actual val isClassifierLoaded: Boolean
        get() = _classifierModule != null

    actual suspend fun loadModel() {
        if (_module == null) {
            val modelPath = assetFilePath(context, "crop_disease_yolo26.pte")
            val file = File(modelPath)
            _module = Module.load(modelPath)
            val sizeMb = file.length() / (1024f * 1024f)
            Log.d("ObjectDetector", "ExecuTorch detection model 'crop_disease_yolo26.pte' loaded successfully. Path: ${file.absolutePath}, Size: ${"%.2f".format(sizeMb)} MB")
        }
    }

    actual suspend fun loadClassifierModel() {
        if (_classifierModule == null) {
            val modelPath = assetFilePath(context, "crop_classifier.pte")
            val file = File(modelPath)
            _classifierModule = Module.load(modelPath)
            val sizeMb = file.length() / (1024f * 1024f)
            Log.d("ObjectDetector", "ExecuTorch classifier model 'crop_classifier.pte' loaded successfully. Path: ${file.absolutePath}, Size: ${"%.2f".format(sizeMb)} MB")
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
        
        if (outputArray == null || outputArray.size < 3) {
            return null
        }

        val maxLogit = maxOf(outputArray[0], maxOf(outputArray[1], outputArray[2]))
        val exp0 = kotlin.math.exp(outputArray[0] - maxLogit)
        val exp1 = kotlin.math.exp(outputArray[1] - maxLogit)
        val exp2 = kotlin.math.exp(outputArray[2] - maxLogit)
        val sum = exp0 + exp1 + exp2

        val prob0 = exp0 / sum
        val prob1 = exp1 / sum
        val prob2 = exp2 / sum

        val probs = floatArrayOf(prob0, prob1, prob2)
        val classes = arrayOf("Corn", "Pepper", "Tomato")

        var maxProb = -1f
        var maxIdx = -1
        for (i in probs.indices) {
            if (probs[i] > maxProb) {
                maxProb = probs[i]
                maxIdx = i
            }
        }

        val threshold = settingsManager.getClassifierThreshold()
        val label = if (maxProb >= threshold) classes[maxIdx] else "unknown"

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

        val origWidth = bitmap.width
        val origHeight = bitmap.height

        // Letterbox to 640×640 maintaining aspect ratio — matches ultralytics preprocessing.
        // Stretching (scale(640,640)) distorts the aspect ratio and causes the model to miss detections.
        val (letterboxedBitmap, meta) = letterboxBitmap(bitmap, 640)
        val scale = meta[0]
        val padLeft = meta[1]
        val padTop = meta[2]

        val floatArray = bitmapToFloat32Array(
            letterboxedBitmap,
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(1f, 1f, 1f)
        )
        letterboxedBitmap.recycle()

        val inputTensor = Tensor.fromBlob(
            floatArray,
            longArrayOf(1, 3, 640, 640)
        )

        val outputTensors = module.forward(EValue.from(inputTensor))
        if (outputTensors == null || outputTensors.isEmpty()) {
            return emptyList()
        }

        val outputTensor = outputTensors[0].toTensor()
        val outputArray = outputTensor.getDataAsFloatArray()
        val outputShape = outputTensor.shape()

        val numClasses = outputShape[1].toInt() - 4
        val numPredictions = outputShape[2].toInt()

        val preliminaryDetections = mutableListOf<DetectionResult>()

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

            if (maxScore > settingsManager.getDetectionThreshold()) {
                // Boxes from model are in letterboxed-640×640 space.
                // Un-letterbox to original pixel coords, then re-express in stretched-640×640
                // space so drawBoundingBoxesOnBitmap(modelWidth=640, modelHeight=640) maps correctly.
                val cx = outputArray[0 * numPredictions + i]
                val cy = outputArray[1 * numPredictions + i]
                val w  = outputArray[2 * numPredictions + i]
                val h  = outputArray[3 * numPredictions + i]

                val x1Lb = cx - w / 2f
                val y1Lb = cy - h / 2f
                val x2Lb = cx + w / 2f
                val y2Lb = cy + h / 2f

                val x1 = ((x1Lb - padLeft) / scale / origWidth  * 640f).coerceIn(0f, 640f)
                val y1 = ((y1Lb - padTop)  / scale / origHeight * 640f).coerceIn(0f, 640f)
                val x2 = ((x2Lb - padLeft) / scale / origWidth  * 640f).coerceIn(0f, 640f)
                val y2 = ((y2Lb - padTop)  / scale / origHeight * 640f).coerceIn(0f, 640f)

                preliminaryDetections.add(
                    DetectionResult(
                        classIndex = classId,
                        score = maxScore,
                        box = floatArrayOf(x1, y1, x2, y2),
                        className = AppConstants.CLASS_LABELS.getOrElse(classId) { "Unknown" }
                    )
                )
            }
        }

        return nonMaxSuppression(preliminaryDetections, settingsManager.getIouThreshold())
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
        if (file.exists() && file.length() > 0) return file.absolutePath
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

private val TORCHVISION_NORM_MEAN_RGB = floatArrayOf(0.485f, 0.456f, 0.406f)
private val TORCHVISION_NORM_STD_RGB = floatArrayOf(0.229f, 0.224f, 0.225f)

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
