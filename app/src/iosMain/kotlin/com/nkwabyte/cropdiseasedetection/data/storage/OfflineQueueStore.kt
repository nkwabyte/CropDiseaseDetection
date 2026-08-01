package com.nkwabyte.cropdiseasedetection.data.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class OfflineQueueStore actual constructor() {
    private val fileManager = NSFileManager.defaultManager

    // Application Support rather than Documents: this is app-managed working state, not
    // user documents, so it should stay out of the Files app.
    private val dir: String? by lazy {
        val base = NSSearchPathForDirectoriesInDomains(
            directory = NSApplicationSupportDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true
        ).firstOrNull() as? String ?: return@lazy null

        val path = "$base/$DIR_NAME"
        if (!fileManager.fileExistsAtPath(path)) {
            // Application Support itself may not exist yet on a fresh install.
            fileManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
        }
        path
    }

    actual fun save(name: String, contents: String) {
        val path = dir ?: return
        val bytes = contents.encodeToByteArray()
        if (bytes.isEmpty()) return

        // atomically = true writes to a scratch file and swaps it in, so being killed
        // mid-write cannot leave a truncated record behind.
        val data = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        if (!data.writeToFile("$path/$name", atomically = true)) {
            println("OfflineQueueStore: failed to save $name")
        }
    }

    actual fun readAll(): Map<String, String> {
        val path = dir ?: return emptyMap()
        val names = fileManager.contentsOfDirectoryAtPath(path, error = null)
            ?.filterIsInstance<String>()
            ?: return emptyMap()

        return names.mapNotNull { name ->
            val data = NSData.dataWithContentsOfFile("$path/$name")
            if (data == null) {
                println("OfflineQueueStore: failed to read $name")
                return@mapNotNull null
            }
            name to data.toByteArray().decodeToString()
        }.toMap()
    }

    actual fun delete(name: String) {
        val path = dir ?: return
        fileManager.removeItemAtPath("$path/$name", error = null)
    }

    private companion object {
        const val DIR_NAME = "offline_queue"
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}
