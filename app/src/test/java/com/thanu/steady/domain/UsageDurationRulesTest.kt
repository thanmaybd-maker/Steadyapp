package com.thanu.steady.domain
import org.junit.Assert.*
import org.junit.Test

class UsageDurationRulesTest {
    @Test fun observedIntervalsClipBoundariesIgnoreDuplicateEndsAndIncludeOngoingEvents() {
        val events = listOf(UsageTransition("synthetic.a", 0, true), UsageTransition("synthetic.a", 12, true),
            UsageTransition("synthetic.a", 15, false), UsageTransition("synthetic.a", 16, false),
            UsageTransition("synthetic.b", 18, true), UsageTransition("synthetic.b", 21, false))
        assertEquals(mapOf("synthetic.a" to 5L, "synthetic.b" to 2L), UsageDurationRules.durations(10, 20, events))
        assertTrue(UsageDurationRules.durations(10, 20, emptyList()).isEmpty())
    }
}
