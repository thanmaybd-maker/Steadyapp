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
class CompletionWorkflowTest {
    private lateinit var context: Context
    private val open = mutableListOf<androidx.room.RoomDatabase>()
    private val names = mutableListOf<String>()
    private val key = ByteArray(32).also(java.security.SecureRandom()::nextBytes)
    private val clock = Clock.fixed(Instant.parse("2026-01-05T12:00:00Z"),ZoneOffset.UTC)
    private fun id() = UUID.randomUUID().toString()
    @Before fun setup() { context = InstrumentationRegistry.getInstrumentation().targetContext; System.loadLibrary("sqlcipher") }
    private fun db(): SteadyDatabase {
        val name = "completion-${id()}.db"; names += name
        return Room.databaseBuilder(context,SteadyDatabase::class.java,name).openHelperFactory(SupportOpenHelperFactory(key.copyOf()))
            .addMigrations(SteadyDatabase.MIGRATION_1_2,SteadyDatabase.MIGRATION_2_3,EXPANDED_MIGRATION_3_4).build().also(open::add)
    }
    private fun safe(name: String): PrivateSafetyDatabase = Room.databaseBuilder(context,PrivateSafetyDatabase::class.java,name)
        .openHelperFactory(SupportOpenHelperFactory(key.copyOf())).addMigrations(SAFETY_MIGRATION_1_2).build().also(open::add)
    @After fun cleanup() { open.forEach { it.close() }; names.forEach { context.deleteDatabase(it) }; key.fill(0) }

    @Test fun programmeNotesSetsEffortAndRestAreDurableAndStaleSafe() = runBlocking {
        val db = db(); var now = ActivityClock(clock.millis(),1000,4)
        val activity = ActivityRepository({ db },{ now },PreferencesRepository { db })
        val programme = IntervalProgram(10,5,2)
        val interval = activity.create("WORKOUT","INTERVALS","Synthetic intervals",programme.totalSeconds,notes = "Synthetic note",program = programme)
        assertEquals(programme,activity.program(interval.id)); assertEquals("Synthetic note",db.expandedDao().session(interval.id)!!.notes)
        now = now.copy(wall = now.wall+11000,elapsed = now.elapsed+11000)
        val paused = activity.transition(interval.id,interval.generation,ActivityState.PAUSED)
        now = now.copy(wall = now.wall+300000,elapsed = now.elapsed+300000)
        assertEquals(IntervalPhase("REST",1,4),activity.program(interval.id)!!.phase(activity.activeMillis(paused)))
        activity.transition(paused.id,paused.generation,ActivityState.STOPPED)
        val strength = activity.create("WORKOUT","STRENGTH","Synthetic strength",null)
        val set = ExerciseSet(id(),strength.id,"Synthetic exercise",5,10.0,doneAt = now.wall)
        activity.saveSet(set); activity.saveSet(set.copy(reps = 7)); assertEquals(1,activity.sets(strength.id).size)
        activity.effort(strength.id,8); assertEquals(8,db.expandedDao().session(strength.id)!!.effort)
        val rest = activity.startRest(strength.id,10)
        assertNull(activity.completeRest(rest.id,rest.generation))
        assertTrue(RecoveryRepository { db }.snapshot().expanded!!.notes.none { it.id.startsWith("rest:") })
        now = now.copy(wall = now.wall+10000,elapsed = now.elapsed+10000)
        assertNotNull(activity.completeRest(rest.id,rest.generation)); assertNull(activity.completeRest(rest.id,rest.generation))
        activity.transition(strength.id,strength.generation,ActivityState.PAUSED)
        assertNull(activity.rest(strength.id))
    }

