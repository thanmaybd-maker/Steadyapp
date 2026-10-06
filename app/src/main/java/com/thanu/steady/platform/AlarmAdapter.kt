package com.thanu.steady.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

class AlarmAdapter(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleExactAlarm(targetElapsedMs: Long, sessionId: String, generation: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.thanu.steady.TIMER_ALARM"
            putExtra("sessionId", sessionId)
            putExtra("generation", generation)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        targetElapsedMs,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact if permission denied
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        targetElapsedMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    targetElapsedMs,
                    pendingIntent
                )
            }
            Log.d("SteadyAlarm", "Scheduled alarm for session $sessionId gen $generation at elapsed $targetElapsedMs")
        } catch (e: SecurityException) {
            Log.e("SteadyAlarm", "SecurityException scheduling alarm: ${e.message}")
        }
    }

    fun cancelAlarm(sessionId: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.thanu.steady.TIMER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        Log.d("SteadyAlarm", "Cancelled alarm for session $sessionId")
    }
}
