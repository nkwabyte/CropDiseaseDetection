package com.nkwabyte.cropdiseasedetection.model
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val userId: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val profile: String? = null,
    val bio: String? = null,
    val reports: List<DetectionReports> = emptyList<DetectionReports>()
)

@Serializable
data class DetectionReports(
    val reports: List<DetectionResult> = emptyList()
)

// list of sample user profiles
val sampleUserProfiles = listOf(
    UserProfile(
        userId = "user123",
        name = "John Doe",
        email = "jane@email.com",
        phone = "+1234567890",
        profile = "https://example.com/profile.jpg",
        bio = "Agriculture enthusiast and crop disease researcher."
    ),
    UserProfile(
        userId = "user456",
        name = "Alice Smith",
        email = "sample@email.com",
        phone = "+0987654321",
        profile = "https://example.com/profile2.jpg",
        bio = "Expert in sustainable farming practices."
    ),
)