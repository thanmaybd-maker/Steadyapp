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

    fun readLegacyJsonFromUri(uri: Uri): String? {
        val bytes = readBackupFromUri(uri, com.thanu.steady.data.LegacyOgCodec.MAX_BYTES) ?: return null
        return try {
            Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        } catch (_: Exception) { null } finally { bytes.fill(0) }
    }
    fun readBackupFromUri(uri: Uri, maxBytes: Int = com.thanu.steady.data.PortableCodec.MAX_BYTES + 60): ByteArray? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use {
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = it.read(buffer)
                    if (count < 0) break
                    if (output.size().toLong() + count > maxBytes) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } catch (e: Exception) {
            null
        }
    }
}
