package com.thanu.steady.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

class AlarmAdapter(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    fun hasExactAccess() = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
    private fun intent(id: String) = Intent(context, AlarmReceiver::class.java).apply {
        action = "com.thanu.steady.TIMER_ALARM"
        data = Uri.Builder().scheme("steady").authority("timer").appendPath(id).build()
    }
    fun scheduleExactAlarm(targetElapsedMs: Long, sessionId: String, generation: Int) {
        val pending = PendingIntent.getBroadcast(context, 0, intent(sessionId).apply {
            putExtra("sessionId", sessionId); putExtra("generation", generation)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (hasExactAccess()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, targetElapsedMs, pending)
                return
            } catch (_: SecurityException) { /* Access can be revoked between check and schedule. */ }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, targetElapsedMs, pending)
    }
    fun cancelAlarm(sessionId: String) {
        val pending = PendingIntent.getBroadcast(context, 0, intent(sessionId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        alarmManager.cancel(pending)
        pending.cancel()
    }
}
