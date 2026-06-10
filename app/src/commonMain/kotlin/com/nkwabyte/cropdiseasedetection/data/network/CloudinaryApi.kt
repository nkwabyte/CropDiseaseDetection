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
        try {
            val timestamp = (GMTDate().timestamp / 1000L).toString()
            val apiSecret = BuildKonfig.CLOUDINARY_API_SECRET

            // Cloudinary signing: params sorted alphabetically (excluding file, api_key, resource_type, cloud_name)
            val stringToSign = if (folder != null) {
                "folder=$folder&timestamp=$timestamp$apiSecret"
            } else {
                "timestamp=$timestamp$apiSecret"
            }

            val signature = SHA1().digest(stringToSign.encodeToByteArray()).joinToString("") { byte ->
                val hex = "0123456789abcdef"
                "${hex[(byte.toInt() shr 4) and 0x0f]}${hex[byte.toInt() and 0x0f]}"
            }

            val response: HttpResponse = client.submitFormWithBinaryData(
                url = "https://api.cloudinary.com/v1_1/${BuildKonfig.CLOUDINARY_CLOUD_NAME}/image/upload",
                formData = formData {
                    append("file", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"upload.jpg\"")
                    })
                    append("api_key", BuildKonfig.CLOUDINARY_API_KEY)
                    append("timestamp", timestamp)
                    append("signature", signature)
                    if (folder != null) append("folder", folder)
                }
            )

            val uploadResponse = response.body<CloudinaryUploadResponse>()
            if (uploadResponse.error != null) {
                println("Cloudinary error: ${uploadResponse.error.message}")
            }
            return uploadResponse.secure_url
        } catch (e: Exception) {
            println("Cloudinary upload failed: ${e.message}")
            return null
        }
    }
}