    @Test fun scratchpadExportIsScopedAndExcludesFormAndSchedulerMetadata() = runBlocking {
        val db = db(); val preferences = PreferencesRepository { db }
        val repo = ExpandedRepository({ db },clock,preferences)
        val activity = ActivityRepository({ db },{ ActivityClock(clock.millis(),1000,1) },preferences)
        val s = activity.create("FOCUS","STUDY","Synthetic study",60)
        repo.saveNote(SessionNote("scratchpad:${s.id}",s.id,"Synthetic formula ΔG = -RT ln(K)",clock.millis()))
        repo.saveNote(SessionNote("draft:editor",null,"Synthetic unsaved input",clock.millis()))
        repo.saveNote(SessionNote("delivery:2026-01-05:HABIT:${id()}",null,"RESERVED",clock.millis()))
        val recovery = RecoveryRepository { db }
        val text = recovery.markdown(LocalDate.parse("2026-01-05"),LocalDate.parse("2026-01-05"),setOf("FOCUS"),true,clock)
        assertTrue(text.contains("Synthetic formula ΔG = -RT ln(K)")); assertFalse(text.contains("Synthetic unsaved input")); assertFalse(text.contains("RESERVED"))
        assertFalse(recovery.markdown(LocalDate.parse("2026-01-05"),LocalDate.parse("2026-01-05"),setOf("FOCUS"),false,clock).contains("ΔG"))
        assertFalse(recovery.markdown(LocalDate.parse("2026-01-06"),LocalDate.parse("2026-01-06"),setOf("FOCUS"),true,clock).contains("ΔG"))
        val backup = recovery.snapshot().expanded!!
        assertTrue(backup.notes.none { it.id.startsWith("draft:") || it.id.startsWith("delivery:") })
    }

    @Test fun legacyRestoreKeepsExpandedHistoryButInterruptsCurrentActivity() = runBlocking {
        val db = db(); val preferences = PreferencesRepository { db }
        val activity = ActivityRepository({ db },{ ActivityClock(clock.millis(),1000,1) },preferences)
        val s = activity.create("FOCUS","STUDY","Synthetic retained",60)
        db.expandedDao().save(WaterLog(id(),"2026-01-05",250,clock.millis(),"Asia/Kolkata",240))
        RecoveryRepository { db }.replace(RecoverySnapshot(emptyList(),emptyList(),emptyList()))
        val old = db.expandedDao().session(s.id)!!
        assertEquals("INTERRUPTED",old.state); assertNull(old.deadlineElapsed); assertEquals(s.generation+1,old.generation)
        assertEquals(1,db.expandedDao().water("2026-01-05","2026-01-05").size)
        assertNull(activity.complete(s.id,s.generation))
    }

    @Test fun privateDraftMigratesAndReopensWithoutEnteringPortableScope() = runBlocking {
        val name = "private-draft-${id()}.db"; names += name
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("schemas/private-safety-1.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        val helper = SupportOpenHelperFactory(key.copyOf()).create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for(i in 0 until entities.length()) { val e = entities.getJSONObject(i); db.execSQL(e.getString("createSql").replace("\${TABLE_NAME}",e.getString("tableName"))) }
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase,oldVersion: Int,newVersion: Int) { error("Unexpected fixture upgrade") }
            }).build())
        helper.writableDatabase.execSQL("INSERT INTO safety_migration (id,complete) VALUES (1,1)"); helper.close()
        var safety = safe(name); val organiser = db()
        var repository = PrivateSafetyRepository({ safety },{ organiser })
        val draft = SafetyPlan(copingSteps = "Synthetic private draft",updatedAt = clock.instant())
        repository.saveDraft(draft)
        assertEquals(2,safety.openHelper.readableDatabase.version)
        safety.close(); safety = safe(name); repository = PrivateSafetyRepository({ safety },{ organiser })
        assertTrue(repository.loadDraft()?.copingSteps == draft.copingSteps)
        assertFalse(PortableCodec.encode(RecoveryRepository { organiser }.snapshot()).contains(draft.copingSteps))
        repository.save(SafetyPlanEntity(1,"",draft.copingSteps,"","","","",null,null,"unreviewed",clock.instant()),emptyList())
        assertNull(repository.loadDraft()); assertTrue(repository.load().first?.copingSteps == draft.copingSteps)
    }
}
