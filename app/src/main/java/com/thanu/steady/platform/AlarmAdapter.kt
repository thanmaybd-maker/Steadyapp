package com.thanu.steady.platform

import android.content.Context
import android.util.Log

class AlarmAdapter(private val context: Context) {
    // Stub implementation. A real implementation requires PendingIntent and AlarmManager.
    fun scheduleExactAlarm(targetElapsedMs: Long, sessionId: String, generation: Int) {
        // Log the scheduling for verification.
        Log.d("SteadyAlarm", "Scheduled alarm for session $sessionId gen $generation at elapsed $targetElapsedMs")
    }

    fun cancelAlarm(sessionId: String) {
        Log.d("SteadyAlarm", "Cancelled alarm for session $sessionId")
    }
}
