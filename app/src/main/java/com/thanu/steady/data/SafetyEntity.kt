package com.thanu.steady.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "safety_plan")
data class SafetyPlanEntity(
    @PrimaryKey
    val id: Int = 1,
    val warningSigns: String,
    val copingSteps: String,
    val safePeoplePlaces: String,
    val environmentSteps: String,
    val clinicName: String,
    val clinicPhone: String,
    val followUpAt: String?,
    val reviewedByUserAt: Instant?,
    val clinicianReviewStatus: String,
    val updatedAt: Instant
)

@Entity(tableName = "support_contact")
data class SupportContactEntity(
    @PrimaryKey
    val id: String,
    val planId: Int = 1,
    val role: String,
    val displayName: String,
    val phone: String,
    val note: String?,
    val sortOrder: Int
)
