package com.thanu.steady

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import com.thanu.steady.domain.BackupService
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PortableRecoveryTest {
    private lateinit var context: Context
    private val opened = mutableListOf<SteadyDatabase>()
    private val names = mutableListOf<String>()
    private val at = 1767614400000L
    private val day = "2026-01-05"
    private val zone = "Asia/Kolkata"
    private fun id() = UUID.randomUUID().toString()
    @Before fun setup() { context = InstrumentationRegistry.getInstrumentation().targetContext; System.loadLibrary("sqlcipher") }
    private fun db(): SteadyDatabase {
        val name = "portable-synthetic-${id()}.db"; names += name
        return Room.databaseBuilder(context, SteadyDatabase::class.java,name)
            .openHelperFactory(SupportOpenHelperFactory(ByteArray(32).also(java.security.SecureRandom()::nextBytes)))
            .addMigrations(SteadyDatabase.MIGRATION_1_2,SteadyDatabase.MIGRATION_2_3,EXPANDED_MIGRATION_3_4)
            .build().also(opened::add)
    }
    @After fun cleanup() { opened.forEach { it.close() }; names.forEach { context.deleteDatabase(it) } }
    private fun fixture(): ExpandedArchive {
        val subject = Subject(id(),"Synthetic subject")
        val task = PlanItem(id(),day,"Synthetic task",subjectId=subject.id,created=at,updated=at,zone=zone,boundary=240)
        val habit = HabitDefinition(id(),at)
        val version = HabitVersion(id(),habit.id,day,"Synthetic habit","COUNT","items",2.0,anchorDay=day,created=at)
        val occurrence = HabitOccurrence(id(),habit.id,version.id,day,"COMPLETED",2.0,updated=at)
        val session = ActivitySession(id(),"WORKOUT","STRENGTH","Synthetic workout",taskId=task.id,state="STOPPED",
            activeMillis=120000,started=at,ended=at+120000,zone=zone,boundary=240,updated=at)
        val food = FoodIdeaRecord(id(),"Synthetic recipe","Synthetic ingredients","Synthetic preparation",updated=at)
        val care = CareReminder(id(),"Synthetic instruction",minute=600,updated=at)
        return ExpandedArchive(tasks=listOf(task),habits=listOf(habit),versions=listOf(version),occurrences=listOf(occurrence),
            logs=listOf(HabitLog(id(),occurrence.id,2.0,at)),subjects=listOf(subject),
            topics=listOf(Topic(id(),subject.id,"Synthetic topic")),sessions=listOf(session),
            segments=listOf(ActivitySegment(id(),session.id,at,at+120000,0,120000,120000,zone,240)),
            notes=listOf(SessionNote(id(),session.id,"Synthetic note",at)),
            sets=listOf(ExerciseSet(id(),session.id,"Synthetic movement",3,doneAt=at)),
            templates=listOf(WorkoutTemplate(id(),"Synthetic template","INTERVALS",rounds=2)),
            water=listOf(WaterLog(id(),day,250,at,zone,240)),
            sleep=listOf(SleepLog(id(),day,at-28800000,at,notes="Synthetic sleep",zone=zone,boundary=240)),
            foods=listOf(food),meals=listOf(MealLog(id(),day,food.title,foodId=food.id,at=at,zone=zone,boundary=240)),
            care=listOf(care),careLogs=listOf(CareLog(id(),care.id,day,care.instruction,at)),
            reflections=listOf(Reflection(id(),day,day,helped="Synthetic reflection",updated=at)),
            captures=listOf(Capture(id(),"Synthetic capture",at)),
            interruptions=listOf(InterruptionEvent(id(),outcome="RETURNED",at=at,pauseSeconds=3)),
            observations=listOf(ActivityObservation(id(),"synthetic-source",day,at,at+120000,steps=50,source="PHONE_SENSOR",quality="OBSERVED",boot=0,zone=zone,boundary=240)),
            routes=listOf(RoutePoint(id(),session.id,at,0.0,0.0,5f,0)),
            estimates=listOf(EnergyEstimate(id(),session.id,60.0,3.0,4.2,6.3,"Synthetic validated method","Synthetic reference",at)),
            days=listOf(DaySettings(day,"NORMAL","Synthetic next",zone,240,updated=at)),
            profile=ExpandedProfile(displayName="Synthetic name",onboarded=true),preferences=AppPreferences())
    }
    @Test fun everyEligibleCategoryRoundTripsAndRoutesRequireExplicitScope() = runBlocking {
        val source = db(); val a = fixture(); val dao = source.portableDao()
        dao.restoreTasks(a.tasks)
        dao.restoreHabits(a.habits)
        dao.restoreVersions(a.versions)
        dao.restoreOccurrences(a.occurrences)
        dao.restoreLogs(a.logs)
        dao.restoreSubjects(a.subjects)
        dao.restoreTopics(a.topics)
        dao.restoreSessions(a.sessions)
        dao.restoreSegments(a.segments)
        dao.restoreNotes(a.notes)
        dao.restoreSets(a.sets)
        dao.restoreTemplates(a.templates)
        dao.restoreWater(a.water)
        dao.restoreSleep(a.sleep)
        dao.restoreFoods(a.foods)
        dao.restoreMeals(a.meals)
        dao.restoreCare(a.care)
        dao.restoreCareLogs(a.careLogs)
        dao.restoreReflections(a.reflections)
        dao.restoreCaptures(a.captures)
        dao.restoreInterruptions(a.interruptions)
        dao.restoreObservations(a.observations)
        dao.restoreRoutes(a.routes)
        dao.restoreEstimates(a.estimates)
        dao.restoreDays(a.days)
        source.expandedDao().save(a.profile!!); source.preferencesDao().save(a.preferences!!)
        source.expandedDao().save(SessionNote("draft:synthetic",null,"Synthetic unfinished form",at))
        source.safetyDao().insertPlan(SafetyPlanEntity(1,"SYNTHETIC_PRIVATE_EXCLUDED","","","","","",null,null,"unreviewed",java.time.Instant.ofEpochMilli(at)))
        val ordinary = RecoveryRepository { source }.snapshot()
        val ordinaryArchive = requireNotNull(ordinary.expanded)
        assertTrue(ordinaryArchive.routes.isEmpty())
        assertEquals(1,ordinaryArchive.notes.size)
        val snapshot = RecoveryRepository { source }.snapshot(includeRoutes=true)
        val payload = PortableCodec.encode(snapshot)
        assertFalse(payload.contains("SYNTHETIC_PRIVATE_EXCLUDED"))
        assertFalse(payload.contains("Synthetic unfinished form"))
        assertFalse(payload.contains("warningSigns"))
        val password = "Synthetic portable password".toCharArray()
        val encrypted = BackupService().createEncryptedBackup(payload,password)
        val decoded = PortableCodec.decode(BackupService().restoreEncryptedBackup(encrypted,password)!!)
        val destination = db()
        destination.safetyDao().insertPlan(SafetyPlanEntity(1,"SYNTHETIC_DESTINATION_PRIVATE","","","","","",null,null,"unreviewed",java.time.Instant.ofEpochMilli(at)))
        RecoveryRepository { destination }.replace(decoded)
        assertEquals(snapshot.expanded,RecoveryRepository { destination }.snapshot(includeRoutes=true).expanded)
        assertEquals("SYNTHETIC_DESTINATION_PRIVATE",destination.safetyDao().getPlan()!!.warningSigns)
    }
    @Test fun malformedTypesReferencesDuplicateKeysAndUnknownFieldsNeverReplaceData() = runBlocking {
        val database = db()
        database.expandedDao().save(WaterLog(id(),day,250,at,zone,240))
        val original = RecoveryRepository { database }.snapshot()
        val payload = PortableCodec.encode(original)
        val invalid = listOf(payload.replace("\"schema\":2","\"schema\":2,\"schema\":2"),
            payload.replace("\"schema\":2","\"schema\":\"2\""),
            payload.replace("\"millilitres\":250","\"millilitres\":\"250\""),
            payload.replace("\"millilitres\":250","\"millilitres\":0"),
            payload.replace("\"schema\":2","\"schema\":2,\"privateSafety\":{}"),
            payload.dropLast(1))
        invalid.forEach { candidate ->
            try { RecoveryRepository { database }.replace(PortableCodec.decode(candidate)); fail("Invalid archive accepted") }
            catch (_: Exception) { }
            assertEquals(original.expanded,RecoveryRepository { database }.snapshot().expanded)
        }
        try { PortableCodec.validate(fixture().copy(segments=listOf(ActivitySegment(id(),id(),at,at+1,0,1,1,zone,240)))); fail("Orphan accepted") }
        catch (_: Exception) { }
    }
    @Test fun storageFailureRollsBackExpandedTablesAndRunningImportsCannotResumeByAlarm() = runBlocking {
        val database = db(); val a = fixture()
        database.expandedDao().save(WaterLog(id(),day,100,at,zone,240))
        val original = RecoveryRepository { database }.snapshot()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER synthetic_portable_failure BEFORE INSERT ON water_log BEGIN SELECT RAISE(ABORT, 'Synthetic write failure'); END")
        try { RecoveryRepository { database }.replace(RecoverySnapshot(emptyList(),emptyList(),emptyList(),a)); fail("Restore must fail") }
        catch (_: Exception) { }
        assertEquals(original.expanded,RecoveryRepository { database }.snapshot().expanded)
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER synthetic_portable_failure")
        val running = a.sessions.single().copy(state="RUNNING",wallAnchor=at,elapsedAnchor=1000,deadlineElapsed=5000,boot=7,generation=8)
        RecoveryRepository { database }.replace(RecoverySnapshot(emptyList(),emptyList(),emptyList(),a.copy(sessions=listOf(running))))
        val restored = database.expandedDao().session(running.id)!!
        assertEquals("INTERRUPTED",restored.state); assertNull(restored.deadlineElapsed); assertNull(restored.elapsedAnchor)
        assertEquals(120000L,restored.activeMillis)
    }
}
