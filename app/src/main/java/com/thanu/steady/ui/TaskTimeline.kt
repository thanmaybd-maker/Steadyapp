package com.thanu.steady.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thanu.steady.R
import com.thanu.steady.data.PeriodSnapshot
import com.thanu.steady.domain.ActivityTotals
import java.time.*
import java.time.format.DateTimeFormatter

private data class TimelineMark(val title: String,val start: Long,val end: Long,val recorded: Boolean,val lane: Int,val sessionType: String? = null)

/** A graph of actual records/plans only; the labelled list remains independently operable. */
@Composable fun TaskTimeline(period: PeriodSnapshot,onFocus: () -> Unit,onHealth: () -> Unit, clock: Clock, includeActions: Boolean = true) {
    val day = period.days.firstOrNull { it.day == period.end.toString() }
    val zone = ZoneId.of(day?.zone ?: period.preferences.zoneId)
    val boundary = day?.boundary ?: period.preferences.boundaryMinutes
    val range = ActivityTotals.dayBounds(period.end,zone,boundary)
    val now by produceState(clock.millis(), clock) {
        while(true) { value = clock.millis(); kotlinx.coroutines.delay(60_000) }
    }
    val marker = com.thanu.steady.domain.ReviewChartRules.marker(now, range)
    val markerAlpha = if(period.profile.reducedMotion || marker == null) 1f else {
        val transition = rememberInfiniteTransition(label = "horizon-now")
        val value by transition.animateFloat(0.65f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "horizon-beacon")
        value
    }
    val sessions = period.sessions.associateBy { it.id }
    val source = period.tasks.filter { it.state != "ARCHIVED" && it.timeMinutes != null }.map { task ->
        val time = LocalTime.of(task.timeMinutes!!/60,task.timeMinutes%60)
        val date = LocalDate.parse(task.day).let { if(task.timeMinutes < task.boundary) it.plusDays(1) else it }
        val start = date.atTime(time).atZone(ZoneId.of(task.zone)).toInstant().toEpochMilli()
        TimelineMark(task.title,start,start+(task.plannedSeconds ?: 0)*1000,false,0)
    } + period.segments.mapNotNull { span ->
        val session = sessions[span.sessionId] ?: return@mapNotNull null
        val end = span.endWall ?: return@mapNotNull null
        if(session.state == "DISCARDED" || span.activeMillis <= 0) return@mapNotNull null
        TimelineMark(session.title,span.startWall,end,true,0,session.type)
    }
    val laneEnds = mutableListOf<Long>()
    val marks = source.filter { it.start < range.end && it.end >= range.start }.sortedBy { it.start }.map { mark ->
        val start = maxOf(mark.start,range.start); val end = minOf(mark.end,range.end)
        val lane = laneEnds.indexOfFirst { it <= start }.takeIf { it >= 0 } ?: laneEnds.size.also { laneEnds += Long.MIN_VALUE }
        laneEnds[lane] = maxOf(start+1,end)
        mark.copy(start = start,end = end,lane = lane)
    }
    val planned = MaterialTheme.colorScheme.secondary
    val recorded = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outline
    val description = stringResource(R.string.timeline_graph_description)
    Text(stringResource(R.string.timeline_graph_legend))
    Canvas(Modifier.fillMaxWidth().height((48+24*laneEnds.size).dp).semantics { contentDescription = description }) {
        val duration = (range.end-range.start).toDouble()
        fun x(at: Long) = ((at-range.start)/duration*size.width).toFloat()
        drawLine(axis,Offset(0f,8.dp.toPx()),Offset(size.width,8.dp.toPx()),2.dp.toPx())
        marker?.let { fraction ->
            val x = fraction * size.width
            drawLine(recorded.copy(alpha = markerAlpha), Offset(x,0f), Offset(x,size.height), 2.dp.toPx())
            drawCircle(recorded, 5.dp.toPx(), Offset(x,8.dp.toPx()))
        }
        marks.forEach { mark ->
            val y = (28+24*mark.lane).dp.toPx()
            if(mark.end == mark.start) drawCircle(if(mark.recorded) recorded else planned,4.dp.toPx(),Offset(x(mark.start),y))
            else drawLine(if(mark.recorded) recorded else planned,Offset(x(mark.start),y),Offset(x(mark.end),y),8.dp.toPx(),StrokeCap.Round)
        }
    }
    val format = DateTimeFormatter.ofPattern("HH:mm")
    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween) {
        Text(Instant.ofEpochMilli(range.start).atZone(zone).format(format))
        Text(Instant.ofEpochMilli(range.end).atZone(zone).format(format))
    }
    Text(stringResource(R.string.timeline_axis_zone,zone.id))
    marker?.let { Text(stringResource(R.string.timeline_now, Instant.ofEpochMilli(now).atZone(zone).format(format), zone.id)) }
    Text(stringResource(R.string.timeline_linear_alternative))
    marks.forEach { mark ->
        val overlaps = marks.count { it !== mark && it.start < maxOf(mark.end,mark.start+1) && maxOf(it.end,it.start+1) > mark.start }
        Text(stringResource(R.string.timeline_record_row,mark.title,
            Instant.ofEpochMilli(mark.start).atZone(zone).format(format),Instant.ofEpochMilli(mark.end).atZone(zone).format(format),
            stringResource(if(mark.recorded) R.string.recorded_segment else R.string.planned_segment)))
        if(overlaps > 0) Text(stringResource(R.string.timeline_overlap,overlaps+1))
        if(mark.recorded && includeActions) {
            SecondaryAction(if(mark.sessionType == "WORKOUT") R.string.open_health else R.string.open_focus,
                onClick = if(mark.sessionType == "WORKOUT") onHealth else onFocus)
        }
    }
}
