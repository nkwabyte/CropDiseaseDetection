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
    val rawResults: List<DetectionResult>,
    val modelName: String? = null,
    val modelVersion: String? = null,
    val platform: String? = null
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
        rawResults: List<DetectionResult>,
        modelName: String? = null,
        modelVersion: String? = null,
        platform: String? = null
    ) {
        try {
            val user = Firebase.auth.currentUser
            val uid = user?.uid ?: "anonymous"

            val record = DetectionRecord(
                userId = uid,
                cropName = cropName,
                imageUrl = imageUrl,
                detectionSuccessful = detectionSuccessful,
                isCropMismatch = isCropMismatch,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                timestamp = io.ktor.util.date.GMTDate().timestamp,
                matchingResults = matchingResults,
                rawResults = rawResults,
                modelName = modelName,
                modelVersion = modelVersion,
                platform = platform
            )

            firestore.collection("detections").add(record)
            println("Successfully saved detection to Firestore")
        } catch (e: Exception) {
            println("Failed to save detection to Firestore: ${e.message}")
        }
    }

    suspend fun getDetectionRecords(): List<DetectionRecord> {
        return try {
            val user = Firebase.auth.currentUser
            val uid = user?.uid ?: "anonymous"

            val response = firestore.collection("detections")
                .where { "userId" equalTo uid }
                .get()

            response.documents.map { document ->
                document.data(DetectionRecord.serializer())
            }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            println("Failed to fetch detection records: ${e.message}")
            emptyList()
        }
    }

    suspend fun anonymizeUserData() {
        try {
            val user = Firebase.auth.currentUser
            val uid = user?.uid
            if (uid == null) {
                println("No logged-in user to anonymize.")
                return
            }

            // Get all records belonging to this user
            val response = firestore.collection("detections")
                .where { "userId" equalTo uid }
                .get()

            // Update each record's userId to 'anonymous'
            for (document in response.documents) {
                document.reference.update("userId" to "anonymous")
            }
            println("Successfully anonymized user data in Firestore")
        } catch (e: Exception) {
            println("Failed to anonymize user data: ${e.message}")
        }
    }
}
