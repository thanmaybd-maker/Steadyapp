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
    private val cues = AlarmCueStore(context)
    suspend fun claim(kind: String, id: String, generation: Int) = cues.claim(kind, id, generation)
    suspend fun invalidatePending() {
        cues.tokens().filter { !it.routine }.forEach { if(it.kind == "REST") cancelRest(it.id) else cancel(it.id) }
        cues.clearRoutineScheduled()
    }
    private fun intent(id: String) = Intent(context, ActivityAlarmReceiver::class.java).apply {
        action = "com.thanu.steady.ACTIVITY_COMPLETE"
        data = Uri.Builder().scheme("steady").authority("activity").appendPath(id).build()
    }
    suspend fun schedule(session: ActivitySession) {
        val target = session.deadlineElapsed ?: return
        if (session.state != "RUNNING") return
        val now = cues.time()
        cues.register(com.thanu.steady.domain.AlarmCueToken("ACTIVITY", session.id, session.generation, session.boot,
            now.wall + target - now.elapsed + 3_600_000, session.cueFlags, deadlineElapsed = target))
        val pending = PendingIntent.getBroadcast(context, 0, intent(session.id).apply {
            putExtra("id", session.id); putExtra("generation", session.generation)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, target, pending); return }
            catch (_: SecurityException) { /* Revocation races use the visibly documented approximate mode. */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, target, pending)
    }
    suspend fun cancel(id: String) {
        cues.cancel("ACTIVITY", id)
        val pending = PendingIntent.getBroadcast(context, 0, intent(id),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        manager.cancel(pending); pending.cancel()
    }
    suspend fun scheduleRest(value: com.thanu.steady.domain.WorkoutRest) {
        if(value.complete) return
        val now = cues.time()
        cues.register(com.thanu.steady.domain.AlarmCueToken("REST", value.id, value.generation, value.boot,
            now.wall + value.deadlineElapsed - now.elapsed + 3_600_000, value.cueFlags, deadlineElapsed = value.deadlineElapsed))
        val pending = PendingIntent.getBroadcast(context, 0, Intent(context, ActivityAlarmReceiver::class.java).apply {
            action = "com.thanu.steady.REST_COMPLETE"
            data = Uri.Builder().scheme("steady").authority("rest").appendPath(value.sessionId).build()
            putExtra("id",value.id); putExtra("generation",value.generation)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if(Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()) {
            try { manager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,value.deadlineElapsed,pending); return }
            catch (_: SecurityException) { /* Use the disclosed inexact fallback. */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,value.deadlineElapsed,pending)
    }
    suspend fun cancelRest(id: String) {
        cues.cancel("REST", id)
        val pending = PendingIntent.getBroadcast(context,0,Intent(context,ActivityAlarmReceiver::class.java).apply {
            action = "com.thanu.steady.REST_COMPLETE"; data = Uri.Builder().scheme("steady").authority("rest").appendPath(id).build()
        },PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        manager.cancel(pending); pending.cancel()
    }
}
class ActivityAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf("com.thanu.steady.ACTIVITY_COMPLETE","com.thanu.steady.REST_COMPLETE")) return
        val id = intent.getStringExtra("id") ?: return
        val generation = intent.getIntExtra("generation", -1)
        if (generation < 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as SteadyApplication).container
                val kind = if(intent.action == "com.thanu.steady.REST_COMPLETE") "REST" else "ACTIVITY"
                BackgroundCueDelivery(container.alarmCues, { container.isPrivateAccessible }, { type, tokenId, revision ->
                    if(type == "REST") container.activityRepository.completeRest(tokenId, revision) != null
                    else container.activityRepository.complete(tokenId, revision)?.let { container.activityAlarms.cancelRest(it.id); true } ?: false
                }, { token -> container.notificationAdapter.showTimerCompleteNotification(
                    if(token.kind == "REST") "rest:${token.id}" else token.id, token.flags) }).timer(kind, id, generation)
            } catch (_: Exception) { /* Preserve records; never log personal callback metadata or fabricate a cue. */ }
            finally { pending.finish() }
        }
    }
}
