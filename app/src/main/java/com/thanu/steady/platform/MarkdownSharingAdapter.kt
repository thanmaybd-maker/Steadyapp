package com.thanu.steady.platform

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import com.thanu.steady.R

/** Operates only on an explicitly prepared organiser preview, never on repositories/private stores. */
class MarkdownSharingAdapter(private val context: Context,
    private val copyClip: (ClipData) -> Unit = { context.getSystemService(ClipboardManager::class.java).setPrimaryClip(it) },
    private val launch: (Intent) -> Unit = context::startActivity) {
    companion object { const val MAX_BYTES = 256 * 1024 }
    private fun eligible(text: String) = text.isNotBlank() && text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES
    fun copyPreview(text: String): Boolean {
        if(!eligible(text)) return false
        return try {
            val clip=ClipData.newPlainText(context.getString(R.string.markdown_title),text)
            if(Build.VERSION.SDK_INT >= 33) clip.description.extras=PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE,true) }
            copyClip(clip); true
        } catch (_: Exception) { false }
    }
    fun sharePreview(text: String): Boolean {
        if(!eligible(text)) return false
        return try {
            val send=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text)
            launch(Intent.createChooser(send,context.getString(R.string.share_markdown_preview)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
        } catch (_: android.content.ActivityNotFoundException) { false }
          catch (_: SecurityException) { false }
    }
}
