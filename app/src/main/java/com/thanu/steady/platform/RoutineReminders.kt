package com.thanu.steady.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.net.Uri
import com.thanu.steady.SteadyApplication
import com.thanu.steady.domain.*
import com.thanu.steady.data.*
import androidx.room.withTransaction
import kotlinx.coroutines.*
import java.time.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Alarm payloads contain only an opaque source identifier, date and scheduled instant. */
class RoutineReminders(private val context: Context, private val provider: () -> SteadyDatabase, private val clock: Clock) {
    private val scheduling = Mutex()
    private val manager = context.getSystemService(AlarmManager::class.java)
    private fun intent(slot: Int) = Intent(context,RoutineReminderReceiver::class.java).apply {
        action = "com.thanu.steady.ROUTINE_REMINDER"; data = Uri.Builder().scheme("steady").authority("routine").appendPath(slot.toString()).build()
    }
    fun cancelAll() { for(slot in 0..14) {
        PendingIntent.getBroadcast(context,slot,intent(slot),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            manager.cancel(it); it.cancel()
        }
    } }
    suspend fun refresh() = scheduling.withLock {
        cancelAll()
        val db = provider()
        val p = db.preferencesDao().get() ?: AppPreferences()
        val profile = db.expandedDao().profile() ?: ExpandedProfile()
        if(profile.alertBudget == 0 || !NotificationAdapter(context).canNotify()) return@withLock
        val zone = ZoneId.of(p.zoneId)
        val today = LogicalDayPolicy().getLogicalDay(clock.instant(),zone,p.boundaryMinutes)
        var slot = 0
        for(offset in 0L..2L) {
            val day = today.plusDays(offset)
            val mode = db.expandedDao().day(day.toString())?.mode ?: if(p.pauseEnabled) "PAUSED" else "NORMAL"
            if(mode == "PAUSED") continue
            val candidates = mutableListOf<Triple<Long,String,String>>()
            db.expandedDao().versionsForDay(day.toString()).filter { it.reminderMinute != null &&
                HabitRules.scheduled(day,LocalDate.parse(it.anchorDay),it.weekdays,it.everyDays) }.forEach {
                candidates += Triple(ReminderRules.instant(day,it.reminderMinute!!,zone,p.boundaryMinutes),"HABIT",it.habitId)
            }
            db.expandedDao().care().filter { it.enabled && it.weekdays and (1 shl(day.dayOfWeek.value-1)) != 0 }.forEach {
                candidates += Triple(ReminderRules.instant(day,it.minute,zone,p.boundaryMinutes),"CARE",it.id)
            }
            val used = db.expandedDao().reminderCount(day.toString())
            candidates.sortedBy { it.first }.filter { it.first > clock.millis() &&
                !ReminderRules.quiet(Instant.ofEpochMilli(it.first).atZone(zone).let { t -> t.hour*60+t.minute },profile.quietStart,profile.quietEnd) &&
                db.expandedDao().note("delivery:$day:${it.second}:${it.third}") == null }.take((profile.alertBudget-used).coerceAtLeast(0)).forEach { (at,kind,id) ->
                val index = slot++
                val pending = PendingIntent.getBroadcast(context,index,intent(index).apply {
                    putExtra("id",id); putExtra("kind",kind); putExtra("day",day.toString()); putExtra("at",at)
                },PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pending)
            }
        }
    }
    suspend fun receive(id: String,kind: String,day: String,scheduled: Long) {
        require(kind in setOf("HABIT","CARE")); java.util.UUID.fromString(id); LocalDate.parse(day)
        val notifier = NotificationAdapter(context)
        if(!notifier.canNotify()) return
        val db = provider()
        val reserved = db.withTransaction {
            val dao = db.expandedDao(); val p = db.preferencesDao().get() ?: AppPreferences()
            val profile = dao.profile() ?: ExpandedProfile(); val zone = ZoneId.of(p.zoneId)
            val now = clock.millis(); val date = LocalDate.parse(day)
            val local = clock.instant().atZone(zone); val mode = dao.day(day)?.mode ?: if(p.pauseEnabled) "PAUSED" else "NORMAL"
            val key = "delivery:$day:$kind:$id"
            if(dao.note(key) != null || LogicalDayPolicy().getLogicalDay(clock.instant(),zone,p.boundaryMinutes) != date ||
                !ReminderRules.eligible(now,scheduled,mode,ReminderRules.quiet(local.hour*60+local.minute,profile.quietStart,profile.quietEnd),dao.reminderCount(day),profile.alertBudget)) return@withTransaction false
            val valid = if(kind == "CARE") dao.care().any { it.id == id && it.enabled &&
                it.weekdays and (1 shl(date.dayOfWeek.value-1)) != 0 && ReminderRules.instant(date,it.minute,zone,p.boundaryMinutes) == scheduled } &&
                dao.careLogs(day,day).none { it.reminderId == id }
            else dao.versionsForDay(day).any { it.habitId == id && it.reminderMinute != null &&
                HabitRules.scheduled(date,LocalDate.parse(it.anchorDay),it.weekdays,it.everyDays) && ReminderRules.instant(date,it.reminderMinute!!,zone,p.boundaryMinutes) == scheduled } &&
                dao.occurrences(day,day).none { it.habitId == id && it.state in setOf("COMPLETED","SKIPPED") }
            if(!valid) return@withTransaction false
            dao.save(SessionNote(key,null,"RESERVED",now)); true
        }
        if(reserved) notifier.showRoutineReminder()
        refresh()
    }
}
class RoutineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        if(intent.action != "com.thanu.steady.ROUTINE_REMINDER") return
        val id = intent.getStringExtra("id") ?: return; val kind = intent.getStringExtra("kind") ?: return
        val day = intent.getStringExtra("day") ?: return; val at = intent.getLongExtra("at",-1)
        val pending = goAsync()
        CoroutineScope(SupervisorJob()+Dispatchers.IO).launch {
            try { (context.applicationContext as SteadyApplication).container.routineReminders.receive(id,kind,day,at) }
            catch (_: Exception) { /* No private content in diagnostics; leave stored records intact. */ }
            finally { pending.finish() }
        }
    }
}
