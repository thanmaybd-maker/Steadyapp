package com.thanu.steady.platform

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UsageInterceptor(private val context: Context) {

    private val _isPermissionGranted = MutableStateFlow(checkPermission())
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted

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

    fun requestPermission(onRequested: () -> Unit) {
        if (!checkPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            onRequested()
        }
    }

    fun getUsageInsights(startTime: Long, endTime: Long): Map<String, Long> {
        if (!checkPermission()) return emptyMap()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime) ?: return emptyMap()

        val appDurations = mutableMapOf<String, Long>()
        val startTimes = mutableMapOf<String, Long>()

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    startTimes[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    startTimes[event.packageName]?.let { start ->
                        val duration = event.timeStamp - start
                        appDurations[event.packageName] = (appDurations[event.packageName] ?: 0L) + duration
                        startTimes.remove(event.packageName)
                    }
                }
            }
        }
        return appDurations
    }
}
