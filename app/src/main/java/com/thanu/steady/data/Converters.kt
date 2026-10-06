package com.thanu.steady.data

import androidx.room.TypeConverter
import com.thanu.steady.domain.DayMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? = date?.toEpochMilli()

    @TypeConverter
    fun fromLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun dateToLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromZoneId(value: String?): ZoneId? = value?.let { ZoneId.of(it) }

    @TypeConverter
    fun zoneIdToString(zoneId: ZoneId?): String? = zoneId?.id

    @TypeConverter
    fun fromDayMode(value: String?): DayMode? = value?.let { DayMode.valueOf(it) }

    @TypeConverter
    fun dayModeToString(mode: DayMode?): String? = mode?.name
    
    @TypeConverter
    fun fromTimerState(value: String?): com.thanu.steady.domain.TimerState? = value?.let { com.thanu.steady.domain.TimerState.valueOf(it) }

    @TypeConverter
    fun timerStateToString(state: com.thanu.steady.domain.TimerState?): String? = state?.name
}
