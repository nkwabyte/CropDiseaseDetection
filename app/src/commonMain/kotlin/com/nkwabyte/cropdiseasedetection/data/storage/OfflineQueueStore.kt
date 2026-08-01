package com.nkwabyte.cropdiseasedetection.data.storage

/**
 * A small file-backed blob store for the offline sync queue, keyed by file name.
 *
 * Deliberately not SharedPreferences / NSUserDefaults: a queued scan carries its image as
 * base64, so entries run to megabytes, and both of those APIs read their entire contents
 * into memory and are documented as unsuitable for payloads that size.
 *
 * Calls are blocking file I/O — invoke them off the main thread.
 */
expect class OfflineQueueStore() {
    /** Writes [contents] under [name], replacing any existing entry. */
    fun save(name: String, contents: String)

    /** Every stored entry as `name -> contents`. Unreadable entries are skipped. */
    fun readAll(): Map<String, String>

    /** Removes [name]; a no-op when it does not exist. */
    fun delete(name: String)
}
