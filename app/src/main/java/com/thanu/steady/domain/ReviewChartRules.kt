package com.thanu.steady.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class ReviewInterval(val kind: String, val start: Long, val end: Long, val zone: String, val boundary: Int)
data class DailyFocusRest(val day: LocalDate, val focusMillis: Long, val restMillis: Long) {
    val ratio: Double? get() = if (focusMillis + restMillis == 0L) null else focusMillis.toDouble() / (focusMillis + restMillis)
}

/** Closed recorded segments, grouped by their historical logical-day policy. */
object ReviewChartRules {
    fun daily(start: LocalDate, end: LocalDate, source: List<ReviewInterval>): List<DailyFocusRest> {
        require(!end.isBefore(start) && ChronoUnit.DAYS.between(start, end) <= 366)
        return generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.map { day ->
            fun total(kinds: Set<String>) = ActivityTotals.unionMillis(source.filter { it.kind in kinds && it.end > it.start }.mapNotNull { span ->
                val bounds = ActivityTotals.dayBounds(day, ZoneId.of(span.zone), span.boundary)
                val begin = maxOf(span.start, bounds.start); val stop = minOf(span.end, bounds.end)
                if (stop > begin) WallInterval(begin, stop) else null
            })
            DailyFocusRest(day, total(setOf("FOCUS")), total(setOf("BREAK", "REST")))
        }.toList()
    }
    fun consistency(statuses: List<String>): Pair<Int, Int> {
        var current = 0; var best = 0
        statuses.forEach { status -> when (status) {
            "COMPLETED" -> { current++; best = maxOf(best, current) }
            "NOT_DUE", "SKIPPED", "PENDING" -> Unit
            else -> current = 0
        } }
        return current to best
    }
    fun marker(now: Long, bounds: WallInterval): Float? = if (bounds.end <= bounds.start || now !in bounds.start until bounds.end) null
        else ((now - bounds.start).toDouble() / (bounds.end - bounds.start)).toFloat()
}
