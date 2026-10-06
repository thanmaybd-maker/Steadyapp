package com.thanu.steady.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DailyPlanEntity::class, SafetyPlanEntity::class, SupportContactEntity::class,
    TimerSessionEntity::class, WeeklyReviewEntity::class, AppPreferences::class,
    PlanItem::class, HabitDefinition::class, HabitVersion::class, HabitOccurrence::class, HabitLog::class,
    Subject::class, Topic::class, ActivitySession::class, ActivitySegment::class, SessionNote::class,
    ExerciseSet::class, WorkoutTemplate::class, WaterLog::class, SleepLog::class, FoodIdeaRecord::class,
    MealLog::class, CareReminder::class, CareLog::class, Reflection::class, Capture::class,
    InterruptionEvent::class, ActivityObservation::class, RoutePoint::class, EnergyEstimate::class,
    DaySettings::class, ExpandedProfile::class], version = 5, exportSchema = true)
@TypeConverters(Converters::class)
abstract class SteadyDatabase : RoomDatabase() {
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun safetyDao(): SafetyDao
    abstract fun timerDao(): TimerDao
    abstract fun reviewDao(): ReviewDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun expandedDao(): ExpandedDao
    abstract fun portableDao(): PortableDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_preferences` (`id` INTEGER NOT NULL, `appLock` INTEGER NOT NULL, `hideRecents` INTEGER NOT NULL, `pauseEnabled` INTEGER NOT NULL, `zoneId` TEXT NOT NULL, `boundaryMinutes` INTEGER NOT NULL, `cueFlags` INTEGER NOT NULL, `softerTheme` INTEGER NOT NULL, `vegetarian` INTEGER NOT NULL, `avoidFoods` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }
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
