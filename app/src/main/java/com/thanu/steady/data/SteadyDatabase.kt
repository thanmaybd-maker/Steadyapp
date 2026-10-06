package com.thanu.steady.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DailyPlanEntity::class, SafetyPlanEntity::class, SupportContactEntity::class, TimerSessionEntity::class, WeeklyReviewEntity::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class SteadyDatabase : RoomDatabase() {
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun safetyDao(): SafetyDao
    abstract fun timerDao(): TimerDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `timer_session` (
                        `id` TEXT NOT NULL, 
                        `logicalDay` TEXT, 
                        `kind` TEXT NOT NULL, 
                        `durationMs` INTEGER NOT NULL, 
                        `remainingMs` INTEGER NOT NULL, 
                        `state` TEXT NOT NULL, 
                        `startedAt` INTEGER, 
                        `targetWallTime` INTEGER, 
                        `targetElapsedTime` INTEGER, 
                        `bootMarker` INTEGER NOT NULL, 
                        `cueFlags` INTEGER NOT NULL, 
                        `completedAt` INTEGER, 
                        `generation` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)
            }
        }
    }
}
