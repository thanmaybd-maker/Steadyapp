package com.thanu.steady.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.thanu.steady.TIMER_ALARM") {
            val sessionId = intent.getStringExtra("sessionId")
            val generation = intent.getIntExtra("generation", 0)
            Log.d("SteadyAlarm", "Alarm triggered for session $sessionId gen $generation")
            
            // High-importance notification automatically plays sound/haptics according to user settings
            val notificationAdapter = NotificationAdapter(context)
            notificationAdapter.showTimerCompleteNotification(sessionId ?: "unknown")
        }
    }
}
