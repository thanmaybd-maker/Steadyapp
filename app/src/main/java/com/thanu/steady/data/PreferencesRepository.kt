package com.thanu.steady.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "app_preferences")
data class AppPreferences(
    @PrimaryKey val id: Int = 1,
    val appLock: Boolean = false,
    val hideRecents: Boolean = true,
    val pauseEnabled: Boolean = false,
    val zoneId: String = "Asia/Kolkata",
    val boundaryMinutes: Int = 240,
    val cueFlags: Int = 3,
    val softerTheme: Boolean = false,
    val vegetarian: Boolean = false,
    val avoidFoods: String = ""
)

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM app_preferences WHERE id = 1")
    fun observe(): Flow<AppPreferences?>
    @Query("SELECT * FROM app_preferences WHERE id = 1")
    suspend fun get(): AppPreferences?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(preferences: AppPreferences)
}

class PreferencesRepository(private val databaseProvider: () -> SteadyDatabase) {
    fun observe(): Flow<AppPreferences> = databaseProvider().preferencesDao().observe().map { it ?: AppPreferences() }
    suspend fun get(): AppPreferences = databaseProvider().preferencesDao().get() ?: AppPreferences()
    suspend fun update(transform: (AppPreferences) -> AppPreferences) {
        val db = databaseProvider()
        db.withTransaction {
            val next = transform(db.preferencesDao().get() ?: AppPreferences())
            require(next.id == 1 && next.boundaryMinutes in 0..1439 && next.cueFlags in 0..7 && next.avoidFoods.length <= 2000)
            java.time.ZoneId.of(next.zoneId)
            db.preferencesDao().save(next)
        }
    }
}
