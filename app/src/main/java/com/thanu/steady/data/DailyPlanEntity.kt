package com.thanu.steady.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thanu.steady.domain.DayMode
import com.thanu.steady.domain.DailyPlan
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "daily_plans")
data class DailyPlanEntity(
    @PrimaryKey
    val logicalDay: LocalDate,
    val zoneId: ZoneId,
    val boundaryMinutes: Int,
    val mode: DayMode,
    val healthTask: String,
    val studyTask: String,
    val buildTask: String,
    val nextAction: String,
    val studyEvidence: String?,
    val buildEvidence: String?,
    val shutdownAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    fun toDomain() = DailyPlan(
        logicalDay = logicalDay,
        zoneId = zoneId,
        boundaryMinutes = boundaryMinutes,
        mode = mode,
        healthTask = healthTask,
        studyTask = studyTask,
        buildTask = buildTask,
        nextAction = nextAction,
        studyEvidence = studyEvidence,
        buildEvidence = buildEvidence,
        shutdownAt = shutdownAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(plan: DailyPlan) = DailyPlanEntity(
            logicalDay = plan.logicalDay,
            zoneId = plan.zoneId,
            boundaryMinutes = plan.boundaryMinutes,
            mode = plan.mode,
            healthTask = plan.healthTask,
            studyTask = plan.studyTask,
            buildTask = plan.buildTask,
            nextAction = plan.nextAction,
            studyEvidence = plan.studyEvidence,
            buildEvidence = plan.buildEvidence,
            shutdownAt = plan.shutdownAt,
            createdAt = plan.createdAt,
            updatedAt = plan.updatedAt
        )
    }
}
