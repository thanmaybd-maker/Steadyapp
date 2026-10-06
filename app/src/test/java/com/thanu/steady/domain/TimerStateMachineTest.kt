package com.thanu.steady.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TimerStateMachineTest {
    private val machine = TimerStateMachine()
    private val now = Instant.parse("2026-01-02T12:00:00Z")
    private fun idle() = TimerSession(durationMs = 60_000, remainingMs = 60_000)

    @Test fun pauseAndResumePreserveRemainingAndInvalidatePreviousGeneration() {
        val running = machine.start(idle(), now, 1000, 7)
        val paused = machine.pause(running, 21_000)
        assertEquals(40_000, paused.remainingMs)
        assertNull(paused.targetElapsedTime)
        val resumed = machine.start(paused, now.plusSeconds(30), 31_000, 7)
        assertEquals(running.id, resumed.id)
        assertEquals(71_000L, resumed.targetElapsedTime)
        assertEquals(running.generation + 2, resumed.generation)
    }
    @Test fun restartingCompletedTimerCreatesNewIdAndClearsCompletion() {
        val running = machine.start(idle(), now, 1000, 7)
        val finished = machine.complete(running, now.plusSeconds(60))
        val restarted = machine.start(finished, now.plusSeconds(90), 91_000, 7)
        assertNotEquals(running.id, restarted.id)
        assertNull(restarted.completedAt)
        assertEquals(60_000, restarted.remainingMs)
        assertEquals(151_000L, restarted.targetElapsedTime)
    }
    @Test fun interruptionClearsDeadlinesAndClampsClockJump() {
        val running = machine.start(idle(), now, 1000, 7)
        val interrupted = machine.interrupt(running, now.minusSeconds(120))
        assertEquals(TimerState.INTERRUPTED, interrupted.state)
        assertEquals(60_000, interrupted.remainingMs)
        assertNull(interrupted.targetElapsedTime)
        assertNull(interrupted.targetWallTime)
        assertEquals(running.generation + 1, interrupted.generation)
    }
}
