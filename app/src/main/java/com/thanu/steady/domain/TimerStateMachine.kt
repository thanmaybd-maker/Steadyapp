package com.thanu.steady.domain

import java.time.Instant

class TimerStateMachine {
    fun start(session: TimerSession, nowWall: Instant, nowElapsed: Long, bootMarker: Long): TimerSession {
        if (session.state == TimerState.RUNNING) return session
        require(session.durationMs in 1000..86_400_000)
        val resuming = session.state == TimerState.PAUSED || session.state == TimerState.INTERRUPTED
        val remaining = if (resuming && session.remainingMs > 0) session.remainingMs else session.durationMs
        
        return session.copy(
            state = TimerState.RUNNING,
            id = if (resuming) session.id else java.util.UUID.randomUUID().toString(),
            remainingMs = remaining,
            startedAt = if (resuming) session.startedAt ?: nowWall else nowWall,
            completedAt = null,
            targetWallTime = nowWall.plusMillis(remaining),
            targetElapsedTime = nowElapsed + remaining,
            bootMarker = bootMarker,
            generation = session.generation + 1
        )
    }

    fun interrupt(session: TimerSession, nowWall: Instant): TimerSession = session.copy(
        state = TimerState.INTERRUPTED,
        remainingMs = session.targetWallTime?.let { java.time.Duration.between(nowWall, it).toMillis() }
            ?.coerceIn(0, session.durationMs) ?: session.remainingMs,
        targetWallTime = null, targetElapsedTime = null, generation = session.generation + 1
    )

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
