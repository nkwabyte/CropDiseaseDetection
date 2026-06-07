package com.nkwabyte.cropdiseasedetection.common.model
import kotlinx.serialization.Serializable

@Serializable()
data class UserProfile(
    val userId: String = "user123",
    val userName: String = "JANE DOE",
    val successRate: String = "84%",
    val detections: String = "02",
    val profileImage: String = "https://picsum.photos/200",
    val userPhone: String? = null,
    val userEmail: String? = null,
    val userAddress: String? = null,
    val userBio: String? = null,
    val userLocation: String? = null,
    val userCrops: List<String> = emptyList(),
){
    fun isValid(): Boolean {
        return userId.isNotEmpty() && userName.isNotEmpty()
    }

    fun isNoCrops(): Boolean {
        return userCrops.isEmpty()
    }
}