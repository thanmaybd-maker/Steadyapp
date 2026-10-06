package com.thanu.steady.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val PERSONALIZATION_MIGRATION_4_5 = object : Migration(4,5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expanded_profile ADD COLUMN ringMetrics TEXT NOT NULL DEFAULT 'STEPS,FOCUS,HABITS'")
        db.execSQL("ALTER TABLE expanded_profile ADD COLUMN waterQuickMl TEXT NOT NULL DEFAULT '100,250,500'")
        db.execSQL("ALTER TABLE expanded_profile ADD COLUMN waterUnit TEXT NOT NULL DEFAULT 'ML'")
        db.execSQL("ALTER TABLE expanded_profile ADD COLUMN reviewCards TEXT NOT NULL DEFAULT 'FOCUS,HABITS,WORKOUTS,WATER,SLEEP'")
    }
}
