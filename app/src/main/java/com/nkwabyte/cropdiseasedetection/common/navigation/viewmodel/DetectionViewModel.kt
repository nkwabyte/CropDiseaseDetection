package com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkwabyte.cropdiseasedetection.common.helpers.PyTorchObjectDetector
import com.nkwabyte.cropdiseasedetection.common.helpers.TFObjectDetector
import com.nkwabyte.cropdiseasedetection.common.model.DetectionData
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DetectionViewModel(
    private val pytorchDetector: PyTorchObjectDetector,
    private val tfDetector: TFObjectDetector
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                pytorchDetector.loadModel()
                tfDetector.loadModel()
                _detectionState.update {
                    it.copy(
                        isModelLoading = false,
                        isDetectionSuccessful = false,
                        isDetected = false,
                        isDetecting = false,
                    )
                }
            } catch (e: Exception) {
                Log.e("DetectionViewModel", "Model loading failed: ${e.message}")
            }
        }
    }

    fun detectWithPyTorch(bitmap: Bitmap, crop: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _detectionState.update { it.copy(isDetecting = true) }

                val results = pytorchDetector.detect(bitmap)
                Log.d("DetectionViewModel", "PyTorch detection results: $results")

                if (results.isEmpty()) {
                    Log.d("DetectionViewModel", "No detection results found")
                    // No detection made at all
                    _detectionState.update {
                        it.copy(
                            results = emptyList(),
                            isDetecting = false,
                            isDetected = true,
                            isDetectionSuccessful = false,
                            isCropMissMatch = false,
                            imageWidth = bitmap.width,
                            imageHeight = bitmap.height,
                        )
                    }
                    return@launch
                }

                val matchingResults = results.filter {
                    it.className?.contains(crop, ignoreCase = true) == true
                }
                Log.d("DetectionViewModel", "Matching results: $matchingResults")

                val isMismatch = matchingResults.isEmpty()
                Log.d("DetectionViewModel", "Crop mismatch: $isMismatch")

                _detectionState.update {
                    it.copy(
                        results = matchingResults,
                        isDetecting = false,
                        isDetected = true,
                        isDetectionSuccessful = matchingResults.isNotEmpty(),
                        isCropMissMatch = isMismatch,
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height,
                    )
                }
            } catch (e: Exception) {
                Log.e("DetectionViewModel", "PyTorch detection failed: ${e.message}")
            } finally {
                _detectionState.update {
                    it.copy(isDetecting = false)
                }
            }
        }
    }

    fun detectWithTF(input: Bitmap, crop: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _detectionState.update { it.copy(isDetecting = true) }

                val results = tfDetector.detect(input)
                Log.d("DetectionViewModel", "TF detection results: $results")

                val matchingResults = results.filter {
                    it.className?.contains(crop, ignoreCase = true) == true
                }

                val isMismatch = matchingResults.isEmpty()

                _detectionState.update {
                    it.copy(
                        results = matchingResults,
                        isDetecting = false,
                        isDetected = true,
                        isDetectionSuccessful = matchingResults.isNotEmpty(),
                        isCropMissMatch = isMismatch,
                        imageWidth = input.width,
                        imageHeight = input.height,
                    )
                }
            } catch (e: Exception) {
                Log.e("DetectionViewModel", "TF detection failed: ${e.message}")
            } finally {
                _detectionState.update {
                    it.copy(
                        isDetecting = false,
                    )
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
