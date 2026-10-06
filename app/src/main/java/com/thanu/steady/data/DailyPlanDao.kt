package com.thanu.steady.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyPlanDao {
    @Query("SELECT * FROM daily_plans WHERE logicalDay = :day LIMIT 1")
    suspend fun getPlan(day: LocalDate): DailyPlanEntity?

    @Query("SELECT * FROM daily_plans WHERE logicalDay = :day LIMIT 1")
    fun getPlanFlow(day: LocalDate): Flow<DailyPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: DailyPlanEntity)

    @Query("SELECT * FROM daily_plans ORDER BY logicalDay")
    suspend fun getAll(): List<DailyPlanEntity>

    @Query("DELETE FROM daily_plans")
    suspend fun clear()
}
