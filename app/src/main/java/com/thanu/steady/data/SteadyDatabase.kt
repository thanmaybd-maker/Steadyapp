package com.thanu.steady.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DailyPlanEntity::class, SafetyPlanEntity::class, SupportContactEntity::class, TimerSessionEntity::class, WeeklyReviewEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class SteadyDatabase : RoomDatabase() {
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun safetyDao(): SafetyDao
    abstract fun timerDao(): TimerDao
    abstract fun reviewDao(): ReviewDao
}
