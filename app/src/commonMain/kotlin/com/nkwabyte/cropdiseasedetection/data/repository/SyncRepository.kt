package com.nkwabyte.cropdiseasedetection.data.repository

import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

@Serializable
data class DetectionRecord(
    val userId: String,
    val cropName: String,
    val imageUrl: String,
    val detectionSuccessful: Boolean,
    val isCropMismatch: Boolean,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestamp: Long,
    val matchingResults: List<DetectionResult>,
    val rawResults: List<DetectionResult>
)

class SyncRepository {
    private val firestore = Firebase.firestore

    suspend fun saveDetectionRecord(
        cropName: String,
        imageUrl: String,
        detectionSuccessful: Boolean,
        isCropMismatch: Boolean,
        imageWidth: Int,
        imageHeight: Int,
        matchingResults: List<DetectionResult>,
        rawResults: List<DetectionResult>
    ) {
        try {
            val user = Firebase.auth.currentUser
            if (user == null) {
                println("Cannot save detection record: user not signed in")
                // In a real app we might allow anonymous saves or queue them.
                return
            }

            val record = DetectionRecord(
                userId = user.uid,
                cropName = cropName,
                imageUrl = imageUrl,
                detectionSuccessful = detectionSuccessful,
                isCropMismatch = isCropMismatch,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                timestamp = io.ktor.util.date.GMTDate().timestamp,
                matchingResults = matchingResults,
                rawResults = rawResults
            )

            firestore.collection("detections").add(record)
            println("Successfully saved detection to Firestore")
        } catch (e: Exception) {
            println("Failed to save detection to Firestore: ${e.message}")
        }
    }
}
