package com.thanu.steady.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.HabitRules
import java.time.LocalDate

@Composable fun ExpandedReview(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit) {
    val period = state.period ?: return
    var mode by remember { mutableStateOf("WEEK") }
    var endText by remember { mutableStateOf(period.end.toString()) }
    var editor by remember { mutableStateOf<ActivitySession?>(null) }
    var details by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { val end = period.end; model.reload(end.minusDays(6), end) }
    fun select(end: LocalDate) { endText = end.toString(); model.reload(if (mode == "MONTH") end.withDayOfMonth(1) else end.minusDays(6), end) }
    ExpandedPage {
        StateMessages(state)
        SectionCard(R.string.review_period) {
            ChoiceList(mode, listOf("WEEK" to R.string.week_period, "MONTH" to R.string.month_period)) {
                mode = it; val end = LocalDate.parse(endText)
                model.reload(if (it == "MONTH") end.withDayOfMonth(1) else end.minusDays(6), end)
            }
            TextInput(endText, R.string.period_end_date, { endText = it })
            PrimaryAction(R.string.show_period) { model.action({ LocalDate.parse(endText) }, success = null,
                after = { select(LocalDate.parse(endText)) }) }
            SecondaryAction(R.string.previous_period) { select(if (mode == "MONTH") period.end.minusMonths(1) else period.end.minusWeeks(1)) }
            SecondaryAction(R.string.next_period) { select(if (mode == "MONTH") period.end.plusMonths(1) else period.end.plusWeeks(1)) }
            SecondaryAction(R.string.current_period) { model.action({ endText = model.repository.logicalDay().toString() }, success = null,
                after = { select(LocalDate.parse(endText)) }) }
            Text(stringResource(R.string.review_range, period.start.toString(), period.end.toString()))
        }
        SectionCard(R.string.review_summary) {
            Text(stringResource(R.string.focus_actual_minutes, focusMillis(period) / 60_000.0))
            val focus = period.sessions.filter { it.type == "FOCUS" && it.state != "DISCARDED" && it.activeMillis > 0 }
            Text(stringResource(R.string.focus_session_count, focus.size))
            period.subjects.forEach { subject ->
                val seconds = activityMillis(period,"FOCUS",subject.id) / 1000
                if (seconds > 0) Text(stringResource(R.string.subject_duration, subject.title, seconds))
            }
            Text(stringResource(R.string.completed_task_count, period.tasks.count { it.state == "COMPLETED" }))
            Text(stringResource(R.string.workout_count, period.sessions.count { it.type == "WORKOUT" && it.state != "DISCARDED" && it.activeMillis > 0 }))
            Text(stringResource(R.string.water_total, period.water.sumOf { it.millilitres.toLong() }))
            if (period.sleep.isEmpty()) Text(stringResource(R.string.sleep_empty)) else
                Text(stringResource(R.string.sleep_entries_count, period.sleep.size))
            PrimaryAction(R.string.supporting_records) { details = !details }
        }
        SectionCard(R.string.habit_history) {
            val dates = generateSequence(period.start) { it.plusDays(1) }.takeWhile { !it.isAfter(period.end) }.toList()
            if (period.habits.isEmpty()) Text(stringResource(R.string.empty_habits))
            period.habits.forEach { habit ->
                dates.forEach { date ->
                    val version = period.versions.filter { it.habitId == habit.id && it.effectiveDay <= date.toString() }.maxByOrNull { it.effectiveDay }
                    if (version != null && (habit.archivedDay == null || date.toString() < habit.archivedDay)) {
                        val due = HabitRules.scheduled(date, LocalDate.parse(version.anchorDay), version.weekdays, version.everyDays)
                        val occurrence = period.occurrences.firstOrNull { it.habitId == habit.id && it.day == date.toString() }
                        val past = state.logicalToday?.let { date < it } ?: false
                        val status = if (!due) "NOT_DUE" else if (occurrence == null) { if(past) "MISSING" else "PENDING" }
                            else if (occurrence.state == "PENDING" && past) "MISSING" else occurrence.state
                        Text(stringResource(R.string.habit_history_row, date.toString(), version.title, stringResource(stateLabel(status))))
                    }
                }
            }
        }
        if (details) SectionCard(R.string.supporting_records) {
            period.tasks.forEach { Text(stringResource(R.string.task_history_row, it.day, it.title, stringResource(stateLabel(it.state)))) }
            period.sessions.filter { it.state !in setOf("RUNNING", "PAUSED", "INTERRUPTED") }.forEach { session ->
                Text(session.title)
                Text(stringResource(R.string.actual_seconds, session.activeMillis / 1000))
                SecondaryAction(R.string.edit_session) { editor = session }
                SecondaryAction(R.string.delete_action, !state.busy) { model.deleteHistory(session.id) }
            }
            period.water.forEach { Text(stringResource(R.string.water_history_row, it.day, it.millilitres)) }
            period.sleep.forEach { Text(stringResource(R.string.sleep_duration, (it.wake - it.bedtime) / 60_000.0)) }
            period.meals.forEach { Text(it.title) }
        }
        val key = "reflection:${period.start}:${period.end}"
        val existing = period.reflection
        LaunchedEffect(key) { model.openDraft(key, mapOf("helped" to existing?.helped.orEmpty(), "demanding" to existing?.demanding.orEmpty(),
            "evidence" to existing?.evidence.orEmpty(), "adjustment" to existing?.adjustment.orEmpty(), "highlight" to existing?.highlight.orEmpty(),
            "obstacle" to existing?.obstacle.orEmpty(), "tomorrow" to existing?.tomorrow.orEmpty())) }
        val drafts by model.drafts.collectAsState()
        val form = drafts[key] ?: emptyMap()
        SectionCard(R.string.reflection_title) {
            listOf("helped" to R.string.reflection_helped, "demanding" to R.string.reflection_demanding,
                "evidence" to R.string.reflection_evidence, "adjustment" to R.string.reflection_adjustment,
                "highlight" to R.string.reflection_highlight, "obstacle" to R.string.reflection_obstacle, "tomorrow" to R.string.reflection_tomorrow).forEach { (field, label) ->
                TextInput(form[field].orEmpty(), label, { model.field(key, field, it) }, 2)
            }
            PrimaryAction(R.string.save_reflection, !state.busy) { model.action({ model.repository.saveReflection(Reflection(existing?.id ?: model.repository.newId(),
                period.start.toString(), period.end.toString(), form["helped"].orEmpty(), form["demanding"].orEmpty(), form["evidence"].orEmpty(), form["adjustment"].orEmpty(),
                form["highlight"].orEmpty(), form["obstacle"].orEmpty(), form["tomorrow"].orEmpty(), updated = model.repository.clock.millis())) }) }
        }
    }
    editor?.let { HistoryEditor(model, state, it, onSafety) { editor = null } }
}
