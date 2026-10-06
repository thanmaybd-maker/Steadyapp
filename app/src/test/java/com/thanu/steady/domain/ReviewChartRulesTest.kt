package com.thanu.steady.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReviewChartRulesTest {
    @Test fun actualFocusAndRestRemainIndependentAndEmptyRatioIsAbsent() {
        val day = LocalDate.parse("2026-01-05")
        val start = day.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val rows = ReviewChartRules.daily(day, day.plusDays(1), listOf(
            ReviewInterval("FOCUS", start, start + 120_000, "UTC", 0),
            ReviewInterval("FOCUS", start + 60_000, start + 180_000, "UTC", 0),
            ReviewInterval("BREAK", start + 180_000, start + 240_000, "UTC", 0),
            ReviewInterval("WORKOUT", start, start + 600_000, "UTC", 0)))
        assertEquals(180_000L, rows[0].focusMillis); assertEquals(60_000L, rows[0].restMillis)
        assertEquals(0.75, rows[0].ratio!!, 0.0001); assertNull(rows[1].ratio)
    }
    @Test fun historicalBoundaryAndDstSplitRealSegmentsWithoutFillingMissingDays() {
        val zone = ZoneId.of("America/New_York")
        val day = LocalDate.parse("2026-03-07")
        val begin = day.plusDays(1).atTime(3, 50).atZone(zone).toInstant().toEpochMilli()
        val rows = ReviewChartRules.daily(day, day.plusDays(1), listOf(
            ReviewInterval("FOCUS", begin, begin + 1_200_000, zone.id, 240)))
        assertEquals(600_000L, rows[0].focusMillis); assertEquals(600_000L, rows[1].focusMillis)
        assertEquals(23 * 3_600_000L, ActivityTotals.dayBounds(day, zone, 240).let { it.end - it.start })
    }
    @Test fun consistencyUsesOnlyActualCompletedEligibleDaysAndMarkerIsBounded() {
        assertEquals(2 to 3, ReviewChartRules.consistency(listOf("COMPLETED", "NOT_DUE", "COMPLETED", "SKIPPED", "COMPLETED", "MISSING", "COMPLETED", "COMPLETED", "PENDING")))
        assertNull(ReviewChartRules.marker(9, WallInterval(10, 20)))
        assertEquals(0.5f, ReviewChartRules.marker(15, WallInterval(10, 20))!!, 0.0001f)
        assertNull(ReviewChartRules.marker(20, WallInterval(10, 20)))
    }
}
