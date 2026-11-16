package com.nkwabyte.cropdiseasedetection.common.models

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

/** Downloaded weights live in `Application Support/models`, excluded from iCloud backup. */
@OptIn(ExperimentalForeignApi::class)
actual class ModelFileStore actual constructor() {
    private val fileManager = NSFileManager.defaultManager

    private val dir: String by lazy {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
            .first() as String
        val path = "$base/models"
        fileManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
        NSURL.fileURLWithPath(path).setResourceValue(NSNumber(bool = true), forKey = NSURLIsExcludedFromBackupKey, error = null)
        path
    }

    actual fun path(fileName: String): String = "$dir/$fileName"

    actual fun exists(fileName: String): Boolean {
        val attrs = fileManager.attributesOfItemAtPath(path(fileName), error = null) ?: return false
        return ((attrs["NSFileSize"] as? NSNumber)?.longLongValue ?: 0L) > 0L
    }

    actual fun openWriter(fileName: String): ModelFileWriter {
        val handle = fopen(path(fileName), "wb") ?: error("Cannot open $fileName for writing")
        return object : ModelFileWriter {
            override fun write(bytes: ByteArray, length: Int) {
                if (length <= 0) return
                val written = bytes.usePinned { fwrite(it.addressOf(0), 1u, length.convert(), handle) }
                check(written.toLong() == length.toLong()) { "Disk write failed — is the device out of space?" }
            }

            override fun close() {
                fclose(handle)
            }
        }
    }

    actual fun moveIntoPlace(from: String, to: String): Boolean {
        fileManager.removeItemAtPath(path(to), error = null)
        return fileManager.moveItemAtPath(path(from), toPath = path(to), error = null)
    }

    actual fun delete(fileName: String) {
        fileManager.removeItemAtPath(path(fileName), error = null)
    }
}
