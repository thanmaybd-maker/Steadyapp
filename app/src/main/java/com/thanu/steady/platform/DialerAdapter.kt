package com.thanu.steady.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

class DialerAdapter(private val context: Context) {
    fun openDialer(phoneNumber: String): Boolean {
        val cleaned = phoneNumber.filterNot { it.isWhitespace() || it in "()-" }
        if (!cleaned.matches(Regex("\\+?[0-9]{2,20}"))) return false
        val uri = Uri.fromParts("tel", cleaned, null)
        val intent = Intent(Intent.ACTION_DIAL, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try { context.startActivity(intent); true }
        catch (_: android.content.ActivityNotFoundException) { false }
        catch (_: SecurityException) { false }
    }
}
