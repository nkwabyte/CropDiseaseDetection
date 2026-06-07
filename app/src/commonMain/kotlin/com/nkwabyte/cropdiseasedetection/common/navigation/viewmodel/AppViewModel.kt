package com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel

import androidx.lifecycle.ViewModel
import com.nkwabyte.cropdiseasedetection.common.model.AppData
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppViewModel: ViewModel() {
    private val _appData = AppData(
        selectedCrop = null,
        selectedImageByteArray = null,
        detectionResult = emptyList(),
        isLoading = false,
        errorMessage = null
    )
    private val _appState = MutableStateFlow(_appData)
    val appState: StateFlow<AppData> = _appState.asStateFlow()

    init {
        println("AppViewModel initialized with default state: $_appData")
        reset()
    }

    fun setSelectedCrop(crop: String) {
        _appState.update { currentState ->
            currentState.copy(
                selectedCrop = crop,
                selectedImageByteArray = null,
                detectionResult = emptyList()
            )
        }
    }
    fun setDetectionResult(result: List<DetectionResult>) {
        _appState.update { currentState ->
            currentState.copy(
                detectionResult = result,
                isLoading = false,
                errorMessage = null
            )
        }
    }
    fun setSelectedImageByteArray(byteArray: ByteArray?) {
        _appState.update { currentState ->
            currentState.copy(
                selectedImageByteArray = byteArray,
                detectionResult = emptyList(),
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun clearSelectedImageByteArray() {
        _appState.update { currentState ->
            currentState.copy(
                selectedImageByteArray = null,
                detectionResult = emptyList(),
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun setLoading(isLoading: Boolean) {
        _appState.update { currentState ->
            currentState.copy(
                isLoading = isLoading,
                errorMessage = null // Reset error message when loading starts
            )
        }
    }
    fun setErrorMessage(errorMessage: String?) {
        _appState.update { currentState ->
            currentState.copy(
                errorMessage = errorMessage,
                isLoading = false // Reset loading state when an error occurs
            )
        }
    }
    fun reset() {
        _appState.update {
            AppData(
                selectedCrop = null,
                selectedImageByteArray = null,
                detectionResult = emptyList(),
                isLoading = false,
                errorMessage = null
            )
        }
    }
}
