package com.thanu.steady.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TimerDao {
    @Query("SELECT * FROM timer_session ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestSession(): TimerSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TimerSessionEntity)

    @Query("SELECT * FROM timer_session WHERE id = :id LIMIT 1")
    suspend fun getSession(id: String): TimerSessionEntity?

    @Query("SELECT * FROM timer_session ORDER BY startedAt")
    suspend fun getAll(): List<TimerSessionEntity>

    @Query("DELETE FROM timer_session")
    suspend fun clear()

    @Query("SELECT * FROM timer_session ORDER BY startedAt DESC LIMIT 1")
    fun observeLatest(): kotlinx.coroutines.flow.Flow<TimerSessionEntity?>
}
