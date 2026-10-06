package com.thanu.steady

import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.thanu.steady.data.*
import com.thanu.steady.domain.SafetyPlan
import com.thanu.steady.ui.SafetyNotesViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.time.*
import java.util.UUID

class PrivateNotesPersistenceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val key=ByteArray(32).also(java.security.SecureRandom()::nextBytes)
    private val names=mutableListOf<String>()
    private val opened=mutableListOf<androidx.room.RoomDatabase>()
    private val clock=Clock.fixed(Instant.parse("2026-01-05T12:00:00Z"),ZoneOffset.UTC)
    private fun name() = "extra-private-synthetic-${UUID.randomUUID()}.db".also(names::add)
    @Before fun setup() { check(context.packageName == "com.thanu.steady.qa"); System.loadLibrary("sqlcipher") }
    @After fun cleanup() { opened.forEach { it.close() }; names.forEach { context.deleteDatabase(it) }; key.fill(0) }
    private fun safe(name: String) = Room.databaseBuilder(context,PrivateSafetyDatabase::class.java,name)
        .openHelperFactory(SupportOpenHelperFactory(key.copyOf())).addMigrations(SAFETY_MIGRATION_1_2,SAFETY_MIGRATION_2_3).build().also(opened::add)
    private fun organiser() = Room.databaseBuilder(context,SteadyDatabase::class.java,name())
        .openHelperFactory(SupportOpenHelperFactory(key.copyOf())).build().also(opened::add)

    @Test fun schemaTwoUpgradeNotesPinsDraftReopenAndOrdinaryExclusionKeepPrivateFields() = runBlocking {
        val name=name()
        val schema=JSONObject(instrumentation.context.assets.open("schemas/private-safety-2.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        val helper=SupportOpenHelperFactory(key.copyOf()).create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object: SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    val entities=schema.getJSONArray("entities")
                    for(index in 0 until entities.length()) {
                        val entity=entities.getJSONObject(index)
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
                        val indices=entity.getJSONArray("indices")
                        for(i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
                    }
                }
                override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase,oldVersion: Int,newVersion: Int) { error("Unexpected fixture upgrade") }
            }).build())
        val canary="Synthetic private exclusion fixture"
        helper.writableDatabase.execSQL("INSERT INTO safety_plan (id,warningSigns,copingSteps,safePeoplePlaces,environmentSteps,clinicName,clinicPhone,followUpAt,reviewedByUserAt,clinicianReviewStatus,updatedAt) VALUES (1,?,'','','','','',NULL,NULL,'unreviewed',?)",arrayOf(canary,clock.millis()))
        helper.writableDatabase.execSQL("INSERT INTO safety_migration (id,complete) VALUES (1,1)")
        helper.close()
        var safe=safe(name)
        var repository=PrivateSafetyRepository({ safe },{ error("Organiser unavailable") })
        assertEquals(3,safe.openHelper.readableDatabase.version)
        assertTrue(repository.load().first?.warningSigns == canary)
        val note=PrivateSafetyNote(UUID.randomUUID().toString(),"Synthetic title",canary,"GROUNDING",created=clock.millis(),updated=clock.millis())
        repository.saveNoteDraft(note); repository.saveNote(note)
        assertNull(repository.noteDraft())
        var saved=repository.notes().single()
        repository.pinNote(saved.id,saved.revision,clock.millis())
        assertTrue(repository.notes().single().pinned)
        assertTrue(runCatching { repository.saveNote(saved.copy(content="Stale synthetic fixture")) }.isFailure)
        saved=repository.notes().single()
        val draft=saved.copy(content="Synthetic retained private draft")
        repository.saveNoteDraft(draft)
        safe.close(); safe=safe(name); repository=PrivateSafetyRepository({ safe },{ error("Organiser unavailable") })
        assertTrue(repository.notes().single() == saved)
        assertTrue(repository.noteDraft() == draft)
        val ordinary=organiser()
        val payload=PortableCodec.encode(RecoveryRepository { ordinary }.snapshot())
        assertFalse(payload.contains(canary) || payload.contains(draft.content) || payload.contains(note.id))
        RecoveryRepository { ordinary }.replace(PortableCodec.decode(payload))
        assertTrue(repository.notes().single() == saved && repository.noteDraft() == draft)
        repository.deleteNote(saved.id,saved.revision)
        assertTrue(repository.notes().isEmpty()); assertNull(repository.noteDraft())
        safe.close()
        assertFalse(context.getDatabasePath(name).readBytes().toString(Charsets.ISO_8859_1).contains(canary))
    }

    @Test fun refusedPrivateNoteSaveRetainsDurableDraftAndExistingPlan() = runBlocking {
        val safe=safe(name()); safe.migrationDao().save(SafetyMigration(complete=true))
        val repository=PrivateSafetyRepository({ safe },{ error("Organiser unavailable") })
        repository.saveDraft(SafetyPlan(copingSteps="Synthetic structured draft",updatedAt=clock.instant()))
        safe.openHelper.writableDatabase.execSQL("CREATE TRIGGER synthetic_note_refusal BEFORE INSERT ON private_safety_note BEGIN SELECT RAISE(ABORT,'Synthetic storage refusal'); END")
        val store=ViewModelStore()
        lateinit var model: SafetyNotesViewModel
        instrumentation.runOnMainSync { model=SafetyNotesViewModel(repository,clock); store.put("private-synthetic",model) }
        try {
            withTimeout(5000) { model.state.first { !it.loading } }
            instrumentation.runOnMainSync { model.edit(); model.field("content","Synthetic retained draft"); model.save() }
            withTimeout(5000) { model.state.first { !it.busy && it.error != null } }
            assertTrue(model.state.value.draft != null && model.state.value.editorOpen)
            assertTrue(repository.noteDraft()?.content == model.state.value.draft?.content)
            assertTrue(repository.notes().isEmpty())
            assertTrue(repository.loadDraft()?.copingSteps == "Synthetic structured draft")
        } finally { instrumentation.runOnMainSync { store.clear() } }
    }
}
