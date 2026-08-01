package com.nkwabyte.cropdiseasedetection.data.network

import com.nkwabyte.cropdiseasedetection.BuildKonfig
import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
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
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun uploadImage(imageBytes: ByteArray, folder: String? = null): String? {
        val apiKey = BuildKonfig.CLOUDINARY_API_KEY.ifBlank { FALLBACK_API_KEY }
        val apiSecret = BuildKonfig.CLOUDINARY_API_SECRET.ifBlank { FALLBACK_API_SECRET }
        val cloudName = BuildKonfig.CLOUDINARY_CLOUD_NAME.ifBlank { FALLBACK_CLOUD_NAME }

        if (apiKey.isNotBlank() && apiSecret.isNotBlank()) {
            val signedResult = trySignedUpload(imageBytes, cloudName, apiKey, apiSecret, folder)
            if (signedResult != null) return signedResult
        }

        // Fallback to unsigned upload
        return tryUnsignedUpload(imageBytes, cloudName, UPLOAD_PRESET, folder)
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

            val fields = buildMap {
                put("api_key", apiKey)
                put("timestamp", timestamp)
                put("signature", signature)
                if (folder != null) put("folder", folder)
            }

            return upload(cloudName, imageBytes, fields, "signed")
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
            val fields = buildMap {
                put("upload_preset", preset)
                if (folder != null) put("folder", folder)
            }

            return upload(cloudName, imageBytes, fields, "unsigned")
        } catch (e: Exception) {
            println("Cloudinary unsigned upload failed: ${e.message}")
            return null
        }
    }

    private suspend fun upload(
        cloudName: String,
        imageBytes: ByteArray,
        fields: Map<String, String>,
        mode: String
    ): String? {
        val parts = buildList {
            add(filePart(imageBytes))
            fields.forEach { (name, value) -> add(textPart(name, value)) }
        }

        val response = client.submitFormWithBinaryData(
            url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload",
            formData = parts
        )

        // Cloudinary answers with JSON on both success and failure, but read it as text
        // first so an unexpected (e.g. HTML error page) body still shows up in the log.
        val body = response.bodyAsText()
        val uploadResponse = runCatching {
            json.decodeFromString<CloudinaryUploadResponse>(body)
        }.getOrNull()

        if (uploadResponse?.secure_url != null) return uploadResponse.secure_url

        val reason = uploadResponse?.error?.message ?: body.take(300)
        println("Cloudinary $mode error (${response.status}): $reason")
        return null
    }

    /**
     * Cloudinary's multipart parser only recognises parts whose `Content-Disposition`
     * spells the name with quotes (`name="api_key"`). Ktor's `formData {}` builder leaves
     * plain keys unquoted, so every non-file field was silently dropped and the upload
     * arrived as an anonymous unsigned one — hence "Upload preset must be specified when
     * using unsigned upload" even on the signed path. Build the parts by hand to keep the
     * quotes.
     */
    private fun textPart(name: String, value: String): PartData.FormItem = PartData.FormItem(
        value,
        {},
        Headers.build {
            append(HttpHeaders.ContentDisposition, "form-data; name=\"$name\"")
        }
    )

    private fun filePart(imageBytes: ByteArray): PartData.BinaryItem = PartData.BinaryItem(
        { buildPacket { writeFully(imageBytes) } },
        {},
        Headers.build {
            append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"upload.jpg\"")
            append(HttpHeaders.ContentType, "image/jpeg")
            // Lets MultiPartFormDataContent compute a Content-Length instead of falling
            // back to chunked encoding.
            append(HttpHeaders.ContentLength, imageBytes.size.toString())
        }
    )

    private companion object {
        const val FALLBACK_API_KEY = "919859216574648"
        const val FALLBACK_API_SECRET = "I8xYSkICFPtCaqDStUDutFQhrX0"
        const val FALLBACK_CLOUD_NAME = "dxdun6eym"

        // Only used when no API key/secret is available. Must exist in the Cloudinary
        // console under Settings > Upload > Upload presets, with signing mode "Unsigned".
        const val UPLOAD_PRESET = "crop_diseases"
    }
}
