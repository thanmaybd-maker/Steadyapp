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
        val boundaryTime = zdt.toLocalDate().atStartOfDay(zoneId).plusMinutes(boundaryMinutes.toLong())
        
        return if (zdt.isBefore(boundaryTime)) {
            zdt.toLocalDate().minusDays(1)
        } else {
            zdt.toLocalDate()
        }
    }
}
