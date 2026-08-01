package com.nkwabyte.cropdiseasedetection.data.storage

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

actual class OfflineQueueStore actual constructor() : KoinComponent {
    private val context: Context by inject()

    private val dir: File by lazy {
        File(context.filesDir, DIR_NAME).apply { mkdirs() }
    }

    actual fun save(name: String, contents: String) {
        try {
            // Write to a scratch file and rename, so being killed mid-write cannot leave a
            // truncated record that fails to parse on the next launch.
            val temp = File(dir, "$name$TEMP_SUFFIX")
            temp.writeText(contents)
            if (!temp.renameTo(File(dir, name))) {
                File(dir, name).writeText(contents)
                temp.delete()
            }
        } catch (e: Exception) {
            println("OfflineQueueStore: failed to save $name: ${e.message}")
        }
    }

    actual fun readAll(): Map<String, String> {
        val files = dir.listFiles() ?: return emptyMap()
        return files
            .filter { it.isFile && !it.name.endsWith(TEMP_SUFFIX) }
            .mapNotNull { file ->
                runCatching { file.name to file.readText() }
                    .onFailure { println("OfflineQueueStore: failed to read ${file.name}: ${it.message}") }
                    .getOrNull()
            }
            .toMap()
    }

    actual fun delete(name: String) {
        runCatching { File(dir, name).delete() }
    }

    private companion object {
        const val DIR_NAME = "offline_queue"
        const val TEMP_SUFFIX = ".tmp"
    }
}
