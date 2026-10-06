package com.thanu.steady.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderExpiryTest {
    @Test fun graceEndsAtQuietHoursOrLogicalDayAndHandlesDst() {
        val day = LocalDate.parse("2026-01-05")
        val utc = ZoneId.of("UTC")
        val at = ReminderRules.instant(day, 1318, utc, 240)
        assertEquals(at + 120_000, ReminderRules.expires(at,day,utc,240,1320,420))
        val beforeBoundary = ReminderRules.instant(day,239,utc,240)
        assertEquals(beforeBoundary + 60_000,ReminderRules.expires(beforeBoundary,day,utc,240,0,0))
        val dstDay = LocalDate.parse("2026-03-08")
        val newYork = ZoneId.of("America/New_York")
        val dstEnd = ReminderRules.instant(dstDay,1439,newYork,0)
        assertEquals(ActivityTotals.dayBounds(dstDay,newYork,0).end,ReminderRules.expires(dstEnd,dstDay,newYork,0,0,0))
        assertEquals(23 * 3_600_000L, ActivityTotals.dayBounds(dstDay,newYork,0).end - ActivityTotals.dayBounds(dstDay,newYork,0).start)
    }
}
