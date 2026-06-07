package com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel

import androidx.lifecycle.ViewModel
import com.nkwabyte.cropdiseasedetection.common.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProfileViewModel: ViewModel() {
    private val _userData = UserProfile(
        userId = "user123",
        userName = "Jane Doe",
        successRate = "0%",
        detections = "0",
        profileImage = "",
        userPhone = null,
        userEmail = null,
        userAddress = null,
        userBio = null,
        userLocation = null,
        userCrops = emptyList()
    )
    private val _userProfileState = MutableStateFlow(_userData)
    val userProfileState = _userProfileState.asStateFlow()

    fun setSelectedProfile(profile: UserProfile) {
        _userProfileState.update { currentState ->
            currentState.copy(
                userId = profile.userId,
                userName = profile.userName,
                successRate = profile.successRate,
                detections = profile.detections,
                profileImage = profile.profileImage,
                userPhone = profile.userPhone,
                userEmail = profile.userEmail,
                userAddress = profile.userAddress,
                userBio = profile.userBio,
                userLocation = profile.userLocation,
                userCrops = profile.userCrops
            )
        }
    }

    fun updateProfile(
        userId: String = _userProfileState.value.userId,
        userName: String = _userProfileState.value.userName,
        successRate: String = _userProfileState.value.successRate,
        detections: String = _userProfileState.value.detections,
        profileImage: String = _userProfileState.value.profileImage,
        userPhone: String? = _userProfileState.value.userPhone,
        userEmail: String? = _userProfileState.value.userEmail,
        userAddress: String? = _userProfileState.value.userAddress,
        userBio: String? = _userProfileState.value.userBio,
        userLocation: String? = _userProfileState.value.userLocation,
        userCrops: List<String> = _userProfileState.value.userCrops
    ) {
        _userProfileState.update { currentState ->
            currentState.copy(
                userId = userId,
                userName = userName,
                successRate = successRate,
                detections = detections,
                profileImage = profileImage,
                userPhone = userPhone,
                userEmail = userEmail,
                userAddress = userAddress,
                userBio = userBio,
                userLocation = userLocation,
                userCrops = userCrops
            )
        }
    }

    fun addCrop(crop: String) {
        _userProfileState.update { currentState ->
            currentState.copy(
                userCrops = currentState.userCrops + crop
            )
        }
    }

    fun resetProfile() {
        _userProfileState.update {
            UserProfile(
                userId = "user123",
                userName = "Jane Doe",
                successRate = "0%",
                detections = "0",
                profileImage = "",
                userPhone = null,
                userEmail = null,
                userAddress = null,
                userBio = null,
                userLocation = null,
                userCrops = emptyList()
            )
        }
    }
}
