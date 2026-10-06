package com.thanu.steady.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate

@Dao
interface ReviewDao {
    @Query("SELECT * FROM weekly_review WHERE weekEnd = :date LIMIT 1")
    suspend fun getReview(date: LocalDate): WeeklyReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: WeeklyReviewEntity)

    @Query("SELECT * FROM daily_plans WHERE logicalDay >= :start AND logicalDay <= :end ORDER BY logicalDay ASC")
    suspend fun getPlansInWindow(start: LocalDate, end: LocalDate): List<DailyPlanEntity>
}
