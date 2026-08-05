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

    /**
     * Records the user deleted during this session.
     *
     * Deleting is optimistic, but the on-disk cache is only rewritten once Firestore
     * confirms the soft delete. Anything that repaints from that cache in between —
     * and on iOS dismissing the detail sheet re-fires `onAppear`, so [loadHistory] runs
     * within milliseconds of the tap — would put the row straight back. Every list the
     * state is built from is filtered through this, so no repaint path can resurrect a
     * record the user already dismissed.
     */
    private val deletedRecords = mutableListOf<DetectionRecord>()

    private fun List<DetectionRecord>.withoutDeleted(): List<DetectionRecord> =
        filterNot { candidate -> deletedRecords.any { candidate.isSameRecordAs(it) } }

    init {
        viewModelScope.launch {
            Firebase.auth.authStateChanged.collect { user ->
                if (user != null) {
                    _historyState.value = _historyState.value.copy(isGuest = false)
                    loadHistory()
                } else {
                    // Records belong to the account that just signed out; drop them rather
                    // than leave one user's scans on screen for the next. The tombstones go
                    // with them — they identify the previous account's documents.
                    deletedRecords.clear()
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
                    records = (cached.records + pending)
                        .withoutDeleted()
                        .sortedByDescending { it.timestamp },
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
                    // A refresh that started before the soft delete landed will still carry
                    // the record; the tombstones keep it off screen either way.
                    records = records.withoutDeleted(),
                    lastUpdated = io.ktor.util.date.GMTDate().timestamp
                )
            } catch (e: Exception) {
                // Keep whatever the cache gave us rather than blanking the page on a failure.
                _historyState.value = _historyState.value.copy(isLoading = false, isRefreshing = false)
            }
        }
    }

    /**
     * Removes [record] from the user's history. The document is moved to the "archive"
     * collection in Firestore for training data and permanently deleted from "detections".
     *
     * The row is dropped from the list first and put back if the write fails, so the tap feels
     * immediate without lying about what was actually stored.
     */
    fun deleteRecord(record: DetectionRecord) {
        val previous = _historyState.value.records
        deletedRecords.add(record)
        _historyState.value = _historyState.value.copy(records = previous.withoutDeleted())

        viewModelScope.launch {
            val ok = syncRepository.archiveAndDeleteDetectionRecord(record)
            if (ok) {
                syncRepository.getCachedHistory()?.let { cached ->
                    // Rewrite the cache so the record does not come back on the next open.
                    syncRepository.overwriteHistoryCache(cached.records.withoutDeleted())
                }
            } else {
                // The write failed, so the record really is still there — drop the tombstone
                // before restoring, or the row would be filtered straight back out.
                deletedRecords.removeAll { it.isSameRecordAs(record) }
                _historyState.value = _historyState.value.copy(records = previous)
            }
        }
    }

    /**
     * A queued scan has no document id, so identity falls back to the timestamp alone.
     *
     * Not the timestamp *and* image: a queued scan's `imageUrl` is an inline data URL and
     * its uploaded twin's is a remote one, so comparing images made the two look like
     * different scans — deleting one left the other to reappear on the next refresh. The
     * upload now carries the original timestamp over, which makes it the stable identity.
     */
    private fun DetectionRecord.isSameRecordAs(other: DetectionRecord): Boolean =
        if (docId != null && other.docId != null) docId == other.docId
        else timestamp == other.timestamp
}
