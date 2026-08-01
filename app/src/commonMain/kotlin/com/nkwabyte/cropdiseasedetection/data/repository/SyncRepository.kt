package com.nkwabyte.cropdiseasedetection.data.repository

import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.UserRole
import com.nkwabyte.cropdiseasedetection.data.storage.OfflineQueueStore
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

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

class SyncRepository(
    private val queueStore: OfflineQueueStore
) {
    private val firestore = Firebase.firestore
    private val json = Json { ignoreUnknownKeys = true }

    private val pendingDetections = mutableListOf<PendingDetectionRecord>()
    private val pendingFlagged = mutableListOf<PendingFlaggedRecord>()

    /** Guards the two lists and the store; never held across a network call. */
    private val queueLock = Mutex()

    /** Guards a whole drain run. Only ever taken before [queueLock], never while holding it. */
    private val processLock = Mutex()
    private var isQueueLoaded = false

    /**
     * Reads the queue back off disk on first use. Lazy rather than done in the constructor
     * because the store does blocking file I/O and Koin builds this on the main thread.
     *
     * Callers reach this from the main dispatcher (HistoryViewModel does), so the repository
     * moves its own blocking work off it rather than relying on every caller to remember.
     * Default rather than IO because commonMain has no Dispatchers.IO.
     */
    private suspend fun ensureQueueLoaded() = queueLock.withLock {
        if (isQueueLoaded) return@withLock
        isQueueLoaded = true

        withContext(Dispatchers.Default) {
            queueStore.readAll().forEach { (name, contents) ->
                val restored = runCatching {
                    when {
                        name.startsWith(DETECTION_PREFIX) ->
                            pendingDetections.add(json.decodeFromString(PendingDetectionRecord.serializer(), contents))
                        name.startsWith(FLAGGED_PREFIX) ->
                            pendingFlagged.add(json.decodeFromString(PendingFlaggedRecord.serializer(), contents))
                        else -> false
                    }
                }.getOrDefault(false)

                // A record we cannot parse will never sync, so drop it rather than retry it
                // on every launch forever.
                if (!restored) {
                    println("Offline: discarding unreadable queue entry $name")
                    queueStore.delete(name)
                }
            }

            pendingDetections.sortBy { it.timestamp }
            pendingFlagged.sortBy { it.timestamp }
            if (pendingDetections.isNotEmpty() || pendingFlagged.isNotEmpty()) {
                println(
                    "Offline: restored ${pendingDetections.size} detections, " +
                        "${pendingFlagged.size} flagged from disk"
                )
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun queuePendingDetectionRecord(
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
        ensureQueueLoaded()

        val now = io.ktor.util.date.GMTDate().timestamp
        val record = PendingDetectionRecord(
            id = newRecordId(now),
            imageBase64 = Base64.encode(imageBytes),
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

        queueLock.withLock {
            withContext(Dispatchers.Default) {
                pendingDetections.add(record)
                queueStore.save(
                    DETECTION_PREFIX + record.id,
                    json.encodeToString(PendingDetectionRecord.serializer(), record)
                )
                trimLocked(pendingDetections, DETECTION_PREFIX) { it.id }
                println("Offline: Queued detection record locally (${pendingDetections.size} pending)")
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun queuePendingFlaggedRecord(
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
        ensureQueueLoaded()

        val now = io.ktor.util.date.GMTDate().timestamp
        val record = PendingFlaggedRecord(
            id = newRecordId(now),
            imageBase64 = Base64.encode(imageBytes),
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

        queueLock.withLock {
            withContext(Dispatchers.Default) {
                pendingFlagged.add(record)
                queueStore.save(
                    FLAGGED_PREFIX + record.id,
                    json.encodeToString(PendingFlaggedRecord.serializer(), record)
                )
                trimLocked(pendingFlagged, FLAGGED_PREFIX) { it.id }
                println("Offline: Queued flagged record locally (${pendingFlagged.size} pending)")
            }
        }
    }

    /**
     * The queue now outlives the process, so it needs a ceiling — otherwise a long spell
     * offline grows it without bound. Drops the oldest entries past [MAX_PENDING].
     * Caller must hold [queueLock].
     */
    private fun <T> trimLocked(queue: MutableList<T>, prefix: String, idOf: (T) -> String) {
        while (queue.size > MAX_PENDING) {
            val dropped = queue.removeAt(0)
            queueStore.delete(prefix + idOf(dropped))
            println("Offline: queue full, dropped oldest ${prefix.trimEnd('-')} record")
        }
    }

    /**
     * Uploads everything queued, dropping each entry only once it is safely in Firestore.
     *
     * [processLock] serialises whole runs: a scan finishing and the history page opening
     * both trigger this, and without it the two would upload the same record twice and
     * leave duplicate documents behind. The second caller simply finds the queue drained.
     */
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun processPendingQueue(
        cloudinaryApi: com.nkwabyte.cropdiseasedetection.data.network.CloudinaryApi
    ) = processLock.withLock {
        ensureQueueLoaded()

        val detectionsCopy = queueLock.withLock { pendingDetections.toList() }
        val flaggedCopy = queueLock.withLock { pendingFlagged.toList() }
        if (detectionsCopy.isEmpty() && flaggedCopy.isEmpty()) return@withLock

        println("Processing pending offline queue: ${detectionsCopy.size} detections, ${flaggedCopy.size} flagged")

        for (pending in detectionsCopy) {
            try {
                val bytes = withContext(Dispatchers.Default) { Base64.decode(pending.imageBase64) }
                val url = cloudinaryApi.uploadImage(bytes)
                if (url != null) {
                    val saved = saveDetectionRecord(
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
                    // Only drop it once Firestore actually accepted it; otherwise leave it
                    // queued so the next drain retries rather than losing the scan.
                    if (saved) {
                        queueLock.withLock {
                            pendingDetections.removeAll { it.id == pending.id }
                            queueStore.delete(DETECTION_PREFIX + pending.id)
                        }
                        println("Successfully uploaded pending detection record!")
                    }
                }
            } catch (e: Exception) {
                println("Failed to process pending detection: ${e.message}")
            }
        }

        for (pending in flaggedCopy) {
            try {
                val bytes = withContext(Dispatchers.Default) { Base64.decode(pending.imageBase64) }
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
                    queueLock.withLock {
                        pendingFlagged.removeAll { it.id == pending.id }
                        queueStore.delete(FLAGGED_PREFIX + pending.id)
                    }
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
    ): Boolean {
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
            return true
        } catch (e: Exception) {
            println("Failed to save detection to Firestore: ${e.message}")
            return false
        }
    }

    suspend fun getDetectionRecords(): List<DetectionRecord> {
        ensureQueueLoaded()

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

        // Building data: URLs concatenates the full base64 of every queued image, so keep
        // it off whichever dispatcher the caller happened to be on.
        val localRecords = withContext(Dispatchers.Default) {
            queueLock.withLock { pendingDetections.toList() }.map { pending ->
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
        ensureQueueLoaded()
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

            // Queued scans are stamped with the caller's uid at sync time, so anything
            // still pending would re-attach this account moments after we detached it.
            queueLock.withLock {
                pendingDetections.forEach { queueStore.delete(DETECTION_PREFIX + it.id) }
                pendingDetections.clear()
            }
            println("Successfully anonymized user data in Firestore")
        } catch (e: Exception) {
            println("Failed to anonymize user data: ${e.message}")
        }
    }

    /**
     * Doubles as the on-disk file name, so it has to stay unique: two scans can land in
     * the same millisecond, and the timestamp alone would have them overwrite each other.
     */
    private fun newRecordId(timestamp: Long): String =
        "$timestamp-${Random.nextInt(0, 0xFFFFFF).toString(16)}"

    private companion object {
        const val DETECTION_PREFIX = "detection-"
        const val FLAGGED_PREFIX = "flagged-"

        /**
         * Each entry carries a base64 image and is held in memory as well as on disk, so
         * the queue needs a ceiling now that it survives restarts. Comfortably more than
         * a realistic offline session, without letting a stuck queue eat the device.
         */
        const val MAX_PENDING = 25
    }
}
