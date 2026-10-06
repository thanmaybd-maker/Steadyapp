package com.thanu.steady.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.*
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HabitHistoryDay(val date: LocalDate, val status: String, val fraction: Float)
fun habitHistoryDays(period: PeriodSnapshot, habit: HabitDefinition, today: LocalDate): List<HabitHistoryDay> =
    generateSequence(period.start) { it.plusDays(1) }.takeWhile { it <= period.end }.map { date ->
        val occurrence = period.occurrences.firstOrNull { it.habitId == habit.id && it.day == date.toString() }
        val version = period.versions.firstOrNull { it.id == occurrence?.versionId } ?: period.versions
            .filter { it.habitId == habit.id && it.effectiveDay <= date.toString() }.maxByOrNull { it.effectiveDay }
        val scheduled = version != null && (habit.archivedDay == null || date.toString() < habit.archivedDay) &&
            HabitRules.scheduled(date, LocalDate.parse(version.anchorDay), version.weekdays, version.everyDays)
        val mode = period.days.firstOrNull { it.day == date.toString() }?.mode
        val suppressed = mode == "PAUSED" || mode == "MINIMUM" && version?.essential != true
        val status = ChartRules.habitStatus(scheduled, suppressed, occurrence?.state, date < today)
        HabitHistoryDay(date, status, if (status in setOf("COMPLETED", "PARTIAL")) ChartRules.progress(occurrence?.quantity, version?.target) ?: 0f else 0f)
    }.toList()

@Composable fun HabitWeekHistory(model: ExpandedViewModel, state: ExpandedUiState) {
    val current = state.period ?: return
    var history by remember { mutableStateOf<PeriodSnapshot?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var consistency by remember { mutableStateOf(false) }
    LaunchedEffect(current, retry) {
        failed = false
        try { history = withContext(Dispatchers.IO) { model.repository.snapshot(current.end.minusDays(6), current.end) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    SectionCard(R.string.habit_week_preview) {
        if (failed) {
            Text(stringResource(R.string.habit_history_load_failed))
            SecondaryAction(R.string.retry) { retry++ }
        } else if (history == null) Text(stringResource(R.string.habit_history_loading))
        history?.let { period ->
            ToggleRow(R.string.habit_consistency_optional, consistency) { consistency = it }
            if (consistency) Text(stringResource(R.string.habit_consistency_policy))
            period.habits.forEach { habit ->
                val version = period.versions.filter { it.habitId == habit.id }.maxByOrNull { it.effectiveDay } ?: return@forEach
                val days = habitHistoryDays(period, habit, state.logicalToday ?: current.end)
                if (days.all { it.status == "NOT_DUE" }) return@forEach
                Text(version.title, style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    days.forEach { day ->
                        val fraction by animateFloatAsState(day.fraction, tween(if (current.profile.reducedMotion) 0 else 240), label = "habit-history")
                        val track = MaterialTheme.colorScheme.outline
                        val color = if (day.status == "COMPLETED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        Column(Modifier.widthIn(min = 88.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Text(day.date.format(DateTimeFormatter.ofPattern("EEE dd")))
                            Canvas(Modifier.size(32.dp)) {
                                drawCircle(track, size.minDimension / 2 - 2.dp.toPx(), style = Stroke(2.dp.toPx()))
                                if (fraction > 0) drawArc(color, -90f, fraction * 360, true, Offset(3.dp.toPx(), 3.dp.toPx()),
                                    Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()))
                            }
                            Text(stringResource(stateLabel(day.status)))
                        }
                    }
                }
                // Full dates and states are a nonvisual alternative to the decorative circles.
                days.forEach { Text(stringResource(R.string.habit_day_dot, it.date.toString(), stringResource(stateLabel(it.status)))) }
                if (consistency) {
                    val eligible = days.map { if (it.date >= (state.logicalToday ?: current.end) && it.status == "PARTIAL") "PENDING" else it.status }
                    val (run, best) = ReviewChartRules.consistency(eligible)
                    Text(stringResource(R.string.habit_consistency_summary, run, best))
                }
            }
        }
    }
}
