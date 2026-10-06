package com.thanu.steady.domain

import org.junit.Test
import org.junit.Assert.*
import java.time.*

class ProgrammeRulesTest {
    @Test fun intervalsUseActualTimeWithNoTrailingRest() {
        val p = IntervalProgram(30,10,3,5,7)
        assertEquals(122L,p.totalSeconds)
        assertEquals(IntervalPhase("WARMUP",0,5),p.phase(0))
        assertEquals(IntervalPhase("WORK",1,30),p.phase(5000))
        assertEquals(IntervalPhase("REST",1,10),p.phase(35000))
        assertEquals(IntervalPhase("WORK",2,30),p.phase(45000))
        assertEquals(IntervalPhase("WORK",3,30),p.phase(85000))
        assertEquals(IntervalPhase("COOLDOWN",3,7),p.phase(115000))
        assertEquals(IntervalPhase("COMPLETE",3,0),p.phase(122000))
    }
    @Test fun singleRoundAndZeroRestHaveNoPhantomPhase() {
        assertEquals(10L,IntervalProgram(10,60,1).totalSeconds)
        assertEquals(IntervalPhase("WORK",2,10),IntervalProgram(10,0,2).phase(10000))
    }
    @Test(expected = IllegalArgumentException::class) fun oversizedProgrammeCannotOverflowOrStart() {
        IntervalProgram(86400,86400,100).validate()
    }
    @Test fun pauseDoesNotAdvanceIntervalPhase() {
        val engine = ActivityEngine(); val p = IntervalProgram(30,10,2)
        val start = ActivityClock(1000,1000,1)
        val running = engine.resume(ActivityTiming(targetMillis = p.totalSeconds*1000),start)
        val paused = engine.close(running,ActivityClock(16000,16000,1),ActivityState.PAUSED)
        assertEquals(IntervalPhase("WORK",1,15),p.phase(engine.active(paused,ActivityClock(90000,90000,1))))
    }
    @Test fun quietHoursWrapAndEqualMeansDisabled() {
        assertTrue(ReminderRules.quiet(0,1320,420)); assertTrue(ReminderRules.quiet(1320,1320,420))
        assertFalse(ReminderRules.quiet(420,1320,420)); assertFalse(ReminderRules.quiet(700,700,700))
        assertTrue(ReminderRules.quiet(700,600,800)); assertFalse(ReminderRules.quiet(800,600,800))
    }
    @Test fun remindersRespectLogicalDayAndDaylightSaving() {
        val zone = ZoneId.of("America/New_York"); val day = LocalDate.of(2026,3,7)
        assertEquals(Instant.parse("2026-03-08T07:30:00Z").toEpochMilli(),ReminderRules.instant(day,150,zone,240))
        assertEquals(Instant.parse("2026-03-07T14:00:00Z").toEpochMilli(),ReminderRules.instant(day,540,zone,240))
    }
    @Test fun pausedQuietBudgetAndOldAlarmsCannotDeliver() {
        assertTrue(ReminderRules.eligible(1000,1000,"NORMAL",false,0,5))
        assertFalse(ReminderRules.eligible(1000,1000,"PAUSED",false,0,5))
        assertFalse(ReminderRules.eligible(1000,1000,"NORMAL",true,0,5))
        assertFalse(ReminderRules.eligible(1000,1000,"NORMAL",false,5,5))
        assertFalse(ReminderRules.eligible(301001,1000,"NORMAL",false,0,5))
        assertFalse(ReminderRules.eligible(999,1000,"NORMAL",false,0,5))
    }
}
