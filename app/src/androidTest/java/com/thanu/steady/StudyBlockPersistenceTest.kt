package com.thanu.steady

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import com.thanu.steady.domain.*
import kotlinx.coroutines.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class StudyBlockPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "blocks-synthetic-${UUID.randomUUID()}.db"
    private val key = ByteArray(32).also(java.security.SecureRandom()::nextBytes)
    private var database: SteadyDatabase? = null
    private val day = LocalDate.parse("2026-01-05")
    private val clock = Clock.fixed(Instant.parse("2026-01-05T08:00:00Z"),ZoneOffset.UTC)
    private fun open(): SteadyDatabase = Room.databaseBuilder(context,SteadyDatabase::class.java,name)
        .openHelperFactory(SupportOpenHelperFactory(key.copyOf()))
        .addMigrations(SteadyDatabase.MIGRATION_1_2,SteadyDatabase.MIGRATION_2_3,EXPANDED_MIGRATION_3_4,PERSONALIZATION_MIGRATION_4_5)
        .build().also { database = it }
    @Before fun setup() { System.loadLibrary("sqlcipher") }
    @After fun cleanup() { database?.close(); context.deleteDatabase(name); key.fill(0) }

    @Test fun repeatedAdoptionRenameRestartDeleteAndBackupKeepOneAssociation() = runBlocking {
        var db = open()
        fun repository() = ExpandedRepository({ requireNotNull(database) },clock,PreferencesRepository { requireNotNull(database) })
        val proposal = StudyBlockRules.templates(day,"UTC",240,listOf("Synthetic morning","Synthetic afternoon","Synthetic evening"))[0]
        val ids = (1..20).map { async(Dispatchers.IO) { repository().adoptStudyBlock(proposal) } }.awaitAll()
        assertEquals(1, ids.distinct().size)
        assertEquals(1,db.expandedDao().tasks(day.toString(),day.toString()).size)
        val plan = requireNotNull(db.expandedDao().task(ids[0]))
        repository().saveTask(plan.copy(title = "Synthetic renamed",day = day.plusDays(1).toString()))
        assertEquals(plan.id,repository().adoptStudyBlock(proposal))
        assertEquals("Synthetic renamed",db.expandedDao().task(plan.id)!!.title)
        val saved = repository().studyBlocks(day).single()
        repository().saveStudyBlock(saved.copy(primer = "Synthetic primer", reminder = true))
        assertEquals(day.plusDays(1).toString(),db.expandedDao().task(plan.id)!!.day)
        assertTrue(runCatching { repository().saveStudyBlock(saved.copy(title = "Stale synthetic edit")) }.isFailure)
        repository().studyBlockSettings(StudyBlockSettings(reminders = true))
        val encoded = PortableCodec.encode(RecoveryRepository { db }.snapshot())
        val decoded = PortableCodec.decode(encoded)
        db.close(); db = open()
        assertEquals(plan.id,repository().studyBlocks(day).single().adoptedPlanId)
        assertEquals("Synthetic primer",db.expandedDao().task(plan.id)!!.notes)
        assertTrue(repository().studyBlockSettings().reminders)
        repository().deleteTask(plan.id)
        assertEquals(plan.id,repository().adoptStudyBlock(proposal))
        assertTrue(db.expandedDao().tasks(day.toString(),day.plusDays(1).toString()).isEmpty())
        RecoveryRepository { db }.replace(decoded)
        assertEquals(1,db.expandedDao().tasks(day.toString(),day.plusDays(1).toString()).size)
        assertEquals(plan.id,repository().studyBlocks(day).single().adoptedPlanId)
        assertTrue(repository().studyBlockSettings().reminders)
        assertTrue(db.expandedDao().notesInRange(clock.millis()-1,clock.millis()+1).isEmpty())
    }
}
