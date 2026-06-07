package com.nkwabyte.cropdiseasedetection.common.helpers

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Module
import org.pytorch.executorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream

actual class ObjectDetector actual constructor() : KoinComponent {
    private val context: Context by inject()
    private var _module: Module? = null

    actual val isLoaded: Boolean
        get() = _module != null

    actual suspend fun loadModel() {
        if (_module == null) {
            val modelPath = assetFilePath(context, "crop_disease_yolo26.pte")
            _module = Module.load(modelPath)
        }
    }

    actual fun detect(imageBytes: ByteArray): List<DetectionResult> {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalStateException("Failed to decode image bytes")
            
        val module = _module ?: throw IllegalStateException("Model not loaded")

        val resizedBitmap = bitmap.scale(640, 640)
        
        val pTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )
        
        val inputTensor = Tensor.fromBlob(
            pTensor.dataAsFloatArray,
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

            if (maxScore > 0.25f) {
                val cx = outputArray[0 * numPredictions + i]
                val cy = outputArray[1 * numPredictions + i]
                val w = outputArray[2 * numPredictions + i]
                val h = outputArray[3 * numPredictions + i]

                val x1 = cx - w / 2f
                val y1 = cy - h / 2f
                val x2 = cx + w / 2f
                val y2 = cy + h / 2f

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

        return nonMaxSuppression(preliminaryDetections)
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
    }
}

private fun nonMaxSuppression(
    detections: List<DetectionResult>,
    iouThreshold: Float = 0.5f
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
