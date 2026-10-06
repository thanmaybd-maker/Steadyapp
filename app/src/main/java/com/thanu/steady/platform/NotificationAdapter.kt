package com.thanu.steady.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.thanu.steady.R

class NotificationAdapter(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    fun cancelAll() = notificationManager.cancelAll()

    fun canNotify(): Boolean = notificationManager.areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    // Channels are immutable after creation; each sound/haptic choice has its own channel.
    fun showRoutineReminder(route: String = "today"): Boolean {
        if(!canNotify()) return false
        val id = "steady_routine_reminders"
        notificationManager.createNotificationChannel(NotificationChannel(id,context.getString(R.string.reminder_settings),NotificationManager.IMPORTANCE_DEFAULT))
        val launch = android.app.PendingIntent.getActivity(context,0,android.content.Intent(context,com.thanu.steady.MainActivity::class.java).apply {
            data = android.net.Uri.Builder().scheme("steady").authority("reminder").appendPath(route).build()
            putExtra("steady_reminder_route", route)
        },
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        val value = NotificationCompat.Builder(context,id).setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(context.getString(R.string.reminder_notification_title)).setContentText(context.getString(R.string.reminder_notification_body))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(launch).setAutoCancel(true).build()
        return try { notificationManager.notify("routine",2,value); true } catch (_: SecurityException) { false }
    }
    fun showTimerCompleteNotification(sessionId: String, cueFlags: Int = 3): Boolean {
        if (!canNotify()) return false
        val flags = cueFlags and 3
        val names = arrayOf(R.string.channel_silent, R.string.channel_sound,
            R.string.channel_haptic, R.string.channel_sound_haptic)
        val channelId = "steady_timer_cues_$flags"
        val channel = NotificationChannel(channelId, context.getString(names[flags]),
            NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_description)
            enableVibration(flags and 2 != 0)
            setSound(if (flags and 1 != 0) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }
        notificationManager.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(context.getString(R.string.timer_complete))
            .setContentText(context.getString(R.string.timer_complete_detail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(android.app.PendingIntent.getActivity(context, 0, android.content.Intent(context, com.thanu.steady.MainActivity::class.java).apply {
                data = android.net.Uri.parse("steady://reminder/focus"); putExtra("steady_reminder_route", "focus")
            }, android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build()
        return try { notificationManager.notify(sessionId, 1, notification); true }
        catch (_: SecurityException) { false }
    }

}
