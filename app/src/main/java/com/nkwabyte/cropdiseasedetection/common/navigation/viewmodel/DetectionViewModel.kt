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
                _detectionState.update { currentState ->
                    currentState.copy(
                        isModelLoading = false,
                        isDetecting = false,
                    )
                }
            } catch (e: Exception) {
                Log.e("DetectionViewModel", "Model loading failed: ${e.message}")
            }
        }
    }

    fun detectWithPyTorch(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // set is detecting to true
                _detectionState.update { currentState ->
                    currentState.copy(isDetecting = true)
                }
                val results = pytorchDetector.detect(bitmap)
                // log the detection results
                Log.d("DetectionViewModel", "PyTorch detection results: $results")
                _detectionState.update { currentState ->
                    currentState.copy(
                        results = results,
                        isDetecting = false,
                        imageWidth = bitmap.width,
                        imageHeight = bitmap.height,
                    )
                }
            } catch (e: Exception) {
                Log.e("DetectionViewModel", "PyTorch detection failed: ${e.message}")
            } finally {
                _detectionState.update { currentState ->
                    currentState.copy(
                        isDetecting = false
                    )
                }
            }
        }
    }

    fun detectWithTF(input: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _detectionState.update { currentState ->
                    currentState.copy(isDetecting = true)
                }
                val results = tfDetector.detect(input)
                Log.d("DetectionViewModel", "PyTorch detection results: $results")
                _detectionState.update { currentState ->
                    currentState.copy(
                        results = results,
                        isDetecting = false,
                        imageWidth = input.width,
                        imageHeight = input.height,
                    )
                }
            } catch (e: Exception) {
                Log.e("DetectionViewModel", "TFLite detection failed: ${e.message}")
            } finally {
                _detectionState.update { currentState ->
                    currentState.copy(
                        isDetecting = false
                    )
                }
            }
        }
    }

    fun setDetectionResult(results: List<DetectionResult>) {
        _detectionState.update { currentState ->
            currentState.copy(
                results = results,
            )
        }
    }

    fun addDetectionResult(result: DetectionResult) {
        _detectionState.update { currentState ->
            currentState.copy(
                results = currentState.results + result,
            )
        }
    }

    fun clearDetectionResults() {
        _detectionState.update { currentState ->
            currentState.copy(
                results = emptyList(),
                isDetecting = false,
            )
        }
    }

    fun removeDetectionResult(result: DetectionResult) {
        _detectionState.update { currentState ->
            currentState.copy(
                results = currentState.results.filter { it != result }
            )
        }
    }

    fun reset() {
        _detectionState.update {
            DetectionData(
                isModelLoading = true,
                isDetecting = false,
                results = emptyList(),
                imageWidth = 0,
                imageHeight = 0
            )
        }
    }
}
