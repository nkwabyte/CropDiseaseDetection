package com.nkwabyte.cropdiseasedetection.common.helpers

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale
import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult

class PyTorchObjectDetector(private val context: Context) {
    private var _module: Module? = null

    val isLoaded: Boolean
        get() = _module != null

    suspend fun loadModel() {
        if (_module == null) {
            val modelPath = assetFilePath(context, "best.torchscript")
            _module = Module.load(modelPath)
        }
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val module = _module ?: throw IllegalStateException("Model not loaded")

        // 1. Pre-process the image
        val resizedBitmap = bitmap.scale(640, 640)
        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
            resizedBitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
            TensorImageUtils.TORCHVISION_NORM_STD_RGB
        )

        // 2. Run the model
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
        val outputArray = outputTensor.dataAsFloatArray
        val outputShape = outputTensor.shape() // Should be [1, 27, 8400]

        val numClasses = outputShape[1].toInt() - 4 // 27 - 4 = 23 classes
        val numPredictions = outputShape[2].toInt() // 8400 predictions

        val preliminaryDetections = mutableListOf<DetectionResult>()

        // 3. Decode the output tensor
        // The output is "transposed": instead of [box1, box2, ...], it's [all_x, all_y, all_w, all_h, all_cls1_scores, ...]
        // We need to loop through each of the 8400 predictions and piece together the data for each box.
        for (i in 0 until numPredictions) {
            // Find the class with the highest score for this prediction
            var maxScore = 0f
            var classId = -1
            for (j in 0 until numClasses) {
                // Score for class 'j' and prediction 'i'
                // The offset is 4 because the first 4 rows are for box coordinates (cx, cy, w, h)
                val score = outputArray[(j + 4) * numPredictions + i]
                if (score > maxScore) {
                    maxScore = score
                    classId = j
                }
            }

            // Apply confidence threshold
            if (maxScore > 0.25f) {
                // Get bounding box coordinates for prediction 'i'
                val cx = outputArray[0 * numPredictions + i]
                val cy = outputArray[1 * numPredictions + i]
                val w = outputArray[2 * numPredictions + i]
                val h = outputArray[3 * numPredictions + i]

                // Convert from center-width-height to x1-y1-x2-y2 format
                val x1 = cx - w / 2f
                val y1 = cy - h / 2f
                val x2 = cx + w / 2f
                val y2 = cy + h / 2f

                preliminaryDetections.add(
                    DetectionResult(
                        classIndex = classId,
                        score = maxScore,
                        // Note: These box coordinates are for the 640x640 image
                        box = floatArrayOf(x1, y1, x2, y2),
                        className = AppConstants.CLASS_LABELS.getOrElse(classId) { "Unknown" }
                    )
                )
            }
        }

        // 4. Apply Non-Max Suppression (NMS) to remove redundant boxes
        return nonMaxSuppression(preliminaryDetections)
    }

//    fun detect(bitmap: Bitmap): List<DetectionResult> {
//        val module = _module ?: throw IllegalStateException("Model not loaded")
//
//        val resizedBitmap = bitmap.scale(640, 640)
//        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
//            resizedBitmap,
//            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
//            TensorImageUtils.TORCHVISION_NORM_STD_RGB
//        )
//
//        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
//        val outputShape = outputTensor.shape() // should be [1, N, 6]
//        Log.d("PyTorchOutput", "Shape: ${outputShape.joinToString()}")
//        val outputArray = outputTensor.dataAsFloatArray
//
//        val detections = mutableListOf<DetectionResult>()
//
//        val batchSize = outputShape[0].toInt()
//        val numBoxes = outputShape[1].toInt()
//        val numAttributes = outputShape[2].toInt()  // should be 6: [x1, y1, x2, y2, score, class_id]
////        val numBoxes = outputShape[0].toInt()
////        val numAttributes = outputShape[1].toInt()
//
//        for (i in 0 until numBoxes) {
//            val offset = i * numAttributes
//            val x1 = outputArray[offset]
//            val y1 = outputArray[offset + 1]
//            val x2 = outputArray[offset + 2]
//            val y2 = outputArray[offset + 3]
//            val score = outputArray[offset + 4]
//            val label = outputArray[offset + 5].toInt()
//
//            if (score > 0.25f) {
//                detections.add(
//                    DetectionResult(
//                        classIndex = label,
//                        score = score,
//                        box = floatArrayOf(x1, y1, x2, y2),
//                        className = AppConstants.CLASS_LABELS.getOrElse(label) { "Unknown" }
//                    )
//                )
//            }
//        }
//        return detections
//    }

    @Suppress("SameParameterValue")
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

    fun release() {
        _module?.destroy()
        _module = null
    }
}


/**
 * A simple Non-Max Suppression implementation.
 * You might want to use a more optimized version for production.
 */
private fun nonMaxSuppression(
    detections: List<DetectionResult>,
    iouThreshold: Float = 0.5f
): List<DetectionResult> {
    // Sort detections by score in descending order
    val sortedDetections = detections.sortedByDescending { it.score }
    val finalDetections = mutableListOf<DetectionResult>()

    for (detection in sortedDetections) {
        var shouldAdd = true
        for (finalDetection in finalDetections) {
            // Check if the new detection overlaps significantly with an existing one of the same class
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

/**
 * Calculates Intersection over Union (IoU) for two bounding boxes.
 */
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