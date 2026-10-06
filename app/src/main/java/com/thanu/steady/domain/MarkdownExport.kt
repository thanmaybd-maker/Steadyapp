package com.thanu.steady.domain

import java.time.LocalDate

class ExportPolicy {
    fun generateMarkdown(date: LocalDate, plan: DailyPlan?, review: WeeklyReview?,
        includeSensitive: Boolean = false, includeEvidence: Boolean = false,
        includeIndicators: Boolean = includeSensitive): String = buildString {
        fun field(label: String, value: String) {
            val escaped = value.replace("\\", "\\\\").replace(Regex("([`*_{}\\[\\]<>#])"), "\\\\$1")
                .replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\n  ")
            append("- **$label**: $escaped\n")
        }
        append("# Steady export: $date\n\n")
        if (plan != null) {
            append("## Daily plan\n")
            field("Logical day", plan.logicalDay.toString())
            field("Timezone", plan.zoneId.id)
            field("Day boundary (minutes)", plan.boundaryMinutes.toString())
            field("Mode", plan.mode.name)
            field("Study", plan.studyTask)
            field("Build", plan.buildTask)
            field("Next action", plan.nextAction)
            if (includeSensitive) field("Health", plan.healthTask)
            if (includeEvidence) {
                plan.studyEvidence?.let { field("Study evidence", it) }
                plan.buildEvidence?.let { field("Build evidence", it) }
            }
            append('\n')
        }
        if (review != null) {
            append("## Week ending ${review.weekEnd}\n")
            field("Helped", review.helped)
            field("Too demanding", review.tooDemanding)
            field("Changed evidence", review.changedEvidence)
            field("Adjustment", review.adjustment)
            if (includeIndicators) {
                field("Sleep", review.indicatorSleep)
                field("Learning", review.indicatorLearning)
                field("Building", review.indicatorBuilding)
                field("Health", review.indicatorHealth)
                field("Connection", review.indicatorConnection)
            }
        }
        if (plan == null && review == null) append("_No records for this date._\n")
    }
}
