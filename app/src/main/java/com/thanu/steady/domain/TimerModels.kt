package com.thanu.steady.domain

import java.time.Instant
import java.util.UUID

enum class TimerState {
    IDLE, RUNNING, PAUSED, COMPLETED, CANCELLED, INTERRUPTED
}

data class TimerSession(
    val id: String = UUID.randomUUID().toString(),
    val logicalDay: String? = null,
    val kind: String = "break",
    val durationMs: Long,
    val remainingMs: Long,
    val state: TimerState = TimerState.IDLE,
    val startedAt: Instant? = null,
    val targetWallTime: Instant? = null,
    val targetElapsedTime: Long? = null,
    val bootMarker: Long = 0L,
    val cueFlags: Int = 0,
    val completedAt: Instant? = null,
    val generation: Int = 0
)
