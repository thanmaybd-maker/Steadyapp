package com.thanu.steady.domain

import java.time.Instant

class TimerStateMachine {
    fun start(session: TimerSession, nowWall: Instant, nowElapsed: Long, bootMarker: Long): TimerSession {
        if (session.state == TimerState.RUNNING) return session
        
        return session.copy(
            state = TimerState.RUNNING,
            startedAt = session.startedAt ?: nowWall,
            targetWallTime = nowWall.plusMillis(session.remainingMs),
            targetElapsedTime = nowElapsed + session.remainingMs,
            bootMarker = bootMarker,
            generation = session.generation + 1
        )
    }

    fun pause(session: TimerSession, nowElapsed: Long): TimerSession {
        if (session.state != TimerState.RUNNING || session.targetElapsedTime == null) return session
        
        val newRemaining = maxOf(0L, session.targetElapsedTime - nowElapsed)
        return session.copy(
            state = TimerState.PAUSED,
            remainingMs = newRemaining,
            targetWallTime = null,
            targetElapsedTime = null,
            generation = session.generation + 1
        )
    }

    fun stop(session: TimerSession): TimerSession {
        return session.copy(
            state = TimerState.CANCELLED,
            remainingMs = session.durationMs,
            targetWallTime = null,
            targetElapsedTime = null,
            generation = session.generation + 1
        )
    }

    fun complete(session: TimerSession, nowWall: Instant): TimerSession {
        if (session.state != TimerState.RUNNING) return session
        return session.copy(
            state = TimerState.COMPLETED,
            remainingMs = 0L,
            completedAt = nowWall,
            targetWallTime = null,
            targetElapsedTime = null,
            generation = session.generation + 1
        )
    }
}
