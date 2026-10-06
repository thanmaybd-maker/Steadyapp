package com.thanu.steady.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thanu.steady.R
import com.thanu.steady.data.PeriodSnapshot
import com.thanu.steady.domain.*
import java.time.LocalDate

fun focusRestRows(period: PeriodSnapshot): List<DailyFocusRest> {
    val sessions = period.sessions.filter { it.state != "DISCARDED" }.associateBy { it.id }
    return ReviewChartRules.daily(period.start, period.end, period.segments.mapNotNull { segment ->
        val session = sessions[segment.sessionId] ?: return@mapNotNull null
        val end = segment.endWall ?: return@mapNotNull null
        if (segment.activeMillis <= 0) null else ReviewInterval(session.type, segment.startWall, end, segment.zone, segment.boundary)
    })
}

/** Native offline counterpart of OG's paired bar chart, with labelled day actions. */
@Composable fun FocusRestChart(period: PeriodSnapshot, onRecords: (LocalDate) -> Unit) {
    val rows = remember(period) { focusRestRows(period) }
    var selected by remember(period.start, period.end) { mutableStateOf<LocalDate?>(null) }
    val focus = MaterialTheme.colorScheme.primary
    val rest = MaterialTheme.colorScheme.tertiary
    val axis = MaterialTheme.colorScheme.outline
    val highlight = MaterialTheme.colorScheme.secondaryContainer
    val description = stringResource(R.string.focus_rest_graph_description)
    val focusTotal = rows.sumOf { it.focusMillis }; val restTotal = rows.sumOf { it.restMillis }
    val ratio = if (focusTotal + restTotal == 0L) null else focusTotal.toDouble() / (focusTotal + restTotal)
    SectionCard(R.string.focus_rest_chart_title) {
        Text(stringResource(R.string.focus_rest_totals, focusTotal / 60_000.0, restTotal / 60_000.0))
        Text(if (ratio == null) stringResource(R.string.focus_ratio_absent) else stringResource(R.string.focus_ratio_value, ratio * 100))
        Text(stringResource(R.string.focus_rest_graph_legend))
        val maximum = maxOf(60_000L, rows.maxOfOrNull { maxOf(it.focusMillis, it.restMillis) } ?: 0)
        Text(stringResource(R.string.focus_rest_axis, maximum / 60_000.0))
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Canvas(Modifier.width(maxOf(280, rows.size * 44).dp).height(180.dp).semantics { contentDescription = description }
                .pointerInput(rows) { detectTapGestures { point -> selected = rows[(point.x / size.width * rows.size).toInt().coerceIn(rows.indices)].day } }) {
                val width = size.width / rows.size
                val plot = size.height - 8.dp.toPx()
                rows.forEachIndexed { index, row ->
                    if (row.day == selected) drawRect(highlight, Offset(index * width, 0f), Size(width, plot))
                    fun bar(value: Long, left: Float, color: androidx.compose.ui.graphics.Color) {
                        val height = (value.toDouble() / maximum * plot).toFloat()
                        if (height > 0) drawRect(color, Offset(left, plot - height), Size(width * 0.3f, height))
                    }
                    bar(row.focusMillis, index * width + width * 0.12f, focus)
                    bar(row.restMillis, index * width + width * 0.53f, rest)
                }
                drawLine(axis, Offset(0f, plot), Offset(size.width, plot), 1.dp.toPx())
            }
        }
        rows.forEach { row ->
            val label = stringResource(R.string.focus_rest_day, row.day.toString(), row.focusMillis / 60_000.0, row.restMillis / 60_000.0)
            OutlinedButton(onClick = { selected = row.day }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(label) }
        }
        selected?.let { day ->
            Text(stringResource(R.string.selected_record_day, day.toString()))
            PrimaryAction(R.string.supporting_records) { onRecords(day) }
        }
    }
}
