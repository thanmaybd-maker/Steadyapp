package com.thanu.steady.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "weekly_review")
data class WeeklyReviewEntity(
    @PrimaryKey val weekEnd: LocalDate,
    val indicatorSleep: String,
    val indicatorLearning: String,
    val indicatorBuilding: String,
    val indicatorHealth: String,
    val indicatorConnection: String,
    val helped: String,
    val tooDemanding: String,
    val changedEvidence: String,
    val adjustment: String,
    val updatedAt: Instant
) {
    fun toDomain() = com.thanu.steady.domain.WeeklyReview(weekEnd, indicatorSleep,
        indicatorLearning, indicatorBuilding, indicatorHealth, indicatorConnection,
        helped, tooDemanding, changedEvidence, adjustment, updatedAt)
}
