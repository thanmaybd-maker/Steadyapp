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
}
