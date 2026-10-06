package com.thanu.steady

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import com.thanu.steady.domain.*
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ExpandedFoundationTest {
    private lateinit var context: Context
    private val databases = mutableListOf<androidx.room.RoomDatabase>()
    private val names = mutableListOf<String>()
    private val key = ByteArray(32) { 71 }
    private val clock = Clock.fixed(Instant.parse("2026-01-05T12:00:00Z"), ZoneOffset.UTC)
    @Before fun setup() { context = InstrumentationRegistry.getInstrumentation().targetContext; System.loadLibrary("sqlcipher") }
    private fun db(name: String = "expanded-synthetic-${UUID.randomUUID()}.db"): SteadyDatabase {
        if (name !in names) names += name
        return Room.databaseBuilder(context, SteadyDatabase::class.java, name)
            .openHelperFactory(SupportOpenHelperFactory(key.copyOf()))
            .addMigrations(SteadyDatabase.MIGRATION_1_2, SteadyDatabase.MIGRATION_2_3, EXPANDED_MIGRATION_3_4)
            .build().also(databases::add)
    }
    @After fun cleanup() { databases.forEach { it.close() }; names.forEach { context.deleteDatabase(it) } }

    @Test fun schemaThreeUpgradesWithoutLosingExistingEncryptedPreferences() = runBlocking {
        val name = "expanded-migration-${UUID.randomUUID()}.db"; names += name
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val schema = JSONObject(assets.open("schemas/3.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        val helper = SupportOpenHelperFactory(key.copyOf()).create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                        val indices = entity.getJSONArray("indices")
                        for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                    }
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, old: Int, new: Int) { error("Unexpected synthetic upgrade") }
            }).build())
        helper.writableDatabase.execSQL("INSERT INTO app_preferences (id,appLock,hideRecents,pauseEnabled,zoneId,boundaryMinutes,cueFlags,softerTheme,vegetarian,avoidFoods) VALUES (1,0,1,1,'Asia/Kolkata',240,2,0,1,'Synthetic avoidance')")
        helper.close()
        val upgraded = db(name)
        assertTrue(upgraded.preferencesDao().get()!!.pauseEnabled)
        assertEquals("Synthetic avoidance", upgraded.preferencesDao().get()!!.avoidFoods)
        assertEquals(4, upgraded.openHelper.readableDatabase.version)
        assertNull(upgraded.expandedDao().profile())
        assertTrue(upgraded.expandedDao().water("2026-01-01", "2026-01-31").isEmpty())
    }

    @Test fun partialFocusIsActualTimeAndStaleCompletionCannotDuplicateIt() = runBlocking {
        val database = db()
        var now = ActivityClock(clock.millis(), 1000, 7)
        val repository = ActivityRepository({ database }, { now }, PreferencesRepository { database })
        val running = repository.create("FOCUS", "STUDY", "Synthetic focus", 1500)
        now = now.copy(wall = now.wall + 120_000, elapsed = now.elapsed + 120_000)
        val stopped = repository.transition(running.id, running.generation, ActivityState.STOPPED)
        assertEquals(120_000L, stopped.activeMillis)
        assertNull(repository.complete(running.id, running.generation))
        assertEquals(120_000L, database.expandedDao().segments(running.id).sumOf { it.activeMillis })
        val next = repository.create("FOCUS", "BUILD", "Synthetic next", 60)
        now = now.copy(wall = now.wall + 60_000, elapsed = now.elapsed + 60_000)
        assertNotNull(repository.complete(next.id, next.generation))
        assertNull(repository.complete(next.id, next.generation))
        assertEquals(60_000L, database.expandedDao().session(next.id)!!.activeMillis)
    }

    @Test fun habitLogIsIdempotentAndRenamingPreservesCompletedSnapshot() = runBlocking {
        val database = db()
        val repository = ExpandedRepository({ database }, clock, PreferencesRepository { database })
        val day = repository.logicalDay()
        val habitId = UUID.randomUUID().toString()
        val version = HabitVersion(UUID.randomUUID().toString(), habitId, day.toString(), "Synthetic original", "COUNT", "items", 2.0,
            anchorDay = day.toString(), created = clock.millis())
        repository.saveHabit(version)
        val occurrence = database.expandedDao().occurrences(day.toString(), day.toString()).single()
        val event = UUID.randomUUID().toString()
        repository.logHabit(occurrence.id, 2.0, event)
        repository.logHabit(occurrence.id, 2.0, event)
        assertEquals(1, database.expandedDao().logs(occurrence.id).size)
        repository.saveHabit(version.copy(id = UUID.randomUUID().toString(), title = "Synthetic renamed", target = 4.0))
        assertEquals("Synthetic original", database.expandedDao().versions(listOf(occurrence.versionId)).single().title)
        assertEquals("Synthetic renamed", database.expandedDao().versionsForDay(day.plusDays(1).toString()).single().title)
        repository.undoHabit(event)
        assertEquals(0.0, database.expandedDao().occurrence(occurrence.id)!!.quantity, 0.0)
    }

    @Test fun safetyMovesToIndependentStoreAndStillOpensWhenOrganiserIsUnavailable() = runBlocking {
        val organiser = db()
        val safetyName = "private-synthetic-${UUID.randomUUID()}.db"; names += safetyName
        val safe = Room.databaseBuilder(context, PrivateSafetyDatabase::class.java, safetyName)
            .openHelperFactory(SupportOpenHelperFactory(ByteArray(32) { 83 })).build().also(databases::add)
        val plan = SafetyPlanEntity(1, "Synthetic private fixture", "", "", "", "", "", null, null, "unreviewed", clock.instant())
        organiser.safetyDao().insertPlan(plan)
        val migrating = PrivateSafetyRepository({ safe }, { organiser })
        assertEquals(plan, migrating.load().first)
        assertNull(organiser.safetyDao().getPlan())
        val independent = PrivateSafetyRepository({ safe }, { error("Synthetic organiser unavailable") })
        assertEquals(plan, independent.load().first)
        assertFalse(RecoveryCodec.encode(RecoveryRepository { organiser }.snapshot()).contains("Synthetic private fixture"))
    }
}
