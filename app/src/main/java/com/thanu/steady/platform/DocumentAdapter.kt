package com.thanu.steady.platform

import android.content.Context
import android.net.Uri
import java.io.OutputStreamWriter

class DocumentAdapter(private val context: Context) {
    fun writeMarkdownToUri(uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
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
            context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readBackupFromUri(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }
}
