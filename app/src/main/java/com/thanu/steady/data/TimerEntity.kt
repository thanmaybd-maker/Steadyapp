package com.thanu.steady.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.thanu.steady.domain.TimerState
import java.time.Instant

@Entity(tableName = "timer_session")
data class TimerSessionEntity(
    @PrimaryKey val id: String,
    val logicalDay: String?,
    val kind: String,
    val durationMs: Long,
    val remainingMs: Long,
    val state: TimerState,
    val startedAt: Instant?,
    val targetWallTime: Instant?,
    val targetElapsedTime: Long?,
    val bootMarker: Long,
    val cueFlags: Int,
    val completedAt: Instant?,
    val generation: Int
)
