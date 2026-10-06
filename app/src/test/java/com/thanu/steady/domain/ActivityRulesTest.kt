package com.thanu.steady.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ActivityRulesTest {
    private val engine = ActivityEngine()
    private val initial = ActivityClock(1_000_000, 1000, 7)
    @Test fun stoppingAfterTwoMinutesDoesNotGrantTwentyFive() {
        val running = engine.resume(ActivityTiming(targetMillis = 25 * 60_000L), initial)
        val stopped = engine.close(running, initial.copy(wall = 1_120_000, elapsed = 121_000), ActivityState.STOPPED)
        assertEquals(120_000L, stopped.activeMillis)
        assertNull(stopped.deadlineElapsed)
    }
    @Test fun pausedTimeIsExcludedAndResumeInvalidatesOldCallback() {
        val running = engine.resume(ActivityTiming(targetMillis = 300_000), initial)
        val paused = engine.close(running, initial.copy(wall = 1_060_000, elapsed = 61_000), ActivityState.PAUSED)
        assertEquals(60_000L, engine.active(paused, initial.copy(elapsed = 181_000)))
        val resumed = engine.resume(paused, initial.copy(wall = 1_180_000, elapsed = 181_000))
        assertEquals(421_000L, resumed.deadlineElapsed)
        assertEquals(running.generation + 2, resumed.generation)
    }
    @Test fun rebootDoesNotInventContinuedActivity() {
        val running = engine.resume(ActivityTiming(activeMillis = 60_000, targetMillis = 300_000), initial)
        val recovered = engine.reconcile(running, initial.copy(wall = 5_000_000, elapsed = 20_000, boot = 8))
        assertEquals(ActivityState.INTERRUPTED, recovered.state)
        assertEquals(60_000L, recovered.activeMillis)
        assertNull(recovered.deadlineElapsed)
    }
    @Test fun overlappingIntervalsAreCountedOnceAndSplitAtFourAm() {
        assertEquals(300L, ActivityTotals.unionMillis(listOf(WallInterval(0, 200), WallInterval(100, 300))))
        val start = Instant.parse("2026-01-02T03:30:00Z").toEpochMilli()
        val end = Instant.parse("2026-01-02T04:30:00Z").toEpochMilli()
        val split = ActivityTotals.split(WallInterval(start, end), ZoneId.of("UTC"), 240)
        assertEquals(1_800_000L, split[LocalDate.of(2026, 1, 1)])
        assertEquals(1_800_000L, split[LocalDate.of(2026, 1, 2)])
    }
    @Test fun unscheduledDaysAndPartialHabitQuantitiesAreDistinct() {
        val monday = LocalDate.of(2026, 1, 5)
        assertTrue(HabitRules.scheduled(monday, monday, 1, 1))
        assertFalse(HabitRules.scheduled(monday.plusDays(1), monday, 1, 1))
        assertEquals("PARTIAL", HabitRules.state(1.0, 2.0))
        assertEquals("COMPLETED", HabitRules.state(2.0, 2.0))
        assertEquals("SKIPPED", HabitRules.state(0.0, 2.0, true))
    }
}
