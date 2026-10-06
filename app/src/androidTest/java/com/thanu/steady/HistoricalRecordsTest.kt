package com.thanu.steady

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import com.thanu.steady.domain.*
import com.thanu.steady.ui.activityMillis
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class HistoricalRecordsTest {
    private lateinit var context: Context
    private lateinit var database: SteadyDatabase
    private lateinit var name: String
    private val clock=Clock.fixed(Instant.parse("2026-01-05T12:00:00Z"),ZoneOffset.UTC)
    private fun id()=UUID.randomUUID().toString()
    @Before fun setup() {
        context=InstrumentationRegistry.getInstrumentation().targetContext; System.loadLibrary("sqlcipher")
        name="history-synthetic-${id()}.db"
        database=Room.databaseBuilder(context,SteadyDatabase::class.java,name)
            .openHelperFactory(SupportOpenHelperFactory(ByteArray(32).also(java.security.SecureRandom()::nextBytes)))
            .addMigrations(SteadyDatabase.MIGRATION_1_2,SteadyDatabase.MIGRATION_2_3,EXPANDED_MIGRATION_3_4,PERSONALIZATION_MIGRATION_4_5).build()
    }
    @After fun cleanup() { database.close(); context.deleteDatabase(name) }
    @Test fun changingCurrentZoneKeepsOriginalLogicalDayAndCorrectionsRetainReflection()=runBlocking {
        val prefs=PreferencesRepository { database }; prefs.update { it.copy(zoneId="America/Adak",boundaryMinutes=240) }
        val repo=ExpandedRepository({ database },clock,prefs)
        val start=Instant.parse("2026-01-04T14:30:00Z").toEpochMilli()
        val session=ActivitySession(id(),"FOCUS","STUDY","Synthetic old-zone focus",state="STOPPED",activeMillis=120000,
            started=start,ended=start+120000,zone="Pacific/Kiritimati",boundary=240,updated=clock.millis())
        database.expandedDao().save(session)
        database.expandedDao().save(ActivitySegment(id(),session.id,start,start+120000,0,120000,120000,session.zone,240))
        val day=LocalDate.parse("2026-01-05")
        val reflection=Reflection(id(),day.toString(),day.toString(),helped="Synthetic written reflection",updated=clock.millis())
        repo.saveReflection(reflection)
        val before=repo.snapshot(day,day)
        assertEquals(120000L,activityMillis(before,"FOCUS"))
        assertEquals(1,before.sessions.size)
        ActivityRepository({ database },{ ActivityClock(clock.millis(),1000,1) },prefs).editHistory(session.id,60000,"Synthetic correction",null)
        val corrected=repo.snapshot(day,day)
        assertEquals(60000L,activityMillis(corrected,"FOCUS"))
        assertEquals(before.reflection,corrected.reflection)
        ActivityRepository({ database },{ ActivityClock(clock.millis(),1000,1) },prefs).deleteHistory(session.id)
        assertEquals(0L,activityMillis(repo.snapshot(day,day),"FOCUS"))
        assertEquals(before.reflection,repo.snapshot(day,day).reflection)
    }
    @Test fun decadeOfRecordsProducesBoundedTodayAndReportsMeasuredQueryAndSaveTimes()=runBlocking {
        val day=LocalDate.parse("2026-01-05")
        val water=(0 until 3650).map { offset -> WaterLog(id(),day.minusDays(offset.toLong()).toString(),250,clock.millis()-offset*86400000L,"Asia/Kolkata",240) }
        val tasks=water.map { PlanItem(id(),it.day,"Synthetic historical task",created=it.at,updated=it.at,zone=it.zone,boundary=it.boundary) }
        database.portableDao().restoreWater(water); database.portableDao().restoreTasks(tasks)
        val repo=ExpandedRepository({ database },clock,PreferencesRepository { database })
        val queries=(0 until 20).map {
            val begin=android.os.SystemClock.elapsedRealtimeNanos()
            val snapshot=repo.snapshot(day,day)
            assertEquals(1,snapshot.water.size); assertEquals(1,snapshot.tasks.size)
            (android.os.SystemClock.elapsedRealtimeNanos()-begin)/1000000.0
        }.sorted()
        val saves=(0 until 20).map {
            val begin=android.os.SystemClock.elapsedRealtimeNanos()
            repo.saveWater(WaterLog(id(),day.toString(),100,clock.millis(),"Asia/Kolkata",240))
            (android.os.SystemClock.elapsedRealtimeNanos()-begin)/1000000.0
        }.sorted()
        println("STEADY_SYNTHETIC_PERFORMANCE records=7300 period=1day query_p95_ms=${queries[18]} save_p95_ms=${saves[18]}")
        assertEquals(21,repo.snapshot(day,day).water.size)
    }
}
