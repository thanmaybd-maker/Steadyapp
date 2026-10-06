package com.thanu.steady.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.thanu.steady.SteadyApplication
import com.thanu.steady.data.ActivitySession
import kotlinx.coroutines.*

class ActivityAlarmAdapter(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)
    private fun intent(id: String) = Intent(context, ActivityAlarmReceiver::class.java).apply {
        action = "com.thanu.steady.ACTIVITY_COMPLETE"
        data = Uri.Builder().scheme("steady").authority("activity").appendPath(id).build()
    }
    fun schedule(session: ActivitySession) {
        val target = session.deadlineElapsed ?: return
        if (session.state != "RUNNING") return
        val pending = PendingIntent.getBroadcast(context, 0, intent(session.id).apply {
            putExtra("id", session.id); putExtra("generation", session.generation)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, target, pending); return }
            catch (_: SecurityException) { /* Revocation races use the visibly documented approximate mode. */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, target, pending)
    }
    fun cancel(id: String) {
        val pending = PendingIntent.getBroadcast(context, 0, intent(id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        manager.cancel(pending); pending.cancel()
    }
}
class ActivityAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.thanu.steady.ACTIVITY_COMPLETE") return
        val id = intent.getStringExtra("id") ?: return
        val generation = intent.getIntExtra("generation", -1)
        if (generation < 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as SteadyApplication).container
                container.activityRepository.complete(id, generation)?.let {
                    container.notificationAdapter.showTimerCompleteNotification(it.id, it.cueFlags)
                }
            } catch (_: Exception) { /* Preserve records; never log personal callback metadata or fabricate a cue. */ }
            finally { pending.finish() }
        }
    }
}
