package com.thanu.steady.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.thanu.steady.SteadyApplication
import kotlinx.coroutines.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.thanu.steady.TIMER_ALARM") return
        val id = intent.getStringExtra("sessionId") ?: return
        val generation = intent.getIntExtra("generation", -1)
        if (generation < 0) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as SteadyApplication).container
                if(!container.isPrivateAccessible) return@launch // Legacy timers reconcile when the owner returns.
                container.timerRepository.complete(id, generation)?.let {
                    container.notificationAdapter.showTimerCompleteNotification(it.id, it.cueFlags)
                }
            } catch (_: Exception) {
                // A failed storage open must not expose data or create an unverified alert.
            } finally { pending.finish() }
        }
    }
}

class TimerReconcileReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val interrupt = intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED)
        if (!interrupt && intent.action !in setOf(Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as SteadyApplication).container
                if(intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_TIME_CHANGED) {
                    container.alarmCues.invalidateScheduled()
                    container.routineReminders.cancelAll()
                }
                if(!container.isPrivateAccessible) return@launch
                container.timerRepository.reconcile(interrupt).forEach {
                    container.alarmAdapter.cancelAlarm(it.id)
                    if (it.state == com.thanu.steady.domain.TimerState.RUNNING)
                        it.targetElapsedTime?.let { deadline -> container.alarmAdapter.scheduleExactAlarm(deadline, it.id, it.generation) }
                }
                container.activityRepository.reconcile().forEach {
                    container.activityAlarms.cancel(it.id)
                    container.activityAlarms.cancelRest(it.id)
                    if (it.state == "RUNNING") container.activityAlarms.schedule(it)
                    val rest = container.activityRepository.rest(it.id)
                    if(rest != null && !rest.complete && rest.boot == container.activityRepository.time().boot && it.state == "RUNNING")
                        container.activityAlarms.scheduleRest(rest)
                    else if(rest != null) container.activityRepository.cancelRest(it.id)
                }
                container.routineReminders.refresh()
            } catch (_: Exception) { /* Public help remains independent of this receiver. */ }
            finally { pending.finish() }
        }
    }
}
