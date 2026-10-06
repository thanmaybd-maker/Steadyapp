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
                container.timerRepository.reconcile(interrupt).forEach {
                    container.alarmAdapter.cancelAlarm(it.id)
                    if (it.state == com.thanu.steady.domain.TimerState.RUNNING)
                        it.targetElapsedTime?.let { deadline -> container.alarmAdapter.scheduleExactAlarm(deadline, it.id, it.generation) }
                }
            } catch (_: Exception) { /* Public help remains independent of this receiver. */ }
            finally { pending.finish() }
        }
    }
}
