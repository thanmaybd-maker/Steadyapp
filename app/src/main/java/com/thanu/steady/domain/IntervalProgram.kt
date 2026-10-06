package com.thanu.steady.domain

import kotlinx.serialization.Serializable

@Serializable
data class IntervalProgram(val workSeconds: Int, val restSeconds: Int, val rounds: Int,
    val warmupSeconds: Int = 0, val cooldownSeconds: Int = 0) {
    val totalSeconds: Long get() = warmupSeconds.toLong() + rounds.toLong() * workSeconds +
        (rounds - 1).toLong() * restSeconds + cooldownSeconds
    fun validate() {
        require(workSeconds in 1..86_400 && restSeconds in 0..86_400 && rounds in 1..100)
        require(warmupSeconds in 0..86_400 && cooldownSeconds in 0..86_400 && totalSeconds in 1..86_400)
    }
    /** Phases derive from active time, so pauses and recreation cannot advance a round. */
    fun phase(activeMillis: Long): IntervalPhase {
        validate()
        var elapsed = activeMillis.coerceAtLeast(0) / 1000
        if (elapsed >= totalSeconds) return IntervalPhase("COMPLETE", rounds, 0)
        if (elapsed < warmupSeconds) return IntervalPhase("WARMUP", 0, warmupSeconds - elapsed)
        elapsed -= warmupSeconds
        for (round in 1..rounds) {
            if (elapsed < workSeconds) return IntervalPhase("WORK", round, workSeconds - elapsed)
            elapsed -= workSeconds
            if (round < rounds) {
                if (elapsed < restSeconds) return IntervalPhase("REST", round, restSeconds - elapsed)
                elapsed -= restSeconds
            }
        }
        return IntervalPhase("COOLDOWN", rounds, cooldownSeconds - elapsed)
    }
}
data class IntervalPhase(val kind: String, val round: Int, val remainingSeconds: Long)

@Serializable
data class WorkoutRest(val id: String, val sessionId: String, val deadlineElapsed: Long,
    val boot: Long, val generation: Int, val cueFlags: Int, val complete: Boolean = false)
