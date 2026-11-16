package com.nkwabyte.cropdiseasedetection.common.models

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream

/** Downloaded weights live in `noBackupFilesDir/models` — app-private and never backed up. */
actual class ModelFileStore actual constructor() : KoinComponent {
    private val context: Context by inject()

    private val dir: File
        get() = File(context.noBackupFilesDir, "models").apply { mkdirs() }

    private fun file(fileName: String) = File(dir, fileName)

    actual fun path(fileName: String): String = file(fileName).absolutePath

    actual fun exists(fileName: String): Boolean = file(fileName).let { it.isFile && it.length() > 0 }

    actual fun openWriter(fileName: String): ModelFileWriter {
        val out = FileOutputStream(file(fileName), false)
        return object : ModelFileWriter {
            override fun write(bytes: ByteArray, length: Int) = out.write(bytes, 0, length)
            override fun close() = out.close()
        }
    }

    actual fun moveIntoPlace(from: String, to: String): Boolean {
        val target = file(to)
        target.delete()
        return file(from).renameTo(target)
    }

    actual fun delete(fileName: String) {
        file(fileName).delete()
    }
}
