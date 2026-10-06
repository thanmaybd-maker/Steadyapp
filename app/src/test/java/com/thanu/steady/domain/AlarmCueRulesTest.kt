package com.thanu.steady.domain
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AlarmCueRulesTest {
    private val token = AlarmCueToken("ACTIVITY", UUID.randomUUID().toString(), 2, 3, 20_000, 0, deadlineElapsed = 5000)
    @Test fun generationBootDeadlineExpiryAndDeliveredStateGuardEveryCue() {
        assertTrue(AlarmCueRules.eligible(token, ActivityClock(10_000, 5000, 3), 2))
        assertFalse(AlarmCueRules.eligible(token, ActivityClock(10_000, 4999, 3), 2))
        assertFalse(AlarmCueRules.eligible(token, ActivityClock(10_000, 5000, 4), 2))
        assertFalse(AlarmCueRules.eligible(token, ActivityClock(10_000, 5000, 3), 1))
        assertFalse(AlarmCueRules.eligible(token, ActivityClock(20_000, 5000, 3), 2))
        assertFalse(AlarmCueRules.eligible(token.copy(delivered = true), ActivityClock(10_000, 5000, 3), 2))
    }
    @Test fun routineDatesAndScheduledTimeRemainPartOfIdentity() {
        val first = token.copy(kind = "BLOCK", generation = 0, deadlineElapsed = null, wallAt = 10_000, day = "2026-01-05")
        first.validate()
        assertNotEquals(first.key, first.copy(day = "2026-01-06").key)
        assertTrue(AlarmCueRules.eligible(first, ActivityClock(10_000, 0, 3), 0, 10_000))
        assertFalse(AlarmCueRules.eligible(first, ActivityClock(10_000, 0, 3), 0, 9999))
    }
}
