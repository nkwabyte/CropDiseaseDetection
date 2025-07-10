package com.nkwabyte.cropdiseasedetection.navigation.viewmodel

import androidx.lifecycle.ViewModel
import com.nkwabyte.cropdiseasedetection.model.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DetectionResultViewModel(
    private val detectionResult: List<DetectionResult>,
): ViewModel() {
    private val _detectionResultState = MutableStateFlow(detectionResult)
    val detectionResultState = _detectionResultState.asStateFlow()
}