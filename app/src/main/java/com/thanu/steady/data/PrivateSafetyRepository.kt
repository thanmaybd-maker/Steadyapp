package com.thanu.steady.data

import androidx.room.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Entity(tableName = "safety_migration")
data class SafetyMigration(@PrimaryKey val id: Int = 1, val complete: Boolean)
@Dao interface SafetyMigrationDao {
    @Query("SELECT * FROM safety_migration WHERE id = 1") suspend fun get(): SafetyMigration?
    @Upsert suspend fun save(value: SafetyMigration)
}
@Database(entities = [SafetyPlanEntity::class, SupportContactEntity::class, SafetyMigration::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class PrivateSafetyDatabase : RoomDatabase() {
    abstract fun safetyDao(): SafetyDao
    abstract fun migrationDao(): SafetyMigrationDao
}

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
        database().safetyDao().saveFullPlan(plan, contacts)
    }
}
