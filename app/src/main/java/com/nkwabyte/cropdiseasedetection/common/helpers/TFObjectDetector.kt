package com.nkwabyte.cropdiseasedetection.common.helpers

import android.content.Context
import android.graphics.Bitmap
import com.nkwabyte.cropdiseasedetection.common.AppConstants
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.utils.preprocessBitmap
import java.io.FileInputStream

class TFObjectDetector(private val context: Context) {
    private var interpreter: Interpreter? = null

    val isLoaded: Boolean
        get() = interpreter != null

    suspend fun loadModel() {
        if (interpreter == null) {
            val modelBuffer = loadModelFile("best.tflite")
            interpreter = Interpreter(modelBuffer)
        }
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val interpreter = interpreter ?: throw IllegalStateException("Model not loaded")
        val input = preprocessBitmap(bitmap)
        val output = Array(1) {
            Array(8400) {
                FloatArray(4 + AppConstants.CLASS_LABELS.size + 1)
            }
        }
        interpreter.run(arrayOf(input), output)
        val threshold = 0.5f
        val detections = mutableListOf<DetectionResult>()

        for (detection in output[0]) {
            val x = detection[0]
            val y = detection[1]
            val w = detection[2]
            val h = detection[3]
            val objectness = detection[4]

            val classScores = detection.copyOfRange(5, 28)
            val (classIndex, classScore) = classScores.withIndex().maxByOrNull {
                it.value
            } ?: continue

            val confidence = objectness * classScore
            if (confidence > threshold) {
                val x1 = x - w / 2
                val y1 = y - h / 2
                val x2 = x + w / 2
                val y2 = y + h / 2

                detections.add(
                    DetectionResult(
                        classIndex = classIndex,
                        score = confidence,
                        box = floatArrayOf(x1, y1, x2, y2),
                        className = AppConstants.CLASS_LABELS[classIndex],
                    )
                )
            }
        }
        return detections
    }

    @Suppress("SameParameterValue")
    private fun loadModelFile(assetName: String): MappedByteBuffer {
        context.assets.openFd(assetName).use { fd ->
            FileInputStream(fd.fileDescriptor).channel.use { fc ->
                return fc.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            }
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
