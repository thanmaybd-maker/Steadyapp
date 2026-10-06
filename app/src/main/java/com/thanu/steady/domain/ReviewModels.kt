package com.thanu.steady.domain

import java.time.Instant
import java.time.LocalDate

data class WeeklyReview(
    val weekEnd: LocalDate,
    val indicatorSleep: String = "",
    val indicatorLearning: String = "",
    val indicatorBuilding: String = "",
    val indicatorHealth: String = "",
    val indicatorConnection: String = "",
    val helped: String = "",
    val tooDemanding: String = "",
    val changedEvidence: String = "",
    val adjustment: String = "",
    val updatedAt: Instant = Instant.now()
)

object ThirtyDaySequence {
    val periods = listOf(
        "Days 1-7: Use the routine immediately. Preserve anchors.",
        "Days 8-14: Consolidate. Try one tested behavior. Don't postpone work.",
        "Days 15-21: Add the timer and review. Measure friction.",
        "Days 22-30: Choose two habits to keep, remove one friction, pick one outcome."
    )
}
