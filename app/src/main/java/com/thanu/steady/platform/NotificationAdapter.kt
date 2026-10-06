package com.thanu.steady.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class NotificationAdapter(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "steady_timer",
                "Timer Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for completed focus and break timers"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTimerCompleteNotification(sessionId: String) {
        val notification = NotificationCompat.Builder(context, "steady_timer")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Timer Complete")
            .setContentText("Your scheduled time is up.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(sessionId.hashCode(), notification)
    }

}
