package com.thanu.steady.domain

import org.junit.Test
import org.junit.Assert.*

class ReflectionDraftRulesTest {
    @Test fun optionalRatingsPreserveMissingValuesAndOnlyAcceptExplicitOneThroughFive() {
        assertNull(ReflectionDraftRules.rating(" "))
        assertEquals(3,ReflectionDraftRules.rating(" 3 "))
        listOf("0","6","-1","2.5","50","good").forEach { assertTrue(runCatching { ReflectionDraftRules.rating(it) }.isFailure) }
    }
}
