package com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.model.DetectionData
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.data.network.CloudinaryApi
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class FlagState {
    object Idle : FlagState()
    object Loading : FlagState()
    data class Success(val message: String = "Detection flagged successfully") : FlagState()
    data class Error(val message: String) : FlagState()
}

class DetectionViewModel(
    private val detector: ObjectDetector,
    private val cloudinaryApi: CloudinaryApi,
    private val syncRepository: SyncRepository
) : ViewModel() {
    private val detectionData = DetectionData(
        isModelLoading = true,
        isDetecting = false,
        isDetectionSuccessful = false,
        results = emptyList(),
        imageWidth = 0,
        imageHeight = 0
    )

    private val _detectionState = MutableStateFlow(detectionData)
    val detectionState = _detectionState.asStateFlow()

    private val _flagState = MutableStateFlow<FlagState>(FlagState.Idle)
    val flagState = _flagState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                detector.loadModel()
                detector.loadClassifierModel()
                _detectionState.update {
                    it.copy(
                        isModelLoading = false,
                        isDetectionSuccessful = false,
                        isDetected = false,
                        isDetecting = false,
                    )
                }
            } catch (e: Exception) {
                println("Model loading failed: ${e.message}")
            }
        }
    }

    fun detect(imageBytes: ByteArray, crop: String, width: Int, height: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                _detectionState.update { it.copy(isDetecting = true, isClassifierRejected = false) }

                // Pick up a detector change made in settings since the last run.
                // loadModel() is a no-op when the selection is unchanged.
                detector.loadModel()

                // Stage 2: Detection
                val results = detector.detect(imageBytes)
                println("PyTorch detection results: $results")

                // Stage 1: Classifier
                val classification = detector.classify(imageBytes)
                println("Classifier result: $classification")

                // If detector found nothing AND classifier rejected the image as not a crop leaf:
                if (results.isEmpty() && classification != null && !classification.isAccepted) {
                    println("Classification rejected: Not a Corn, Pepper, or Tomato crop.")
                    _detectionState.update {
                        it.copy(
                            isDetecting = false,
                            isDetected = true,
                            isDetectionSuccessful = false,
                            isCropMissMatch = true,
                            isClassifierRejected = true,
                            classifierConfidence = classification.confidence,
                            classificationLabel = classification.label,
                            imageWidth = width,
                            imageHeight = height,
                            results = emptyList()
                        )
                    }
                    syncDetection(
                        imageBytes = imageBytes,
                        crop = crop,
                        width = width,
                        height = height,
                        matchingResults = emptyList(),
                        rawResults = results,
                        detectionSuccessful = false,
                        isCropMismatch = true
                    )
                    return@launch
                }

                if (results.isEmpty()) {
                    val isRejected = classification != null && !classification.isAccepted
                    println("No detection results found. Classifier rejected: $isRejected")
                    _detectionState.update {
                        it.copy(
                            results = emptyList(),
                            isDetecting = false,
                            isDetected = true,
                            isDetectionSuccessful = false,
                            isCropMissMatch = isRejected,
                            classifierConfidence = classification?.confidence ?: 0f,
                            classificationLabel = classification?.label,
                            imageWidth = width,
                            imageHeight = height,
                        )
                    }
                    syncDetection(
                        imageBytes = imageBytes,
                        crop = crop,
                        width = width,
                        height = height,
                        matchingResults = emptyList(),
                        rawResults = results,
                        detectionSuccessful = false,
                        isCropMismatch = isRejected
                    )
                    return@launch
                }

                val matchingResults = if (crop.isBlank()) {
                    results
                } else {
                    results.filter {
                        it.className?.contains(crop, ignoreCase = true) == true
                    }
                }
                println("Matching results: $matchingResults")

                val isMismatch = matchingResults.isEmpty() || (classification != null && !classification.isAccepted)
                println("Crop mismatch: $isMismatch")

                _detectionState.update {
                    it.copy(
                        results = matchingResults,
                        isDetecting = false,
                        isDetected = true,
                        isDetectionSuccessful = matchingResults.isNotEmpty(),
                        isCropMissMatch = isMismatch,
                        classifierConfidence = classification?.confidence ?: 0f,
                        classificationLabel = classification?.label,
                        imageWidth = width,
                        imageHeight = height,
                    )
                }

                syncDetection(
                    imageBytes = imageBytes,
                    crop = crop,
                    width = width,
                    height = height,
                    matchingResults = matchingResults,
                    rawResults = results,
                    detectionSuccessful = matchingResults.isNotEmpty(),
                    isCropMismatch = isMismatch
                )
            } catch (e: Exception) {
                println("PyTorch detection failed: ${e.message}")
            } finally {
                _detectionState.update {
                    it.copy(isDetecting = false)
                }
            }
        }
    }

    /**
     * Persists a scan to Firestore. Called on every terminal path of [detect] — including
     * the ones with no findings — so the history page mirrors what the user actually ran.
     * Runs fire-and-forget: the UI state is already published by the time we get here.
     */
    private fun syncDetection(
        imageBytes: ByteArray,
        crop: String,
        width: Int,
        height: Int,
        matchingResults: List<DetectionResult>,
        rawResults: List<DetectionResult>,
        detectionSuccessful: Boolean,
        isCropMismatch: Boolean
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val imageUrl = cloudinaryApi.uploadImage(imageBytes)
                if (imageUrl != null) {
                    syncRepository.saveDetectionRecord(
                        cropName = crop,
                        imageUrl = imageUrl,
                        detectionSuccessful = detectionSuccessful,
                        isCropMismatch = isCropMismatch,
                        imageWidth = width,
                        imageHeight = height,
                        matchingResults = matchingResults,
                        rawResults = rawResults,
                        modelName = MODEL_NAME,
                        modelVersion = MODEL_VERSION,
                        platform = PLATFORM
                    )
                } else {
                    syncRepository.queuePendingDetectionRecord(
                        imageBytes = imageBytes,
                        cropName = crop,
                        detectionSuccessful = detectionSuccessful,
                        isCropMismatch = isCropMismatch,
                        imageWidth = width,
                        imageHeight = height,
                        matchingResults = matchingResults,
                        rawResults = rawResults,
                        modelName = MODEL_NAME,
                        modelVersion = MODEL_VERSION,
                        platform = PLATFORM
                    )
                }
                syncRepository.processPendingQueue(cloudinaryApi)
            } catch (e: Exception) {
                println("Sync workflow failed: ${e.message}")
            }
        }
    }

    fun flagDetection(
        imageBytes: ByteArray,
        cropName: String,
        userRole: String,
        notes: String?,
        detectionThreshold: Float,
        iouThreshold: Float,
        classifierThreshold: Float
    ) {
        val state = _detectionState.value
        _flagState.value = FlagState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val imageUrl = cloudinaryApi.uploadImage(imageBytes, folder = "flagged")
                if (imageUrl != null) {
                    syncRepository.saveFlaggedRecord(
                        imageUrl = imageUrl,
                        cropName = cropName,
                        userRole = userRole,
                        detectionResults = state.results,
                        classificationLabel = state.classificationLabel,
                        classifierConfidence = state.classifierConfidence,
                        imageWidth = state.imageWidth ?: 0,
                        imageHeight = state.imageHeight ?: 0,
                        notes = notes,
                        platform = PLATFORM,
                        modelName = MODEL_NAME,
                        detectionThreshold = detectionThreshold,
                        iouThreshold = iouThreshold,
                        classifierThreshold = classifierThreshold
                    )
                } else {
                    syncRepository.queuePendingFlaggedRecord(
                        imageBytes = imageBytes,
                        cropName = cropName,
                        userRole = userRole,
                        detectionResults = state.results,
                        classificationLabel = state.classificationLabel,
                        classifierConfidence = state.classifierConfidence,
                        imageWidth = state.imageWidth ?: 0,
                        imageHeight = state.imageHeight ?: 0,
                        notes = notes,
                        platform = PLATFORM,
                        modelName = MODEL_NAME,
                        detectionThreshold = detectionThreshold,
                        iouThreshold = iouThreshold,
                        classifierThreshold = classifierThreshold
                    )
                }
                syncRepository.processPendingQueue(cloudinaryApi)
                _flagState.value = FlagState.Success()
            } catch (e: Exception) {
                println("Flag workflow failed: ${e.message}")
                _flagState.value = FlagState.Error(e.message ?: "Flag submission failed")
            }
        }
    }

    fun resetFlagState() {
        _flagState.value = FlagState.Idle
    }

    fun reset() {
        _detectionState.update {
            DetectionData(
                isModelLoading = true,
                isDetecting = false,
                isDetectionSuccessful = false,
                isCropMissMatch = false,
                isDetected = false,
                results = emptyList(),
                imageWidth = 0,
                imageHeight = 0
            )
        }
    }

    private companion object {
        const val MODEL_NAME = "ExecuTorch (PyTorch Mobile)"
        const val MODEL_VERSION = "v1.0"
        const val PLATFORM = "iOS/Android App"
    }
}
