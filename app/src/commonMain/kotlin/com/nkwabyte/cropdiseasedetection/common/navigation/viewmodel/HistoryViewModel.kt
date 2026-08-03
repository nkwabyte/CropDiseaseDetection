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
    /** Blocking spinner. Only set when there is nothing cached to show yet. */
    val isLoading: Boolean = false,
    /** A refresh running behind content already on screen. */
    val isRefreshing: Boolean = false,
    val isGuest: Boolean = true,
    val records: List<DetectionRecord> = emptyList(),
    /** When the visible records were last fetched from Firestore; 0 when never. */
    val lastUpdated: Long = 0L
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
                    // Records belong to the account that just signed out; drop them rather
                    // than leave one user's scans on screen for the next.
                    _historyState.value = HistoryState(isGuest = true, records = emptyList())
                }
            }
        }
    }

    /**
     * Opening the history page. Serves the cache when it is fresh, so navigating back and
     * forth inside the cache window costs no requests at all.
     */
    fun loadHistory() = load(forceRefresh = false)

    /** Pull-to-refresh: always goes to the network, ignoring the cache window. */
    fun refresh() = load(forceRefresh = true)

    /**
     * Paints the cached history straight away, then goes to the network only when the cache
     * is stale, empty, or [forceRefresh] was asked for.
     *
     * Split from [loadHistory] rather than given a default argument because Kotlin/Native
     * does not export defaults, and the Swift call sites would have to pass the flag.
     */
    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            val cached = syncRepository.getCachedHistory()

            if (cached != null) {
                // Queued scans live outside the cache, so fold them back in before showing it.
                val pending = syncRepository.getPendingAsRecords()
                _historyState.value = _historyState.value.copy(
                    records = (cached.records + pending).sortedByDescending { it.timestamp },
                    lastUpdated = cached.fetchedAt
                )
            }

            val hasCache = cached != null && cached.records.isNotEmpty()
            val needsFetch = forceRefresh || cached == null || syncRepository.isCacheStale(cached.fetchedAt)
            if (!needsFetch) return@launch

            _historyState.value = _historyState.value.copy(
                isLoading = !hasCache,
                isRefreshing = hasCache
            )
            try {
                syncRepository.processPendingQueue(cloudinaryApi)
                val records = syncRepository.getDetectionRecords()
                _historyState.value = _historyState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    records = records,
                    lastUpdated = io.ktor.util.date.GMTDate().timestamp
                )
            } catch (e: Exception) {
                // Keep whatever the cache gave us rather than blanking the page on a failure.
                _historyState.value = _historyState.value.copy(isLoading = false, isRefreshing = false)
            }
        }
    }

    /**
     * Removes [record] from the user's history. The document is only flagged in Firestore, so
     * it stays available for model training.
     *
     * The row is dropped from the list first and put back if the write fails, so the tap feels
     * immediate without lying about what was actually stored.
     */
    fun deleteRecord(record: DetectionRecord) {
        val previous = _historyState.value.records
        _historyState.value = _historyState.value.copy(
            records = previous.filterNot { it.isSameRecordAs(record) }
        )

        viewModelScope.launch {
            val ok = syncRepository.softDeleteDetectionRecord(record)
            if (ok) {
                syncRepository.getCachedHistory()?.let { cached ->
                    // Rewrite the cache so the record does not come back on the next open.
                    syncRepository.overwriteHistoryCache(
                        cached.records.filterNot { it.isSameRecordAs(record) }
                    )
                }
            } else {
                _historyState.value = _historyState.value.copy(records = previous)
            }
        }
    }

    /**
     * A queued scan has no document id, so identity falls back to the timestamp and image —
     * the same pair the list uses for its keys.
     */
    private fun DetectionRecord.isSameRecordAs(other: DetectionRecord): Boolean =
        if (docId != null && other.docId != null) docId == other.docId
        else timestamp == other.timestamp && imageUrl == other.imageUrl
}
