package com.nkwabyte.cropdiseasedetection.data.repository

import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.UserRole
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

@Serializable
data class UserProfileRecord(
    val userId: String = "",
    val userName: String = "",
    val userEmail: String? = null,
    val role: String = "FARMER",
    val createdAt: Long = 0L
)

@Serializable
data class FlaggedRecord(
    val flaggedBy: String,
    val flaggedByName: String,
    val flaggedByRole: String,
    val timestamp: Long,
    val cropName: String,
    val imageUrl: String,
    val detectionResults: List<DetectionResult>,
    val classificationLabel: String?,
    val classifierConfidence: Float,
    val imageWidth: Int,
    val imageHeight: Int,
    val notes: String?,
    val platform: String?,
    val modelName: String?,
    val detectionThreshold: Float,
    val iouThreshold: Float,
    val classifierThreshold: Float
)

@Serializable
data class PendingDetectionRecord(
    val id: String,
    val imageBase64: String,
    val cropName: String,
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

@Serializable
data class PendingFlaggedRecord(
    val id: String,
    val imageBase64: String,
    val cropName: String,
    val userRole: String,
    val detectionResults: List<DetectionResult>,
    val classificationLabel: String?,
    val classifierConfidence: Float,
    val imageWidth: Int,
    val imageHeight: Int,
    val notes: String?,
    val platform: String?,
    val modelName: String?,
    val detectionThreshold: Float,
    val iouThreshold: Float,
    val classifierThreshold: Float,
    val timestamp: Long
)

class SyncRepository {
    private val firestore = Firebase.firestore
    private val pendingDetections = mutableListOf<PendingDetectionRecord>()
    private val pendingFlagged = mutableListOf<PendingFlaggedRecord>()

    @OptIn(ExperimentalEncodingApi::class)
    fun queuePendingDetectionRecord(
        imageBytes: ByteArray,
        cropName: String,
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
        val base64 = Base64.encode(imageBytes)
        val now = io.ktor.util.date.GMTDate().timestamp
        val record = PendingDetectionRecord(
            id = now.toString(),
            imageBase64 = base64,
            cropName = cropName,
            detectionSuccessful = detectionSuccessful,
            isCropMismatch = isCropMismatch,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            timestamp = now,
            matchingResults = matchingResults,
            rawResults = rawResults,
            modelName = modelName,
            modelVersion = modelVersion,
            platform = platform
        )
        pendingDetections.add(record)
        println("Offline: Queued detection record locally (${pendingDetections.size} pending)")
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun queuePendingFlaggedRecord(
        imageBytes: ByteArray,
        cropName: String,
        userRole: String,
        detectionResults: List<DetectionResult>,
        classificationLabel: String?,
        classifierConfidence: Float,
        imageWidth: Int,
        imageHeight: Int,
        notes: String?,
        platform: String?,
        modelName: String?,
        detectionThreshold: Float,
        iouThreshold: Float,
        classifierThreshold: Float
    ) {
        val base64 = Base64.encode(imageBytes)
        val now = io.ktor.util.date.GMTDate().timestamp
        val record = PendingFlaggedRecord(
            id = now.toString(),
            imageBase64 = base64,
            cropName = cropName,
            userRole = userRole,
            detectionResults = detectionResults,
            classificationLabel = classificationLabel,
            classifierConfidence = classifierConfidence,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            notes = notes,
            platform = platform,
            modelName = modelName,
            detectionThreshold = detectionThreshold,
            iouThreshold = iouThreshold,
            classifierThreshold = classifierThreshold,
            timestamp = now
        )
        pendingFlagged.add(record)
        println("Offline: Queued flagged record locally (${pendingFlagged.size} pending)")
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun processPendingQueue(cloudinaryApi: com.nkwabyte.cropdiseasedetection.data.network.CloudinaryApi) {
        if (pendingDetections.isEmpty() && pendingFlagged.isEmpty()) return

        println("Processing pending offline queue: ${pendingDetections.size} detections, ${pendingFlagged.size} flagged")

        val detectionsCopy = pendingDetections.toList()
        for (pending in detectionsCopy) {
            try {
                val bytes = Base64.decode(pending.imageBase64)
                val url = cloudinaryApi.uploadImage(bytes)
                if (url != null) {
                    saveDetectionRecord(
                        cropName = pending.cropName,
                        imageUrl = url,
                        detectionSuccessful = pending.detectionSuccessful,
                        isCropMismatch = pending.isCropMismatch,
                        imageWidth = pending.imageWidth,
                        imageHeight = pending.imageHeight,
                        matchingResults = pending.matchingResults,
                        rawResults = pending.rawResults,
                        modelName = pending.modelName,
                        modelVersion = pending.modelVersion,
                        platform = pending.platform
                    )
                    pendingDetections.remove(pending)
                    println("Successfully uploaded pending detection record!")
                }
            } catch (e: Exception) {
                println("Failed to process pending detection: ${e.message}")
            }
        }

        val flaggedCopy = pendingFlagged.toList()
        for (pending in flaggedCopy) {
            try {
                val bytes = Base64.decode(pending.imageBase64)
                val url = cloudinaryApi.uploadImage(bytes, folder = "flagged")
                if (url != null) {
                    saveFlaggedRecord(
                        imageUrl = url,
                        cropName = pending.cropName,
                        userRole = pending.userRole,
                        detectionResults = pending.detectionResults,
                        classificationLabel = pending.classificationLabel,
                        classifierConfidence = pending.classifierConfidence,
                        imageWidth = pending.imageWidth,
                        imageHeight = pending.imageHeight,
                        notes = pending.notes,
                        platform = pending.platform,
                        modelName = pending.modelName,
                        detectionThreshold = pending.detectionThreshold,
                        iouThreshold = pending.iouThreshold,
                        classifierThreshold = pending.classifierThreshold
                    )
                    pendingFlagged.remove(pending)
                    println("Successfully uploaded pending flagged record!")
                }
            } catch (e: Exception) {
                println("Failed to process pending flagged record: ${e.message}")
            }
        }
    }

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
        val remoteRecords = try {
            val user = Firebase.auth.currentUser
            val uid = user?.uid ?: "anonymous"

            val response = firestore.collection("detections")
                .where { "userId" equalTo uid }
                .get()

            response.documents.map { document ->
                document.data(DetectionRecord.serializer())
            }
        } catch (e: Exception) {
            println("Failed to fetch detection records: ${e.message}")
            emptyList()
        }

        val localRecords = pendingDetections.map { pending ->
            DetectionRecord(
                userId = Firebase.auth.currentUser?.uid ?: "anonymous",
                cropName = pending.cropName,
                imageUrl = "data:image/jpeg;base64,${pending.imageBase64}",
                detectionSuccessful = pending.detectionSuccessful,
                isCropMismatch = pending.isCropMismatch,
                imageWidth = pending.imageWidth,
                imageHeight = pending.imageHeight,
                timestamp = pending.timestamp,
                matchingResults = pending.matchingResults,
                rawResults = pending.rawResults,
                modelName = pending.modelName,
                modelVersion = pending.modelVersion,
                platform = pending.platform
            )
        }

        return (remoteRecords + localRecords).sortedByDescending { it.timestamp }
    }

    suspend fun saveUserProfile(userName: String, userEmail: String?, role: UserRole) {
        try {
            val user = Firebase.auth.currentUser ?: return
            val record = UserProfileRecord(
                userId = user.uid,
                userName = userName,
                userEmail = userEmail,
                role = role.name,
                createdAt = io.ktor.util.date.GMTDate().timestamp
            )
            firestore.collection("users").document(user.uid).set(record)
            println("Successfully saved user profile to Firestore")
        } catch (e: Exception) {
            println("Failed to save user profile to Firestore: ${e.message}")
        }
    }

    suspend fun getUserRole(): UserRole {
        return try {
            val user = Firebase.auth.currentUser ?: return UserRole.FARMER
            val doc = firestore.collection("users").document(user.uid).get()
            if (doc.exists) {
                val record = doc.data(UserProfileRecord.serializer())
                runCatching { UserRole.valueOf(record.role) }.getOrDefault(UserRole.FARMER)
            } else {
                UserRole.FARMER
            }
        } catch (e: Exception) {
            println("Failed to fetch user role: ${e.message}")
            UserRole.FARMER
        }
    }

    suspend fun saveFlaggedRecord(
        imageUrl: String,
        cropName: String,
        userRole: String,
        detectionResults: List<DetectionResult>,
        classificationLabel: String?,
        classifierConfidence: Float,
        imageWidth: Int,
        imageHeight: Int,
        notes: String?,
        platform: String?,
        modelName: String?,
        detectionThreshold: Float,
        iouThreshold: Float,
        classifierThreshold: Float
    ) {
        try {
            val user = Firebase.auth.currentUser
            val uid = user?.uid ?: "anonymous"
            val displayName = user?.displayName ?: "Unknown"

            val record = FlaggedRecord(
                flaggedBy = uid,
                flaggedByName = displayName,
                flaggedByRole = userRole,
                timestamp = io.ktor.util.date.GMTDate().timestamp,
                cropName = cropName,
                imageUrl = imageUrl,
                detectionResults = detectionResults,
                classificationLabel = classificationLabel,
                classifierConfidence = classifierConfidence,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                notes = notes,
                platform = platform,
                modelName = modelName,
                detectionThreshold = detectionThreshold,
                iouThreshold = iouThreshold,
                classifierThreshold = classifierThreshold
            )

            firestore.collection("flagged").add(record)
            println("Successfully saved flagged record to Firestore")
        } catch (e: Exception) {
            println("Failed to save flagged record to Firestore: ${e.message}")
            throw e
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

            val response = firestore.collection("detections")
                .where { "userId" equalTo uid }
                .get()

            for (document in response.documents) {
                document.reference.update("userId" to "anonymous")
            }
            println("Successfully anonymized user data in Firestore")
        } catch (e: Exception) {
            println("Failed to anonymize user data: ${e.message}")
        }
    }
}
