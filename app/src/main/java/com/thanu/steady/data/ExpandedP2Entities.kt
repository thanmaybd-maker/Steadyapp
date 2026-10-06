package com.thanu.steady.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "money_guard_logs")
data class MoneyGuardEntity(
    @PrimaryKey val id: String,
    val day: String,
    val amountCent: Long,
    val currency: String,
    val category: String,
    val note: String
)

@Serializable
@Entity(tableName = "people_relationships")
data class PeopleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val connectionType: String,
    val lastContactedDay: String,
    val notes: String
)

@Serializable
@Entity(tableName = "learn_build_modules")
data class LearnBuildEntity(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "LEARN" or "BUILD"
    val repositoryUrl: String?,
    val progressPercent: Int
)

@Serializable
@Entity(tableName = "ocr_captures")
data class OcrCaptureEntity(
    @PrimaryKey val id: String,
    val captureDay: String,
    val rawText: String,
    val confidence: Float,
    val parsedDataJson: String
)
