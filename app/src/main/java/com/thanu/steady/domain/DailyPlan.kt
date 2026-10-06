package com.thanu.steady.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class DayMode { STANDARD, MINIMUM }

data class DailyPlan(
    val logicalDay: LocalDate,
    val zoneId: ZoneId,
    val boundaryMinutes: Int,
    val mode: DayMode,
    val healthTask: String,
    val studyTask: String,
    val buildTask: String,
    val nextAction: String,
    val studyEvidence: String? = null,
    val buildEvidence: String? = null,
    val shutdownAt: Instant? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
