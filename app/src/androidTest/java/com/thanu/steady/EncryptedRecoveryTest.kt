package com.thanu.steady

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import com.thanu.steady.domain.*
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EncryptedRecoveryTest {
    private lateinit var context: Context
    private val open = mutableListOf<SteadyDatabase>()
    private val names = mutableListOf<String>()
    private val key = ByteArray(32) { (it + 1).toByte() }
    private val day = LocalDate.of(2026, 1, 2)
    private val instant = Instant.parse("2026-01-02T12:00:00Z")

    @Before fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        System.loadLibrary("sqlcipher")
    }
    private fun database(name: String = "synthetic-${UUID.randomUUID()}.db", secret: ByteArray = key): SteadyDatabase {
        if (name !in names) names += name
        return Room.databaseBuilder(context, SteadyDatabase::class.java, name)
            .openHelperFactory(SupportOpenHelperFactory(secret.copyOf()))
            .addMigrations(SteadyDatabase.MIGRATION_1_2, SteadyDatabase.MIGRATION_2_3, EXPANDED_MIGRATION_3_4,PERSONALIZATION_MIGRATION_4_5).build().also(open::add)
    }
    private fun plan(text: String = "SYNTHETIC_ORGANISER_CANARY") = DailyPlanEntity(day,
        ZoneId.of("Asia/Kolkata"), 240, DayMode.MINIMUM, "", text, "Synthetic build", "Synthetic next",
        "Synthetic evidence", null, null, instant, instant)
    private fun safety(text: String) = SafetyPlanEntity(1, text, "", "", "", "", "", null, null, "unreviewed", instant)

    @After fun cleanup() { open.forEach { it.close() }; names.forEach { context.deleteDatabase(it) } }

    @Test fun encryptedCrudSurvivesReopenAndCanaryIsAbsentFromFiles() = runBlocking {
        val db = database()
        val name = names.last()
        db.dailyPlanDao().insertPlan(plan())
        assertEquals(plan(), db.dailyPlanDao().getPlan(day))
        context.getDatabasePath(name).parentFile!!.listFiles()!!.filter { it.name.startsWith(name) }.forEach {
            assertFalse(String(it.readBytes(), Charsets.ISO_8859_1).contains("SYNTHETIC_ORGANISER_CANARY"))
        }
        db.close()
        assertEquals(plan(), database(name).dailyPlanDao().getPlan(day))
        val wrongKey = database(name, ByteArray(32) { 9 })
        try { wrongKey.dailyPlanDao().getPlan(day); fail("Wrong key must not open records") } catch (_: Exception) { }
    }

    @Test fun portableRestorePreservesDestinationSafetyAndStopsImportedTimers() = runBlocking {
        val source = database()
        source.dailyPlanDao().insertPlan(plan())
        source.safetyDao().insertPlan(safety("SYNTHETIC_SOURCE_DEVICE_ONLY"))
        val timer = TimerSessionEntity(UUID.randomUUID().toString(), day.toString(), "break", 60_000, 60_000,
            TimerState.RUNNING, instant, instant.plusSeconds(60), 70_000, 1, 3, null, 2)
        source.timerDao().insertSession(timer)
        val payload = RecoveryCodec.encode(RecoveryRepository { source }.snapshot())
        assertFalse(payload.contains("SYNTHETIC_SOURCE_DEVICE_ONLY"))
        assertFalse(payload.contains("warningSigns"))
        val password = "synthetic recovery password".toCharArray()
        val archive = BackupService().createEncryptedBackup(payload, password)
        val decoded = RecoveryCodec.decode(BackupService().restoreEncryptedBackup(archive, password)!!)
        val destination = database(secret = ByteArray(32) { 42 })
        destination.safetyDao().insertPlan(safety("SYNTHETIC_DESTINATION_DEVICE_ONLY"))
        val contact = SupportContactEntity(UUID.randomUUID().toString(), 1, "first", "Synthetic contact", "000", null, 0)
        destination.safetyDao().insertContacts(listOf(contact))
        RecoveryRepository { destination }.replace(decoded)
        assertEquals(plan(), destination.dailyPlanDao().getPlan(day))
        assertEquals("SYNTHETIC_DESTINATION_DEVICE_ONLY", destination.safetyDao().getPlan()!!.warningSigns)
        assertEquals(listOf(contact), destination.safetyDao().getContacts())
        assertEquals(TimerState.CANCELLED, destination.timerDao().getSession(timer.id)!!.state)
        assertNull(destination.timerDao().getSession(timer.id)!!.targetElapsedTime)
    }

    @Test fun failedImportRollsBackAllEligibleTables() = runBlocking {
        val db = database()
        db.dailyPlanDao().insertPlan(plan("Synthetic original"))
        db.safetyDao().insertPlan(safety("SYNTHETIC_LOCAL_ONLY"))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER synthetic_failure BEFORE INSERT ON daily_plans BEGIN SELECT RAISE(ABORT, 'Synthetic write failure'); END")
        try {
            RecoveryRepository { db }.replace(RecoverySnapshot(listOf(plan("Synthetic replacement")), emptyList(), emptyList()))
            fail("Import must fail")
        } catch (_: Exception) { }
        assertEquals("Synthetic original", db.dailyPlanDao().getPlan(day)!!.studyTask)
        assertEquals("SYNTHETIC_LOCAL_ONLY", db.safetyDao().getPlan()!!.warningSigns)
    }

    @Test fun invalidPayloadTypesUnknownFieldsAndDuplicateDatesAreRejected() {
        val payload = RecoveryCodec.encode(RecoverySnapshot(listOf(plan()), emptyList(), emptyList()))
        fun rejected(value: String) {
            try { RecoveryCodec.decode(value); fail("Invalid payload accepted") } catch (_: Exception) { }
        }
        rejected(payload.replace("\"schema\":1", "\"schema\":99"))
        rejected(payload.replace("\"boundary\":240", "\"boundary\":\"240\""))
        rejected(payload.replace("\"schema\":1", "\"schema\":1,\"unknown\":[]"))
        rejected(payload.replace("\"boundary\":240", "\"boundary\":1440"))
        val objectText = org.json.JSONObject(payload).getJSONArray("plans").getJSONObject(0).toString()
        rejected("{\"schema\":1,\"plans\":[$objectText,$objectText],\"reviews\":[],\"timers\":[]}")
        rejected("{".repeat(1000))
    }
}
