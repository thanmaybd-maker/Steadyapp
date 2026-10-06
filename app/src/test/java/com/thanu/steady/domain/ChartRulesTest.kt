package com.thanu.steady.domain

import org.junit.Assert.*
import org.junit.Test

class ChartRulesTest {
    @Test fun unknownGoalsAndInvalidObservationsNeverLookCompleted() {
        assertNull(ChartRules.progress(null, 10.0))
        assertNull(ChartRules.progress(10.0, null))
        assertNull(ChartRules.progress(10.0, 0.0))
        assertNull(ChartRules.progress(Double.NaN, 10.0))
        assertNull(ChartRules.progress(10.0, Double.POSITIVE_INFINITY))
        assertNull(ChartRules.progress(-1.0, 10.0))
        assertEquals(0f, ChartRules.progress(0.0, 10.0)!!, 0f)
        assertEquals(0.5f, ChartRules.progress(5.0, 10.0)!!, 0f)
        assertEquals(1f, ChartRules.progress(15.0, 10.0)!!, 0f)
    }

    @Test fun historicalDotsRespectSuppressedDaysAndUndo() {
        assertEquals("NOT_DUE", ChartRules.habitStatus(false, false, null, true))
        assertEquals("NOT_DUE", ChartRules.habitStatus(true, true, "PENDING", true))
        assertEquals("COMPLETED", ChartRules.habitStatus(true, true, "COMPLETED", true))
        assertEquals("PARTIAL", ChartRules.habitStatus(true, false, "PARTIAL", true))
        assertEquals("SKIPPED", ChartRules.habitStatus(true, false, "SKIPPED", true))
        assertEquals("MISSING", ChartRules.habitStatus(true, false, "PENDING", true))
        assertEquals("PENDING", ChartRules.habitStatus(true, false, null, false))
    }
}
