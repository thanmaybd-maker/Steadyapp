package com.thanu.steady.platform

import android.content.Context
import android.util.Log

class NotificationAdapter(private val context: Context) {
    fun showTimerCompleteNotification(kind: String) {
        // Stub implementation. Requires NotificationCompat.Builder
        Log.d("SteadyNotif", "Timer complete generic cue for $kind")
    }
}
