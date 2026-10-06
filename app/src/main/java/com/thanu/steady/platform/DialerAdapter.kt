package com.thanu.steady.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

class DialerAdapter(private val context: Context) {
    fun openDialer(phoneNumber: String) {
        val uri = Uri.parse("tel:$phoneNumber")
        val intent = Intent(Intent.ACTION_DIAL, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
