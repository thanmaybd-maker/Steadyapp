package com.thanu.steady

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import kotlinx.coroutines.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class LegacyOgImportTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "legacy-og-synthetic-${UUID.randomUUID()}.db"
    private val key = ByteArray(32).also(java.security.SecureRandom()::nextBytes)
    private var opened: SteadyDatabase? = null
    private val at = 1767614400000L
    private val day = "2026-01-05"
    private val fixture = """{"plans":[{"dateStr":"2026-01-05","title":"Synthetic plan","description":"Synthetic notes","category":"Study","timeSlot":"Morning","estimatedMinutes":30,"isCompleted":false,"priority":"High"}],"sessions":[{"dateStr":"2026-01-05","subject":"Synthetic subject","durationMinutes":25,"mode":"Pomodoro (25m)","rating":4,"notes":"Synthetic notes","timestamp":1767614400000}],"foodIdeas":[{"title":"Synthetic recipe","prepTimeMinutes":10,"benefits":"Unverified claim","ingredients":"Synthetic pantry","instructions":"Synthetic preparation","isFavorite":true,"category":"Quick Snack"}],"reviews":[{"dateStr":"2026-01-05","moodRating":4,"energyRating":3,"highlight":"Synthetic highlight","distractionOrObstacle":"Synthetic obstacle","tomorrowPriority":"Synthetic priority","aiInsight":"Generated draft"}],"safetyNotes":[{"content":"Synthetic private exclusion fixture"}]}"""
    @Before fun setup() { check(context.packageName == "com.thanu.steady.qa"); System.loadLibrary("sqlcipher") }
    @After fun cleanup() { opened?.close(); context.deleteDatabase(name); key.fill(0) }
    private fun db() = Room.databaseBuilder(context,SteadyDatabase::class.java,name)
        .openHelperFactory(SupportOpenHelperFactory(key.copyOf())).build().also { opened=it }
    @Test fun atomicAppendConcurrentRepeatEditDeletionReopenAndBackupPreserveOwnerRecords() = runBlocking {
        var db=db(); var dao=db.expandedDao()
        val ownerReview=Reflection(UUID.randomUUID().toString(),day,day,helped="Synthetic existing prose",updated=at)
        val ownerTask=PlanItem(UUID.randomUUID().toString(),day,"Synthetic existing task",created=at,updated=at,zone="UTC",boundary=240)
        dao.save(ownerReview); dao.save(ownerTask)
        val import=LegacyOgCodec.decode(fixture,"UTC",at)
        val importer=LegacyOgImporter { db }
        val results=coroutineScope { List(8) { async(Dispatchers.IO) { importer.apply(import) } }.awaitAll() }
        assertEquals(3,results.sumOf { it.added })
        assertTrue(dao.reflection(day,day) == ownerReview)
        assertTrue(dao.task(ownerTask.id) == ownerTask)
        val imported=import.archive.tasks.single()
        dao.save(imported.copy(title="Synthetic renamed import")); dao.deleteFood(import.archive.foods.single().id)
        db.close(); db=db(); dao=db.expandedDao()
        assertEquals(0,LegacyOgImporter { db }.apply(import).added)
        assertTrue(dao.task(imported.id)?.title == "Synthetic renamed import")
        assertTrue(dao.foods().isEmpty())
        assertTrue(dao.activeSessions().isEmpty() && dao.segments(import.archive.sessions.single().id).isEmpty())
        assertTrue(dao.notesInRange(0,at+1).isEmpty())
        val payload=PortableCodec.encode(RecoveryRepository { db }.snapshot())
        assertFalse(payload.contains("Synthetic private exclusion fixture") || payload.contains("safetyNotes"))
        RecoveryRepository { db }.replace(PortableCodec.decode(payload))
        assertEquals(0,LegacyOgImporter { db }.apply(import).added)
        assertTrue(dao.reflection(day,day) == ownerReview && dao.foods().isEmpty())
    }
    @Test fun midImportRefusalRollsBackRecordsAndReceiptsAndRetryCommitsAllFourCollections() = runBlocking {
        val db=db(); val dao=db.expandedDao()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER synthetic_og_refusal BEFORE INSERT ON food_idea BEGIN SELECT RAISE(ABORT,'Synthetic import refusal'); END")
        val import=LegacyOgCodec.decode(fixture,"UTC",at)
        assertTrue(runCatching { LegacyOgImporter { db }.apply(import) }.isFailure)
        assertTrue(dao.tasks(day,day).isEmpty() && db.portableDao().allSessions().isEmpty() && db.portableDao().allNotes().isEmpty())
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER synthetic_og_refusal")
        assertEquals(4,LegacyOgImporter { db }.apply(import).added)
        assertTrue(dao.reflection(day,day)?.mood == 4 && dao.foods().single().favorite)
        assertTrue(db.portableDao().allNotes().size == 4 && dao.habits().isEmpty())
        assertFalse(context.getDatabasePath(name).readBytes().toString(Charsets.ISO_8859_1).contains("Synthetic plan"))
    }
}
