package com.thanu.steady.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class LogicalDayPolicy {
    fun getLogicalDay(
        now: Instant,
        zoneId: ZoneId = ZoneId.of("Asia/Kolkata"),
        boundaryMinutes: Int = 240 // 4 hours * 60 = 04:00 AM
    ): LocalDate {
        val zdt = ZonedDateTime.ofInstant(now, zoneId)
        require(boundaryMinutes in 0..1439)
        val boundaryTime = java.time.LocalTime.of(boundaryMinutes / 60, boundaryMinutes % 60)
        
        return if (zdt.toLocalTime().isBefore(boundaryTime)) {
            zdt.toLocalDate().minusDays(1)
        } else {
            zdt.toLocalDate()
        }
    }
}
