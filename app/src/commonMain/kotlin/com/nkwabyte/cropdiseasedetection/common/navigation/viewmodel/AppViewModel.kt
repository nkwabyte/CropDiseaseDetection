package com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkwabyte.cropdiseasedetection.common.model.AppData
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.common.model.UserRole
import com.nkwabyte.cropdiseasedetection.common.utils.SettingsManager
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(
    private val settingsManager: SettingsManager,
    private val syncRepository: SyncRepository
) : ViewModel() {
    private val _appState = MutableStateFlow(
        AppData(
            selectedCrop = null,
            selectedImageByteArray = null,
            detectionResult = emptyList(),
            isLoading = false,
            errorMessage = null,
            selectedTheme = settingsManager.getTheme(),
            classifierThreshold = settingsManager.getClassifierThreshold(),
            iouThreshold = settingsManager.getIouThreshold(),
            detectionThreshold = settingsManager.getDetectionThreshold()
        )
    )
    val appState: StateFlow<AppData> = _appState.asStateFlow()

    init {
        println("AppViewModel initialized with state: ${_appState.value}")
    }

    fun loadUserRole() {
        viewModelScope.launch {
            val role = syncRepository.getUserRole()
            _appState.update { it.copy(userRole = role) }
        }
    }

    fun setUserRole(role: UserRole) {
        _appState.update { it.copy(userRole = role) }
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
                errorMessage = null
            )
        }
    }

    fun setErrorMessage(errorMessage: String?) {
        _appState.update { currentState ->
            currentState.copy(
                errorMessage = errorMessage,
                isLoading = false
            )
        }
    }

    fun setSelectedTheme(theme: String) {
        settingsManager.setTheme(theme)
        _appState.update { currentState ->
            currentState.copy(selectedTheme = theme)
        }
    }

    fun setClassifierThreshold(value: Float) {
        settingsManager.setClassifierThreshold(value)
        _appState.update { it.copy(classifierThreshold = value) }
    }

    fun setIouThreshold(value: Float) {
        settingsManager.setIouThreshold(value)
        _appState.update { it.copy(iouThreshold = value) }
    }

    fun setDetectionThreshold(value: Float) {
        settingsManager.setDetectionThreshold(value)
        _appState.update { it.copy(detectionThreshold = value) }
    }

    fun reset() {
        _appState.update { currentState ->
            AppData(
                selectedCrop = null,
                selectedImageByteArray = null,
                detectionResult = emptyList(),
                isLoading = false,
                errorMessage = null,
                selectedTheme = currentState.selectedTheme,
                classifierThreshold = currentState.classifierThreshold,
                iouThreshold = currentState.iouThreshold,
                detectionThreshold = currentState.detectionThreshold,
                userRole = currentState.userRole
            )
        }
    }
}
