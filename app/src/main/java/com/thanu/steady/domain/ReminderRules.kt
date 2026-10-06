package com.thanu.steady.domain

import java.time.*

object ReminderRules {
    fun quiet(minute: Int, start: Int, end: Int): Boolean {
        require(minute in 0..1439 && start in 0..1439 && end in 0..1439)
        return if(start == end) false else if(start < end) minute in start until end else minute >= start || minute < end
    }
    fun instant(day: LocalDate, minute: Int, zone: ZoneId, boundary: Int): Long {
        require(minute in 0..1439 && boundary in 0..1439)
        return (if(minute < boundary) day.plusDays(1) else day).atTime(minute/60,minute%60).atZone(zone).toInstant().toEpochMilli()
    }
    fun eligible(now: Long, scheduled: Long, mode: String, quiet: Boolean, used: Int, budget: Int): Boolean =
        mode != "PAUSED" && !quiet && budget in 1..5 && used < budget && now in scheduled..scheduled+300_000
}
