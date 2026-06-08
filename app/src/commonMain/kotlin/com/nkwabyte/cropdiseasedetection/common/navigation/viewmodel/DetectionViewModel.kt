package com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.model.DetectionData
import com.nkwabyte.cropdiseasedetection.data.network.CloudinaryApi
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DetectionViewModel(
    private val detector: ObjectDetector,
    private val cloudinaryApi: CloudinaryApi,
    private val syncRepository: SyncRepository
): ViewModel() {
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

                // Stage 1: Classifier
                val classification = detector.classify(imageBytes)
                println("Classifier result: $classification")

                if (classification != null && !classification.isAccepted) {
                    println("Classification rejected: Not a Corn, Pepper, or Tomato crop.")
                    _detectionState.update {
                        it.copy(
                            isDetecting = false,
                            isDetected = true,
                            isDetectionSuccessful = false,
                            isClassifierRejected = true,
                            classifierConfidence = classification.confidence,
                            classificationLabel = classification.label,
                            imageWidth = width,
                            imageHeight = height,
                            results = emptyList()
                        )
                    }
                    return@launch
                }

                // Stage 2: Detection
                val results = detector.detect(imageBytes)
                println("PyTorch detection results: $results")

                if (results.isEmpty()) {
                    println("No detection results found")
                    // No detection made at all
                    _detectionState.update {
                        it.copy(
                            results = emptyList(),
                            isDetecting = false,
                            isDetected = true,
                            isDetectionSuccessful = false,
                            isCropMissMatch = false,
                            classifierConfidence = classification?.confidence ?: 0f,
                            classificationLabel = classification?.label,
                            imageWidth = width,
                            imageHeight = height,
                        )
                    }
                    return@launch
                }

                val matchingResults = results.filter {
                    it.className?.contains(crop, ignoreCase = true) == true
                }
                println("Matching results: $matchingResults")

                val isMismatch = matchingResults.isEmpty()
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

                // Fire and forget image upload and sync
                viewModelScope.launch(Dispatchers.Default) {
                    try {
                        val imageUrl = cloudinaryApi.uploadImage(imageBytes)
                        if (imageUrl != null) {
                            syncRepository.saveDetectionRecord(
                                cropName = crop,
                                imageUrl = imageUrl,
                                detectionSuccessful = matchingResults.isNotEmpty(),
                                isCropMismatch = isMismatch,
                                imageWidth = width,
                                imageHeight = height,
                                matchingResults = matchingResults,
                                rawResults = results,
                                modelName = "ExecuTorch (PyTorch Mobile)", // Assuming hardcoded for now, or extracted from detector config
                                modelVersion = "v1.0",
                                platform = "iOS/Android App" // Ideally fetched via KMP platform API
                            )
                        }
                    } catch (e: Exception) {
                        println("Sync workflow failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("PyTorch detection failed: ${e.message}")
            } finally {
                _detectionState.update {
                    it.copy(isDetecting = false)
                }
            }
        }
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
}
