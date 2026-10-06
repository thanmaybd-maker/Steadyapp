package com.thanu.steady.domain
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class StudyBlockRulesTest {
    @Test fun identitiesDoNotDependOnRenamedTitlesAndConflictChecksRespectBoundaries() {
        val day = LocalDate.parse("2026-01-05")
        val first = StudyBlockRules.templates(day, "UTC", 240, listOf("Morning", "Afternoon", "Evening"))
        val renamed = StudyBlockRules.templates(day, "UTC", 240, listOf("A", "B", "C"))
        assertEquals(first.map { it.id }, renamed.map { it.id })
        assertNotEquals(first[0].id, StudyBlockRules.templates(day.plusDays(1), "UTC", 240, listOf("A", "B", "C"))[0].id)
        assertEquals(first[0].planId, first[0].copy(title = "Changed").planId)
        val start = ReminderRules.instant(day, 540, java.time.ZoneId.of("UTC"), 240)
        assertEquals(1, StudyBlockRules.conflicts(first[0], listOf("overlap" to WallInterval(start, start + 60_000), "adjacent" to WallInterval(start - 60_000, start))))
        assertEquals(0, StudyBlockRules.conflicts(first[0].copy(adoptedPlanId = first[0].planId), listOf(first[0].planId to WallInterval(start, start + 60_000))))
    }
    @Test(expected = IllegalArgumentException::class) fun invalidLeadTimeIsRejected() {
        StudyBlockRules.templates(LocalDate.parse("2026-01-05"), "UTC", 240, listOf("A", "B", "C"))[0].copy(leadMinutes = 121).validate()
    }
}
