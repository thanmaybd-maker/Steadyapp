package com.thanu.steady.domain

import java.time.*
import java.time.temporal.ChronoUnit

enum class ActivityState { IDLE, RUNNING, PAUSED, INTERRUPTED, COMPLETED, STOPPED, DISCARDED }
data class ActivityClock(val wall: Long, val elapsed: Long, val boot: Long)
data class ActivityTiming(val state: ActivityState = ActivityState.IDLE, val targetMillis: Long? = null,
    val activeMillis: Long = 0, val wallAnchor: Long? = null, val elapsedAnchor: Long? = null,
    val deadlineElapsed: Long? = null, val boot: Long = 0, val generation: Int = 0)

/** The display ticker does not own time; every transition is based on persisted anchors. */
class ActivityEngine {
    fun active(t: ActivityTiming, clock: ActivityClock): Long {
        val delta = if (t.state == ActivityState.RUNNING && t.boot == clock.boot && t.elapsedAnchor != null)
            (clock.elapsed - t.elapsedAnchor).coerceAtLeast(0) else 0
        return (t.activeMillis + delta).let { if (t.targetMillis == null) it else it.coerceAtMost(t.targetMillis) }
    }
    fun resume(t: ActivityTiming, clock: ActivityClock): ActivityTiming {
        require(t.state in setOf(ActivityState.IDLE, ActivityState.PAUSED, ActivityState.INTERRUPTED))
        require(t.targetMillis == null || t.targetMillis > t.activeMillis)
        return t.copy(state = ActivityState.RUNNING, wallAnchor = clock.wall, elapsedAnchor = clock.elapsed,
            deadlineElapsed = t.targetMillis?.let { clock.elapsed + it - t.activeMillis },
            boot = clock.boot, generation = t.generation + 1)
    }
    fun close(t: ActivityTiming, clock: ActivityClock, state: ActivityState): ActivityTiming {
        require(state in setOf(ActivityState.PAUSED, ActivityState.STOPPED, ActivityState.COMPLETED, ActivityState.DISCARDED))
        require(t.state in setOf(ActivityState.RUNNING, ActivityState.PAUSED, ActivityState.INTERRUPTED))
        if (state == ActivityState.COMPLETED) require(t.state == ActivityState.RUNNING &&
            t.deadlineElapsed != null && t.boot == clock.boot && clock.elapsed >= t.deadlineElapsed)
        return t.copy(state = state, activeMillis = active(t, clock), wallAnchor = null,
            elapsedAnchor = null, deadlineElapsed = null, generation = t.generation + 1)
    }
    fun reconcile(t: ActivityTiming, clock: ActivityClock): ActivityTiming {
        if (t.state != ActivityState.RUNNING) return t
        val drift = if (t.wallAnchor != null && t.elapsedAnchor != null)
            kotlin.math.abs((clock.wall - t.wallAnchor) - (clock.elapsed - t.elapsedAnchor)) else Long.MAX_VALUE
        return if (t.boot != clock.boot || drift > 60_000) t.copy(state = ActivityState.INTERRUPTED,
            wallAnchor = null, elapsedAnchor = null, deadlineElapsed = null, generation = t.generation + 1) else t
    }
}

object HabitRules {
    fun scheduled(day: LocalDate, anchor: LocalDate, weekdays: Int, everyDays: Int): Boolean {
        require(weekdays in 0..127 && everyDays in 1..3650)
        val days = ChronoUnit.DAYS.between(anchor, day)
        return days >= 0 && days % everyDays == 0L && weekdays and (1 shl (day.dayOfWeek.value - 1)) != 0
    }
    fun state(quantity: Double, target: Double, skipped: Boolean = false): String {
        require(quantity.isFinite() && quantity >= 0 && target.isFinite() && target > 0)
        return when { skipped -> "SKIPPED"; quantity >= target -> "COMPLETED"; quantity > 0 -> "PARTIAL"; else -> "PENDING" }
    }
}

data class WallInterval(val start: Long, val end: Long) { init { require(end >= start) } }
object ActivityTotals {
    fun unionMillis(intervals: List<WallInterval>): Long {
        var end = Long.MIN_VALUE
        var total = 0L
        intervals.sortedBy { it.start }.forEach { span ->
            total += (span.end - maxOf(span.start, end)).coerceAtLeast(0)
            end = maxOf(end, span.end)
        }
        return total
    }
    fun dayBounds(day: LocalDate, zone: ZoneId, boundary: Int): WallInterval {
        require(boundary in 0..1439)
        val time = LocalTime.of(boundary / 60, boundary % 60)
        return WallInterval(day.atTime(time).atZone(zone).toInstant().toEpochMilli(),
            day.plusDays(1).atTime(time).atZone(zone).toInstant().toEpochMilli())
    }
    fun split(interval: WallInterval, zone: ZoneId, boundary: Int): Map<LocalDate, Long> {
        val result = linkedMapOf<LocalDate, Long>()
        val policy = LogicalDayPolicy()
        var cursor = interval.start
        while (cursor < interval.end) {
            val day = policy.getLogicalDay(Instant.ofEpochMilli(cursor), zone, boundary)
            val bound = dayBounds(day, zone, boundary)
            val end = minOf(interval.end, bound.end)
            check(end > cursor)
            result[day] = (result[day] ?: 0) + end - cursor
            cursor = end
        }
        return result
    }
}
