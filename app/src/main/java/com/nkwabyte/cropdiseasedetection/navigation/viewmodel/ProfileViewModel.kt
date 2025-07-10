package com.nkwabyte.cropdiseasedetection.navigation.viewmodel

import androidx.lifecycle.ViewModel
import com.nkwabyte.cropdiseasedetection.model.sampleUserProfiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : ViewModel() {
    private val userId = "user123"
    val userName = "JANE DOE"
    val successRate = "84%"
    val detections = "02"

    private val _userProfileState = MutableStateFlow(
        sampleUserProfiles.first {
            it.userId == userId
        }
    )
    val userProfileState = _userProfileState.asStateFlow()
}
