package com.thanu.steady.data

import androidx.room.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.thanu.steady.domain.SafetyPlan
import com.thanu.steady.domain.SupportContact
import java.time.Instant

@Entity(tableName = "safety_migration")
data class SafetyMigration(@PrimaryKey val id: Int = 1, val complete: Boolean)
@Dao interface SafetyMigrationDao {
    @Query("SELECT * FROM safety_migration WHERE id = 1") suspend fun get(): SafetyMigration?
    @Upsert suspend fun save(value: SafetyMigration)
}
@Entity(tableName = "safety_draft")
data class SafetyDraftEntity(@PrimaryKey val id: Int = 1, val content: String)
@Dao interface SafetyDraftDao {
    @Query("SELECT * FROM safety_draft WHERE id = 1") suspend fun get(): SafetyDraftEntity?
    @Upsert suspend fun save(value: SafetyDraftEntity)
    @Query("DELETE FROM safety_draft") suspend fun clear()
}
val SAFETY_MIGRATION_1_2 = object : androidx.room.migration.Migration(1,2) {
    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `safety_draft` (`id` INTEGER NOT NULL, `content` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}
@Database(entities = [SafetyPlanEntity::class, SupportContactEntity::class, SafetyMigration::class, SafetyDraftEntity::class,
    PrivateSafetyNote::class,PrivateSafetyNoteDraft::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class PrivateSafetyDatabase : RoomDatabase() {
    abstract fun safetyDao(): SafetyDao
    abstract fun migrationDao(): SafetyMigrationDao
    abstract fun draftDao(): SafetyDraftDao
    abstract fun notesDao(): PrivateSafetyNotesDao
}

@Serializable private data class PrivateDraft(val fields: Map<String,String?>,val contacts: List<SupportContact>)
private fun SafetyPlan.draft() = PrivateDraft(mapOf("warning" to warningSigns,"coping" to copingSteps,"places" to safePeoplePlaces,
    "environment" to environmentSteps,"clinic" to clinicName,"phone" to clinicPhone,"followUp" to followUpAt,
    "reviewed" to reviewedByUserAt?.toString(),"status" to clinicianReviewStatus,"updated" to updatedAt.toString()),contacts)
private fun PrivateDraft.plan() = SafetyPlan(warningSigns = fields["warning"].orEmpty(),copingSteps = fields["coping"].orEmpty(),
    safePeoplePlaces = fields["places"].orEmpty(),environmentSteps = fields["environment"].orEmpty(),clinicName = fields["clinic"].orEmpty(),
    clinicPhone = fields["phone"].orEmpty(),followUpAt = fields["followUp"],reviewedByUserAt = fields["reviewed"]?.let(Instant::parse),
    clinicianReviewStatus = fields["status"].orEmpty(),updatedAt = Instant.parse(fields.getValue("updated")),contacts = contacts)

/** Independent key and store. Copy, verify and only then clear legacy organiser Safety. */
class PrivateSafetyRepository(private val privateProvider: () -> PrivateSafetyDatabase,
    private val legacyProvider: () -> SteadyDatabase) {
    private val mutex = Mutex()
    private suspend fun database(): PrivateSafetyDatabase = mutex.withLock {
        val safe = privateProvider()
        if (safe.migrationDao().get()?.complete == true) return@withLock safe
        val old = legacyProvider()
        val legacy = old.withTransaction { old.safetyDao().getPlan() to old.safetyDao().getContacts() }
        check(legacy.first != null || legacy.second.isEmpty())
        safe.withTransaction {
            val existing = safe.safetyDao().getPlan()
            check(existing == null || legacy.first == null || existing == legacy.first)
            legacy.first?.let { safe.safetyDao().saveFullPlan(it, legacy.second) }
            check(safe.safetyDao().getPlan() == (legacy.first ?: existing))
            if (legacy.first != null) check(safe.safetyDao().getContacts() == legacy.second)
        }
        old.withTransaction {
            // Refuse to clear if another writer changed the old store during the copy.
            check(old.safetyDao().getPlan() == legacy.first && old.safetyDao().getContacts() == legacy.second)
            old.safetyDao().clearContacts()
            old.safetyDao().clearPlan()
        }
        safe.migrationDao().save(SafetyMigration(complete = true))
        safe
    }
    suspend fun load(): Pair<SafetyPlanEntity?, List<SupportContactEntity>> {
        val db = database()
        return db.withTransaction { db.safetyDao().getPlan() to db.safetyDao().getContacts() }
    }
    suspend fun save(plan: SafetyPlanEntity, contacts: List<SupportContactEntity>) {
        require(plan.id == 1 && contacts.size <= 100 && contacts.all { it.planId == 1 })
        require(contacts.map { it.id }.distinct().size == contacts.size)
        val db = database()
        db.withTransaction { db.safetyDao().saveFullPlan(plan, contacts); db.draftDao().clear() }
    }
    suspend fun loadDraft(): SafetyPlan? = database().draftDao().get()?.let {
        Json.decodeFromString<PrivateDraft>(it.content).plan()
    }
    suspend fun saveDraft(plan: SafetyPlan) {
        require(plan.contacts.size <= 100)
        val value = Json.encodeToString(plan.draft())
        require(value.toByteArray(Charsets.UTF_8).size <= 8 * 1024 * 1024)
        database().draftDao().save(SafetyDraftEntity(content = value))
    }
    suspend fun notes(): List<PrivateSafetyNote> = database().notesDao().all().also { values -> values.forEach { it.validate() } }
    suspend fun noteDraft(): PrivateSafetyNote? = database().notesDao().draft()?.let {
        Json.decodeFromString<PrivateSafetyNote>(it.content).also { note -> note.validate(draft=true) }
    }
    suspend fun saveNoteDraft(note: PrivateSafetyNote) {
        note.validate(draft=true)
        database().notesDao().save(PrivateSafetyNoteDraft(content=Json.encodeToString(note)))
    }
    suspend fun discardNoteDraft() = database().notesDao().clearDraft()
    suspend fun saveNote(note: PrivateSafetyNote) {
        note.validate()
        val db = database()
        db.withTransaction {
            val dao=db.notesDao(); val old=dao.note(note.id)
            check(note.revision == (old?.revision ?: 0))
            check(old != null || dao.count() < 5000)
            dao.save(note.copy(created=old?.created ?: note.created,updated=maxOf(note.updated,old?.updated ?: note.updated),revision=note.revision+1))
            if(dao.draft()?.let { Json.decodeFromString<PrivateSafetyNote>(it.content).id } == note.id) dao.clearDraft()
        }
    }
    suspend fun pinNote(id: String,revision: Int,now: Long) {
        val db=database()
        db.withTransaction {
            val dao=db.notesDao(); val old=requireNotNull(dao.note(id))
            check(old.revision == revision)
            dao.save(old.copy(pinned=!old.pinned,updated=maxOf(now,old.updated),revision=revision+1))
        }
    }
    suspend fun deleteNote(id: String,revision: Int) {
        val db=database()
        db.withTransaction {
            val dao=db.notesDao(); val old=dao.note(id) ?: return@withTransaction
            check(old.revision == revision); dao.delete(id)
            val draft=dao.draft()?.let { Json.decodeFromString<PrivateSafetyNote>(it.content) }
            if(draft?.id == id) dao.clearDraft()
        }
    }
}
