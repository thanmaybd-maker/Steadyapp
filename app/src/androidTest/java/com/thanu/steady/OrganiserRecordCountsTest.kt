package com.thanu.steady

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.*
import org.junit.Assert.*
import java.util.UUID

class OrganiserRecordCountsTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="organiser-count-synthetic-${UUID.randomUUID()}.db"
    private lateinit var db: SteadyDatabase
    private fun id()=UUID.randomUUID().toString()
    @Before fun setup() {
        check(context.packageName == "com.thanu.steady.qa"); System.loadLibrary("sqlcipher")
        db=Room.databaseBuilder(context,SteadyDatabase::class.java,name)
            .openHelperFactory(SupportOpenHelperFactory(ByteArray(32).also(java.security.SecureRandom()::nextBytes))).build()
    }
    @After fun cleanup() { db.close(); context.deleteDatabase(name) }
    @Test fun countsAllDatesAndArchivedRecordsButExcludesDraftsMetadataAndPrivateTables()=runBlocking {
        val dao=db.expandedDao(); val at=1767614400000L; val day="2026-01-05"
        dao.save(PlanItem(id(),day,"Synthetic archived",state="ARCHIVED",created=at,updated=at,zone="UTC",boundary=0))
        dao.save(PlanItem(id(),"2020-01-01","Synthetic historical",created=at,updated=at,zone="UTC",boundary=0))
        dao.save(HabitDefinition(id(),at,archivedDay=day)); dao.save(FoodIdeaRecord(id(),"Synthetic recipe","","",updated=at))
        dao.save(Reflection(id(),day,day,updated=at)); dao.save(WaterLog(id(),day,250,at,"UTC",0))
        dao.save(Capture(id(),"Synthetic archived capture",at,archived=true))
        dao.save(SessionNote(id(),null,"Synthetic ordinary note",at))
        listOf("draft:synthetic","legacy-og:${id()}","ambient:preferences","delivery:synthetic").forEach {
            dao.save(SessionNote(it,null,"Synthetic internal metadata",at))
        }
        val counts=RecoveryRepository { db }.recordCounts()
        assertEquals(2L,counts.tasks); assertEquals(1L,counts.habits); assertEquals(1L,counts.foods)
        assertEquals(1L,counts.reflections); assertEquals(1L,counts.logs); assertEquals(1L,counts.captures); assertEquals(1L,counts.notes)
        assertEquals(0L,counts.legacy); assertEquals(0L,counts.sessions); assertEquals(0L,counts.motion)
        val privateName="private-count-excluded-${id()}.db"
        val safe=Room.databaseBuilder(context,PrivateSafetyDatabase::class.java,privateName)
            .openHelperFactory(SupportOpenHelperFactory(ByteArray(32).also(java.security.SecureRandom()::nextBytes))).build()
        try {
            safe.migrationDao().save(SafetyMigration(complete=true))
            val privateRepo=PrivateSafetyRepository({ safe },{ error("No organiser fallback") })
            privateRepo.saveNote(PrivateSafetyNote(id(),"Synthetic private fixture","Synthetic private content","OTHER",created=at,updated=at))
            assertEquals(counts,RecoveryRepository { db }.recordCounts())
        } finally { safe.close(); context.deleteDatabase(privateName) }
    }
}
