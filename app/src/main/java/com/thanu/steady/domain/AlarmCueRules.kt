package com.thanu.steady.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

/** Operational bootstrap metadata only. No subject, task title, health, notes or credentials. */
@Serializable data class AlarmCueToken(val kind: String, val id: String, val generation: Int, val boot: Long,
    val expiresWall: Long, val flags: Int = 0, val deadlineElapsed: Long? = null,
    val wallAt: Long? = null, val day: String? = null, val delivered: Boolean = false) {
    val routine get() = kind in setOf("HABIT", "CARE", "BLOCK")
    val key get() = if(routine) "$kind|$id|$day" else "$kind|$id"
    fun validate() {
        UUID.fromString(id)
        require(kind in setOf("ACTIVITY", "REST", "HABIT", "CARE", "BLOCK"))
        require(generation >= 0 && boot >= 0 && expiresWall > 0 && flags in 0..3)
        if(routine) { require(day != null && wallAt != null && wallAt >= 0); LocalDate.parse(day) }
        else require(deadlineElapsed != null && deadlineElapsed >= 0 && day == null && wallAt == null)
    }
}
object AlarmCueRules {
    fun eligible(token: AlarmCueToken, now: ActivityClock, generation: Int, wallAt: Long? = null): Boolean =
        !token.delivered && token.generation == generation && token.boot == now.boot && now.wall < token.expiresWall &&
            if(token.routine) wallAt == token.wallAt && now.wall >= token.wallAt!!
            else token.deadlineElapsed != null && now.elapsed >= token.deadlineElapsed
}
