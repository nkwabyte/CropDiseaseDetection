package com.nkwabyte.cropdiseasedetection.common.utils

import android.content.Context
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import java.io.File

fun saveDetectionReport(context: Context, results: List<DetectionResult>, labels: List<String>): File {
    val file = File(context.filesDir, "detection_report_${System.currentTimeMillis()}.txt")
    file.bufferedWriter().use { out ->
        results.forEach { result ->
            out.write("Class: ${labels[result.classIndex]}, Confidence: ${"%.2f".format(result.score)}, Box: ${result.box.joinToString()}")
            out.newLine()
        }
    }
    return file
}
