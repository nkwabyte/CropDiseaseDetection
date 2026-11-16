package com.nkwabyte.cropdiseasedetection.common.models

import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelCatalog
import com.nkwabyte.cropdiseasedetection.common.model.DetectionModelSpec
import com.nkwabyte.cropdiseasedetection.common.model.ModelHosting
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where a downloadable model stands on this device. */
sealed class ModelDownloadState {
    data object NotDownloaded : ModelDownloadState()

    /** [totalBytes] is null when the server sends no Content-Length. */
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long?) : ModelDownloadState() {
        val fraction: Float?
            get() = totalBytes?.takeIf { it > 0 }?.let { (downloadedBytes.toDouble() / it).toFloat().coerceIn(0f, 1f) }
    }

    data object Downloaded : ModelDownloadState()

    data class Failed(val message: String) : ModelDownloadState()
}

/**
 * Snapshot of every downloadable model's state. A class rather than a bare map so the
 * flow exposes an object type to Swift (SKIE's `SkieSwiftStateFlow` needs one).
 */
data class ModelDownloads(val states: Map<String, ModelDownloadState> = emptyMap()) {
    fun stateOf(modelId: String): ModelDownloadState = states[modelId] ?: ModelDownloadState.NotDownloaded
}

/**
 * Fetches detector weights that are too large to ship in the app and keeps track of
 * which are on the device. Shared by the Android Compose UI and the iOS SwiftUI app.
 */
class ModelDownloadManager(private val store: ModelFileStore) {
    private val client = HttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = mutableMapOf<String, Job>()

    private val _downloads = MutableStateFlow(
        ModelDownloads(
            DetectionModelCatalog.all.filter { it.requiresDownload }.associate { spec ->
                spec.id to if (store.exists(spec.assetName)) ModelDownloadState.Downloaded else ModelDownloadState.NotDownloaded
            },
        ),
    )
    val downloads: StateFlow<ModelDownloads> = _downloads.asStateFlow()

    /** Bundled models are always ready; downloadable ones once their file is on disk. */
    fun isReady(spec: DetectionModelSpec): Boolean =
        !spec.requiresDownload || store.exists(spec.assetName)

    fun isReady(modelId: String): Boolean = isReady(DetectionModelCatalog.byId(modelId))

    /** Absolute path of a downloaded model's weights, or null if it is bundled or missing. */
    fun localPath(spec: DetectionModelSpec): String? =
        if (spec.requiresDownload && store.exists(spec.assetName)) store.path(spec.assetName) else null

    /**
     * The model to actually load for a stored preference: the chosen one if its weights
     * are present, otherwise the bundled default, so the app is never left without one.
     */
    fun resolve(modelId: String?): DetectionModelSpec =
        DetectionModelCatalog.byId(modelId).takeIf(::isReady) ?: DetectionModelCatalog.default

    /** Starts (or keeps running) the download; [onDownloaded] fires once it is on disk. */
    fun download(modelId: String, onDownloaded: () -> Unit = {}) {
        val spec = DetectionModelCatalog.byId(modelId)
        val url = spec.downloadUrl ?: return
        if (!ModelHosting.DOWNLOADS_ENABLED) return
        if (jobs[spec.id]?.isActive == true) return
        if (store.exists(spec.assetName)) {
            setState(spec.id, ModelDownloadState.Downloaded)
            onDownloaded()
            return
        }

        jobs[spec.id] = scope.launch {
            val partName = "${spec.assetName}.part"
            setState(spec.id, ModelDownloadState.Downloading(0, null))
            try {
                client.prepareGet(url).execute { response ->
                    if (response.status.value == 404) {
                        throw IllegalStateException("this model is not available for download yet")
                    }
                    if (!response.status.isSuccess()) {
                        throw IllegalStateException("server responded ${response.status.value}")
                    }
                    val total = response.contentLength()
                    val channel = response.bodyAsChannel()
                    val writer = store.openWriter(partName)
                    try {
                        val buffer = ByteArray(256 * 1024)
                        var downloaded = 0L
                        var reported = 0L
                        while (true) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read == -1) break
                            if (read == 0) continue
                            writer.write(buffer, read)
                            downloaded += read
                            if (downloaded - reported >= 512 * 1024) {
                                reported = downloaded
                                setState(spec.id, ModelDownloadState.Downloading(downloaded, total))
                            }
                        }
                        if (total != null && downloaded != total) {
                            throw IllegalStateException("Download incomplete ($downloaded of $total bytes)")
                        }
                    } finally {
                        writer.close()
                    }
                }
                check(store.moveIntoPlace(partName, spec.assetName)) { "Could not save the model file" }
                setState(spec.id, ModelDownloadState.Downloaded)
                onDownloaded()
            } catch (e: CancellationException) {
                store.delete(partName)
                setState(spec.id, ModelDownloadState.NotDownloaded)
                throw e
            } catch (e: Throwable) {
                store.delete(partName)
                setState(spec.id, ModelDownloadState.Failed(e.message ?: "Download failed"))
            }
        }
    }

    fun cancel(modelId: String) {
        jobs.remove(modelId)?.cancel()
    }

    /** Removes downloaded weights to free space; the model can be downloaded again later. */
    fun delete(modelId: String) {
        val spec = DetectionModelCatalog.byId(modelId)
        if (!spec.requiresDownload) return
        cancel(spec.id)
        store.delete(spec.assetName)
        setState(spec.id, ModelDownloadState.NotDownloaded)
    }

    /** Clears a failure so the picker shows the model as downloadable again. */
    fun dismissError(modelId: String) {
        if (_downloads.value.stateOf(modelId) is ModelDownloadState.Failed) {
            setState(modelId, ModelDownloadState.NotDownloaded)
        }
    }

    private fun setState(modelId: String, state: ModelDownloadState) {
        _downloads.update { it.copy(states = it.states + (modelId to state)) }
    }
}

/** Writes bytes to a file being downloaded. */
interface ModelFileWriter {
    fun write(bytes: ByteArray, length: Int)
    fun close()
}

/** Platform storage for downloaded model weights (app-private, not backed up). */
expect class ModelFileStore() {
    fun path(fileName: String): String
    fun exists(fileName: String): Boolean
    fun openWriter(fileName: String): ModelFileWriter
    fun moveIntoPlace(from: String, to: String): Boolean
    fun delete(fileName: String)
}
