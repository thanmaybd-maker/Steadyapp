package com.thanu.steady.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Serializable data class StudyBlockProposal(val id: String, val day: String, val window: String, val title: String,
    val minute: Int, val durationMinutes: Int = 25, val category: String = "STUDY", val primer: String = "",
    val reminder: Boolean = false, val leadMinutes: Int = 5, val zone: String, val boundary: Int,
    val adoptedPlanId: String? = null, val revision: Int = 0) {
    fun validate() {
        UUID.fromString(id); LocalDate.parse(day); ZoneId.of(zone)
        require(window in setOf("MORNING", "AFTERNOON", "EVENING"))
        require(title.isNotBlank() && title.length <= 500 && primer.length <= 10_000)
        require(minute in 0..1439 && durationMinutes in 1..1440 && leadMinutes in 0..120 && boundary in 0..1439)
        require(category in setOf("STUDY", "BUILD") && revision >= 0)
        adoptedPlanId?.let { require(it == UUID.nameUUIDFromBytes("study-block:$id".toByteArray(Charsets.UTF_8)).toString()) }
    }
    val planId: String get() = adoptedPlanId ?: UUID.nameUUIDFromBytes("study-block:$id".toByteArray(Charsets.UTF_8)).toString()
}
@Serializable data class StudyBlockSettings(val reminders: Boolean = false)

object StudyBlockRules {
    fun templates(day: LocalDate, zone: String, boundary: Int, titles: List<String>): List<StudyBlockProposal> {
        require(titles.size == 3)
        return listOf("MORNING" to 540, "AFTERNOON" to 840, "EVENING" to 1140).mapIndexed { index, (window, minute) ->
            StudyBlockProposal(UUID.nameUUIDFromBytes("block-template:$day:$window".toByteArray(Charsets.UTF_8)).toString(),
                day.toString(), window, titles[index], minute, zone = zone, boundary = boundary).also { it.validate() }
        }
    }
    fun conflicts(proposal: StudyBlockProposal, plans: List<Pair<String, WallInterval>>): Int {
        proposal.validate()
        val start = ReminderRules.instant(LocalDate.parse(proposal.day), proposal.minute, ZoneId.of(proposal.zone), proposal.boundary)
        val stop = start + proposal.durationMinutes * 60_000L
        return plans.count { (id, span) -> id != proposal.adoptedPlanId && span.start < stop && span.end > start }
    }
}
