package com.thanu.steady

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import com.thanu.steady.domain.*
import com.thanu.steady.platform.*
import kotlinx.coroutines.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoutineSchedulingTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "reminder-synthetic-${UUID.randomUUID()}.db"
    private lateinit var db: SteadyDatabase
    private lateinit var cues: AlarmCueStore
    private lateinit var reminders: RoutineReminders
    private val clock = Clock.systemUTC()
    @Before fun setup() = runBlocking {
        check(context.packageName == "com.thanu.steady.qa")
        check(NotificationAdapter(context).canNotify()) { "Grant notifications to the isolated QA app for this device check" }
        System.loadLibrary("sqlcipher")
        db = Room.databaseBuilder(context,SteadyDatabase::class.java,name)
            .openHelperFactory(SupportOpenHelperFactory(ByteArray(32).also(java.security.SecureRandom()::nextBytes))).build()
        cues = AlarmCueStore(context); cues.clear()
        reminders = RoutineReminders(context,{ db },clock,cues) { false }
        PreferencesRepository { db }.update { it.copy(zoneId="UTC",boundaryMinutes=0) }
        ExpandedRepository({ db },clock,PreferencesRepository { db }).saveProfile(ExpandedProfile(onboarded=true,alertBudget=2,quietStart=0,quietEnd=0))
        NotificationAdapter(context).cancelAll()
    }
    @After fun cleanup() = runBlocking {
        if(::reminders.isInitialized) reminders.cancelAll()
        if(::cues.isInitialized) cues.clear()
        NotificationAdapter(context).cancelAll()
        if(::db.isInitialized) db.close()
        context.deleteDatabase(name); Unit
    }
    private fun repository() = ExpandedRepository({ db },clock,PreferencesRepository { db })
    @Test fun rescheduleGlobalPerBlockDeleteQuietAndPauseCancelActualPendingAlarms() = runBlocking {
        val day = LocalDate.now(clock).plusDays(1)
        val block = StudyBlockRules.templates(day,"UTC",0,listOf("Synthetic morning","Synthetic afternoon","Synthetic evening"))[0].copy(reminder=true)
        val repo = repository()
        val planId = repo.adoptStudyBlock(block)
        reminders.refresh(); assertTrue(cues.tokens().isEmpty())
        repo.studyBlockSettings(StudyBlockSettings(true)); reminders.refresh()
        val original = cues.tokens().single()
        assertEquals(block.id,original.id)
        assertEquals(ReminderRules.instant(day,block.minute,ZoneOffset.UTC,0)-300_000,original.wallAt)
        val plan = db.expandedDao().task(planId)!!
        repo.saveTask(plan.copy(timeMinutes=600,title="Synthetic renamed")); reminders.refresh()
        assertNotEquals(original.wallAt,cues.tokens().single().wallAt)
        repo.studyBlockSettings(StudyBlockSettings(false)); reminders.refresh()
        assertTrue(cues.tokens().isEmpty()); assertNull(pending())
        repo.studyBlockSettings(StudyBlockSettings(true)); reminders.refresh()
        repo.saveProfile(repo.profile().copy(quietStart=540,quietEnd=660)); reminders.refresh()
        assertTrue(cues.tokens().isEmpty()); assertNull(pending())
        repo.saveProfile(repo.profile().copy(quietStart=0,quietEnd=0))
        repo.setDay(day,mode="PAUSED"); reminders.refresh()
        assertTrue(cues.tokens().isEmpty())
        repo.setDay(day,mode="NORMAL")
        repo.saveStudyBlock(repo.studyBlocks(day).single().copy(reminder=false)); reminders.refresh()
        assertTrue(cues.tokens().isEmpty())
        repo.saveStudyBlock(repo.studyBlocks(day).single().copy(reminder=true)); reminders.refresh()
        assertEquals(1,cues.tokens().size)
        repo.deleteTask(planId); reminders.refresh()
        assertTrue(cues.tokens().isEmpty()); assertNull(pending())
    }
    @Test fun lockedRoutineDeliveryNeverOpensPrivateProviderAndDuplicateCannotNotifyAgain() = runBlocking {
        val at = clock.millis()-10
        val boot = Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,0).toLong()
        val token = AlarmCueToken("BLOCK",UUID.randomUUID().toString(),0,boot,at+300_000,wallAt=at,day=LocalDate.now(clock).toString())
        cues.register(token)
        val locked = RoutineReminders(context,{ error("Private provider must remain closed") },clock,cues) { false }
        locked.receive(token.id,token.kind,token.day!!,at)
        assertTrue(cues.tokens().single().delivered)
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val first = manager.activeNotifications.single()
        assertEquals(context.getString(R.string.reminder_notification_title),first.notification.extras.getString(android.app.Notification.EXTRA_TITLE))
        locked.receive(token.id,token.kind,requireNotNull(token.day),at)
        assertEquals(first.postTime,manager.activeNotifications.single().postTime)
        val timer = token.copy(kind="ACTIVITY",id=UUID.randomUUID().toString(),day=null,wallAt=null,deadlineElapsed=SystemClock.elapsedRealtime()-1)
        cues.register(timer)
        val container = (context.applicationContext as SteadyApplication).container
        assertFalse(container.isPrivateAccessible)
        context.sendBroadcast(Intent(context,ActivityAlarmReceiver::class.java).apply {
            action="com.thanu.steady.ACTIVITY_COMPLETE"; putExtra("id",timer.id); putExtra("generation",0)
        })
        withTimeout(5000) { while(manager.activeNotifications.none { it.tag == timer.id }) delay(25) }
        assertTrue(cues.tokens().first { it.id == timer.id }.delivered)
    }
    private fun pending(): PendingIntent? = PendingIntent.getBroadcast(context,0,Intent(context,RoutineReminderReceiver::class.java).apply {
        action="com.thanu.steady.ROUTINE_REMINDER"; data=Uri.parse("steady://routine/0")
    },PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
}
