package com.thanu.steady.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LogicalDayPolicyTest {
    @Test fun boundaryUsesLocalTimeAcrossSpringDstChange() {
        val zone = ZoneId.of("America/New_York")
        val atBoundary = LocalDateTime.of(2026, 3, 8, 4, 0).atZone(zone)
        assertEquals(java.time.LocalDate.of(2026, 3, 8), LogicalDayPolicy().getLogicalDay(atBoundary.toInstant(), zone))
    }
    @Test
    fun testLogicalDayBeforeBoundary() {
        val policy = LogicalDayPolicy()
        // Boundary is 240 mins (4 AM). A time at 3:00 AM on Jan 2 should count as Jan 1.
        val time = LocalDateTime.of(2026, 1, 2, 3, 0)
        val zoneId = ZoneId.of("UTC")
        val zdt = time.atZone(zoneId)
        
        val logicalDay = policy.getLogicalDay(zdt.toInstant(), zoneId)
        
        assertEquals(java.time.LocalDate.of(2026, 1, 1), logicalDay)
    }

    @Test
    fun testLogicalDayAfterBoundary() {
        val policy = LogicalDayPolicy()
        // A time at 5:00 AM on Jan 2 should count as Jan 2.
        val time = LocalDateTime.of(2026, 1, 2, 5, 0)
        val zoneId = ZoneId.of("UTC")
        val zdt = time.atZone(zoneId)
        
        val logicalDay = policy.getLogicalDay(zdt.toInstant(), zoneId)
        
        assertEquals(java.time.LocalDate.of(2026, 1, 2), logicalDay)
    }
}
