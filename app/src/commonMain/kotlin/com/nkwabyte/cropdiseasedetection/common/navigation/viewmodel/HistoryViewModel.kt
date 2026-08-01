package com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel

import com.nkwabyte.cropdiseasedetection.data.network.CloudinaryApi
import com.nkwabyte.cropdiseasedetection.data.repository.DetectionRecord
import com.nkwabyte.cropdiseasedetection.data.repository.SyncRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryState(
    val isLoading: Boolean = false,
    val isGuest: Boolean = true,
    val records: List<DetectionRecord> = emptyList()
)

class HistoryViewModel(
    private val syncRepository: SyncRepository,
    private val cloudinaryApi: CloudinaryApi
) {
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _historyState = MutableStateFlow(HistoryState())
    val historyState: StateFlow<HistoryState> = _historyState.asStateFlow()

    init {
        viewModelScope.launch {
            Firebase.auth.authStateChanged.collect { user ->
                if (user != null) {
                    _historyState.value = _historyState.value.copy(isGuest = false)
                    loadHistory()
                } else {
                    _historyState.value = HistoryState(isGuest = true, records = emptyList())
                }
            }
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _historyState.value = _historyState.value.copy(isLoading = true)
            try {
                syncRepository.processPendingQueue(cloudinaryApi)
                val records = syncRepository.getDetectionRecords()
                _historyState.value = _historyState.value.copy(isLoading = false, records = records)
            } catch (e: Exception) {
                _historyState.value = _historyState.value.copy(isLoading = false)
            }
        }
    }
}
