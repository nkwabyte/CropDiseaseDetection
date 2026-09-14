package com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nkwabyte.cropdiseasedetection.common.helpers.ObjectDetector
import com.nkwabyte.cropdiseasedetection.common.model.DetectionData
import com.nkwabyte.cropdiseasedetection.common.model.DetectionResult
import com.nkwabyte.cropdiseasedetection.data.network.CloudinaryApi
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import com.nkwabyte.cropdiseasedetection.common.pipeline.DetectionPipeline
import com.nkwabyte.cropdiseasedetection.common.pipeline.DetectionStateReducer
import com.nkwabyte.cropdiseasedetection.common.pipeline.ObjectDetectorEngine
import com.nkwabyte.cropdiseasedetection.common.pipeline.PipelineOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class FlagState {
    object Idle : FlagState()
    object Loading : FlagState()
    data class Success(val message: String = "Detection flagged successfully") : FlagState()
    data class Error(val message: String) : FlagState()
}

class DetectionViewModel(
    private val detector: ObjectDetector,
    private val cloudinaryApi: CloudinaryApi,
    private val syncRepository: SyncRepository
) : ViewModel() {

    /** The shared production pipeline — the same object the extended benchmark
     *  drives, so the app and the measurements can never describe different
     *  architectures. */
    private val pipeline = DetectionPipeline(ObjectDetectorEngine(detector))

    /** Guards against a second request starting while one is in flight. */
    private var detectJob: Job? = null

    private val detectionData = DetectionData(
        isModelLoading = true,
        isDetecting = false,
        isDetectionSuccessful = false,
        results = emptyList(),
        imageWidth = 0,
        imageHeight = 0
    )

    private val _detectionState = MutableStateFlow(detectionData)
    val detectionState = _detectionState.asStateFlow()

    private val _flagState = MutableStateFlow<FlagState>(FlagState.Idle)
    val flagState = _flagState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                detector.loadModel()
                detector.loadClassifierModel()
                _detectionState.update {
                    it.copy(
                        isModelLoading = false,
                        isDetectionSuccessful = false,
                        isDetected = false,
                        isDetecting = false,
                    )
                }
            } catch (e: Exception) {
                println("Model loading failed: ${e.message}")
            }
        }
    }

    /**
     * Runs one two-stage request: classify, route, and only then detect.
     *
     * The call order is the point. This used to run the detector first and use
     * the classifier afterwards purely as a display flag, filtering results with
     * `className.contains(selectedCrop)` — which paid for a full detector pass on
     * every out-of-distribution photo, and which silently matched nothing
     * whenever the app was not in English, because the selected crop arrived as a
     * translated display string. The decision now lives in [DetectionPipeline] /
     * `CropRoutingPolicy`, is made from the classifier's verdict, and filters on
     * detector class ids.
     *
     * @param selectedCropId the user's crop as a CANONICAL id
     *        ([com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop]),
     *        never a localized label. Blank means "no preference", in which case
     *        the classifier's own crop is used as the route.
     */
    fun detect(imageBytes: ByteArray, selectedCropId: String) {
        // A recomposition or a double-tap must not start a second inference pass
        // over models that are neither reentrant nor cheap. The in-flight job is
        // the guard; the request completes exactly once.
        if (detectJob?.isActive == true) {
            println("Detection already in flight; ignoring duplicate request")
            return
        }
        detectJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                // Clears the previous run's results, label and confidence, so a
                // slow request never shows stale findings while it runs.
                _detectionState.update { DetectionStateReducer.starting(it) }

                val result = pipeline.run(imageBytes, selectedCropId)
                println(
                    "Pipeline outcome=${result.outcome} " +
                        "classifier=${result.classification?.label}@${result.classification?.confidence} " +
                        "detectorExecuted=${result.detectorExecuted} " +
                        "raw=${result.rawResults.size} routed=${result.routedResults.size}"
                )

                _detectionState.update { DetectionStateReducer.from(result, selectedCropId) }

                // Only the truthful terminal result is persisted: a rejected or
                // mismatched scan syncs with empty detections, never with
                // fabricated detector output.
                syncDetection(
                    imageBytes = imageBytes,
                    crop = result.routedCrop?.canonicalLabel
                        ?: selectedCropId.ifBlank { result.classification?.label ?: "Unknown" },
                    width = result.boxCoordinateSpace,
                    height = result.boxCoordinateSpace,
                    matchingResults = result.routedResults,
                    rawResults = result.rawResults,
                    detectionSuccessful = result.isSuccessful,
                    isCropMismatch = result.outcome == PipelineOutcome.CROP_MISMATCH ||
                        result.outcome == PipelineOutcome.REJECTED_OUT_OF_DISTRIBUTION,
                )
            } catch (e: Exception) {
                println("Detection pipeline failed: ${e.message}")
                _detectionState.update {
                    it.copy(
                        isDetecting = false,
                        isDetected = true,
                        isDetectionSuccessful = false,
                        isInferenceError = true,
                        results = emptyList(),
                        outcome = PipelineOutcome.DETECTOR_ERROR.name,
                        errorMessage = e.message,
                    )
                }
            } finally {
                // Belt and braces: every path above already publishes a terminal
                // state, but a cancellation must not leave the UI spinning.
                _detectionState.update { it.copy(isDetecting = false) }
            }
        }
    }

    /**
     * Persists a scan to Firestore. Called on every terminal path of [detect] — including
     * the ones with no findings — so the history page mirrors what the user actually ran.
     * Runs fire-and-forget: the UI state is already published by the time we get here.
     */
    private fun syncDetection(
        imageBytes: ByteArray,
        crop: String,
        width: Int,
        height: Int,
        matchingResults: List<DetectionResult>,
        rawResults: List<DetectionResult>,
        detectionSuccessful: Boolean,
        isCropMismatch: Boolean
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val imageUrl = cloudinaryApi.uploadImage(imageBytes)
                if (imageUrl != null) {
                    syncRepository.saveDetectionRecord(
                        cropName = crop,
                        imageUrl = imageUrl,
                        detectionSuccessful = detectionSuccessful,
                        isCropMismatch = isCropMismatch,
                        imageWidth = width,
                        imageHeight = height,
                        matchingResults = matchingResults,
                        rawResults = rawResults,
                        modelName = MODEL_NAME,
                        modelVersion = MODEL_VERSION,
                        platform = PLATFORM
                    )
                } else {
                    syncRepository.queuePendingDetectionRecord(
                        imageBytes = imageBytes,
                        cropName = crop,
                        detectionSuccessful = detectionSuccessful,
                        isCropMismatch = isCropMismatch,
                        imageWidth = width,
                        imageHeight = height,
                        matchingResults = matchingResults,
                        rawResults = rawResults,
                        modelName = MODEL_NAME,
                        modelVersion = MODEL_VERSION,
                        platform = PLATFORM
                    )
                }
                syncRepository.processPendingQueue(cloudinaryApi)
            } catch (e: Exception) {
                println("Sync workflow failed: ${e.message}")
            }
        }
    }

    fun flagDetection(
        imageBytes: ByteArray,
        cropName: String,
        userRole: String,
        notes: String?,
        detectionThreshold: Float,
        iouThreshold: Float,
        classifierThreshold: Float
    ) {
        val state = _detectionState.value
        _flagState.value = FlagState.Loading
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val imageUrl = cloudinaryApi.uploadImage(imageBytes, folder = "flagged")
                if (imageUrl != null) {
                    syncRepository.saveFlaggedRecord(
                        imageUrl = imageUrl,
                        cropName = cropName,
                        userRole = userRole,
                        detectionResults = state.results,
                        classificationLabel = state.classificationLabel,
                        classifierConfidence = state.classifierConfidence,
                        imageWidth = state.imageWidth ?: 0,
                        imageHeight = state.imageHeight ?: 0,
                        notes = notes,
                        platform = PLATFORM,
                        modelName = MODEL_NAME,
                        detectionThreshold = detectionThreshold,
                        iouThreshold = iouThreshold,
                        classifierThreshold = classifierThreshold
                    )
                } else {
                    syncRepository.queuePendingFlaggedRecord(
                        imageBytes = imageBytes,
                        cropName = cropName,
                        userRole = userRole,
                        detectionResults = state.results,
                        classificationLabel = state.classificationLabel,
                        classifierConfidence = state.classifierConfidence,
                        imageWidth = state.imageWidth ?: 0,
                        imageHeight = state.imageHeight ?: 0,
                        notes = notes,
                        platform = PLATFORM,
                        modelName = MODEL_NAME,
                        detectionThreshold = detectionThreshold,
                        iouThreshold = iouThreshold,
                        classifierThreshold = classifierThreshold
                    )
                }
                syncRepository.processPendingQueue(cloudinaryApi)
                _flagState.value = FlagState.Success()
            } catch (e: Exception) {
                println("Flag workflow failed: ${e.message}")
                _flagState.value = FlagState.Error(e.message ?: "Flag submission failed")
            }
        }
    }

    fun resetFlagState() {
        _flagState.value = FlagState.Idle
    }

    fun reset() {
        detectJob?.cancel()
        detectJob = null
        _detectionState.update {
            DetectionData(
                isModelLoading = true,
                isDetecting = false,
                isDetectionSuccessful = false,
                isCropMissMatch = false,
                isDetected = false,
                results = emptyList(),
                imageWidth = 0,
                imageHeight = 0
            )
        }
    }

    private companion object {
        const val MODEL_NAME = "ExecuTorch (PyTorch Mobile)"
        const val MODEL_VERSION = "v1.0"
        const val PLATFORM = "iOS/Android App"
    }
}
