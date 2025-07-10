//package com.nkwabyte.cropdiseasedetection.model
//
//import android.content.Context
//import android.graphics.Bitmap
//import org.pytorch.IValue
//import org.pytorch.Module
//import org.pytorch.torchvision.TensorImageUtils
//import java.io.File
//import java.io.FileOutputStream
//import androidx.core.graphics.scale
//
//class ObjectDetector(context: Context) {
//
////    private val module: Module = Module.load(assetFilePath(context, "faster_rcnn.pt"))
//
//    private val module: Module
//
//    init {
//        try {
//            val modelPath = assetFilePath(context, "faster_rcnn.pt")
//            module = Module.load(modelPath)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            throw RuntimeException("Model loading failed: ${e.message}")
//        }
//    }
//    fun detect(originalBitmap: Bitmap): List<DetectionResult> {
//        val resizedBitmap = originalBitmap.scale(640, 640)
//
//        val inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
//            resizedBitmap,
//            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
//            TensorImageUtils.TORCHVISION_NORM_STD_RGB
//        )
//
//        val outputTuple = module.forward(IValue.from(inputTensor)).toTuple()
//        val boxes = outputTuple[0].toTensor().dataAsFloatArray
//        val scores = outputTuple[1].toTensor().dataAsFloatArray
//        val labels = outputTuple[2].toTensor().dataAsLongArray
//
//        val results = mutableListOf<DetectionResult>()
//        for (i in scores.indices) {
//            if (scores[i] > 0.5f) {
//                val box = boxes.copyOfRange(i * 4, i * 4 + 4)
//                results.add(
//                    DetectionResult(
//                        classIndex = labels[i].toInt(),
//                        score = scores[i],
//                        box = box
//                    )
//                )
//            }
//        }
//
//        return results
//    }
//
//    fun assetFilePath(context: Context, assetName: String): String {
//        val file = File(context.filesDir, assetName)
//        if (file.exists() && file.length() > 0) {
//            return file.absolutePath
//        }
//
//        context.assets.open(assetName).use { inputStream ->
//            FileOutputStream(file).use { outputStream ->
//                val buffer = ByteArray(4 * 1024)
//                var read: Int
//                while (inputStream.read(buffer).also { read = it } != -1) {
//                    outputStream.write(buffer, 0, read)
//                }
//                outputStream.flush()
//            }
//        }
//
//        return file.absolutePath
//    }
//}
