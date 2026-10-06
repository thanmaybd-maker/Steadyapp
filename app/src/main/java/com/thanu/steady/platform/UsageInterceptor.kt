package com.thanu.steady.platform

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

class UsageInterceptor(private val context: Context) {

    fun checkPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestPermission(): Boolean = try {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (_: android.content.ActivityNotFoundException) { false }
      catch (_: SecurityException) { false }

    fun getUsageInsights(startTime: Long, endTime: Long): Map<String, Long> {
        if (!checkPermission()) throw SecurityException("Usage access unavailable")

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        require(endTime >= startTime && endTime - startTime <= 2 * 86_400_000L)
        val usageEvents = usageStatsManager.queryEvents((startTime - 86_400_000L).coerceAtLeast(0), endTime)
            ?: throw IllegalStateException("Usage events unavailable")
        val events = mutableListOf<com.thanu.steady.domain.UsageTransition>()

        val event = UsageEvents.Event()
        var scanned = 0
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            check(++scanned <= 100_000)
            if(event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == UsageEvents.Event.ACTIVITY_STOPPED)
                event.packageName?.let { events.add(com.thanu.steady.domain.UsageTransition(it, event.timeStamp, event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)) }
        }
        if(!checkPermission()) throw SecurityException("Usage access revoked")
        return com.thanu.steady.domain.UsageDurationRules.durations(startTime, endTime, events)
    }
}
