package com.ayushojha.levain.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * App-private photo storage: photos never touch the device gallery, and the
 * photos directory is excluded from Auto Backup (the export zip covers them).
 */
class PhotoStore(private val context: Context) {

    private val photosDir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    /** Copies a picked image into app-private storage; returns the stored file name. */
    fun importPhoto(source: Uri): String {
        val name = "${UUID.randomUUID()}.jpg"
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Cannot open $source" }
            File(photosDir, name).outputStream().use { output -> input.copyTo(output) }
        }
        return name
    }

    fun fileFor(photoPath: String): File = File(photosDir, photoPath)
}
