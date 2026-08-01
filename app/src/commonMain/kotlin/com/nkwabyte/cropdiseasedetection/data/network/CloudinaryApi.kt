package com.nkwabyte.cropdiseasedetection.data.network

import com.nkwabyte.cropdiseasedetection.BuildKonfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.date.GMTDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.kotlincrypto.hash.sha1.SHA1

@Serializable
data class CloudinaryUploadResponse(
    val secure_url: String? = null,
    val public_id: String? = null,
    val error: CloudinaryError? = null
)

@Serializable
data class CloudinaryError(val message: String)

class CloudinaryApi {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun uploadImage(imageBytes: ByteArray, folder: String? = null): String? {
        val apiKey = BuildKonfig.CLOUDINARY_API_KEY
        val apiSecret = BuildKonfig.CLOUDINARY_API_SECRET
        val cloudName = BuildKonfig.CLOUDINARY_CLOUD_NAME.ifBlank { "dxdun6eym" }
        val uploadPreset = "crop_diseases"

        if (apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            val signedResult = trySignedUpload(imageBytes, cloudName, apiKey, apiSecret, folder)
            if (signedResult != null) return signedResult
        }

        // Fallback to unsigned upload
        return tryUnsignedUpload(imageBytes, cloudName, uploadPreset, folder)
    }

    private suspend fun trySignedUpload(
        imageBytes: ByteArray,
        cloudName: String,
        apiKey: String,
        apiSecret: String,
        folder: String?
    ): String? {
        try {
            val timestamp = (GMTDate().timestamp / 1000L).toString()
            val stringToSign = if (folder != null) {
                "folder=$folder&timestamp=$timestamp$apiSecret"
            } else {
                "timestamp=$timestamp$apiSecret"
            }

            val digest = SHA1().digest(stringToSign.encodeToByteArray())
            val signature = digest.joinToString("") { b ->
                (b.toInt() and 0xff).toString(16).padStart(2, '0')
            }

            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload",
                formData = formData {
                    append("file", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"upload.jpg\"")
                    })
                    append("api_key", apiKey)
                    append("timestamp", timestamp)
                    append("signature", signature)
                    if (folder != null) append("folder", folder)
                }
            )

            val uploadResponse = response.body<CloudinaryUploadResponse>()
            if (uploadResponse.error != null) {
                println("Cloudinary signed error: ${uploadResponse.error.message}")
                return null
            }
            return uploadResponse.secure_url
        } catch (e: Exception) {
            println("Cloudinary signed upload failed: ${e.message}")
            return null
        }
    }

    private suspend fun tryUnsignedUpload(
        imageBytes: ByteArray,
        cloudName: String,
        preset: String,
        folder: String?
    ): String? {
        try {
            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload",
                formData = formData {
                    append("file", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"upload.jpg\"")
                    })
                    append("upload_preset", preset)
                    if (folder != null) append("folder", folder)
                }
            )

            val uploadResponse = response.body<CloudinaryUploadResponse>()
            if (uploadResponse.error != null) {
                println("Cloudinary unsigned error: ${uploadResponse.error.message}")
                return null
            }
            return uploadResponse.secure_url
        } catch (e: Exception) {
            println("Cloudinary unsigned upload failed: ${e.message}")
            return null
        }
    }
}
