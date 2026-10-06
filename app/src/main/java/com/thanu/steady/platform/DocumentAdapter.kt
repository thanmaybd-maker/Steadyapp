package com.thanu.steady.platform

import android.content.Context
import android.net.Uri
import java.io.OutputStreamWriter

class DocumentAdapter(private val context: Context) {
    fun writeMarkdownToUri(uri: Uri, content: String): Boolean {
        return try {
            val output = context.contentResolver.openOutputStream(uri, "wt") ?: return false
            output.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun writeBackupToUri(uri: Uri, data: ByteArray): Boolean {
        return try {
            val output = context.contentResolver.openOutputStream(uri, "wt") ?: return false
            output.use { it.write(data); it.flush() }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readBackupFromUri(uri: Uri): ByteArray? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use {
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    if (output.size().toLong() + count > com.thanu.steady.data.PortableCodec.MAX_BYTES.toLong() + 60) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
    }
}
