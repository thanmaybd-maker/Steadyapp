package com.thanu.steady.domain

import org.junit.Test
import org.junit.Assert.*

class PersonalizationRulesTest {
    @Test fun shortcutBoundsAndDistinctSummariesDoNotInventUnavailableMetrics() {
        PersonalizationRules.validate("WATER,FOCUS,SLEEP","1,250,2000","FLOZ","")
        assertEquals(listOf(1,250,2000),PersonalizationRules.waterQuantities("1,250,2000"))
        listOf("0","2001","250,250","1,2,3,4,5,6","abc").forEach { value ->
            assertTrue(runCatching { PersonalizationRules.waterQuantities(value) }.isFailure)
        }
        listOf("FOCUS,FOCUS,SLEEP","FOCUS,SLEEP","FAKE_READINESS,FOCUS,SLEEP").forEach { value ->
            assertTrue(runCatching { PersonalizationRules.validate(value,"250","ML","FOCUS") }.isFailure)
        }
    }
}
