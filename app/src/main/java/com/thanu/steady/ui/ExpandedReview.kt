package com.thanu.steady.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.HabitRules
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ExpandedReview(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit) {
    val period = state.period ?: return
    val visibleCards = period.profile.reviewCards.split(',').toSet()
    var mode by remember { mutableStateOf("WEEK") }
    var endText by remember { mutableStateOf(period.end.toString()) }
    var editor by remember { mutableStateOf<ActivitySession?>(null) }
    var details by remember { mutableStateOf(false) }
    var calendar by remember { mutableStateOf(false) }
    var taskEditor by remember { mutableStateOf<PlanItem?>(null) }
    var waterEditor by remember { mutableStateOf<WaterLog?>(null) }
    var sleepEditor by remember { mutableStateOf<SleepLog?>(null) }
    var habitEditor by remember { mutableStateOf<HabitOccurrence?>(null) }
    LaunchedEffect(Unit) { val end = period.end; model.reload(end.minusDays(6), end) }
    fun select(end: LocalDate) { endText = end.toString(); model.reload(if (mode == "MONTH") end.withDayOfMonth(1) else end.minusDays(6), end) }
    ExpandedPage {
        StateMessages(state)
        SectionCard(R.string.review_period) {
            androidx.compose.foundation.layout.Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text(stringResource(R.string.review_range, period.start.toString(), period.end.toString()), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF4648D4))
                Text(mode, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.Gray)
            }
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly) {
                androidx.compose.material3.TextButton(onClick = { select(if (mode == "MONTH") period.end.minusMonths(1) else period.end.minusWeeks(1)) }) { Text("< Prev") }
                androidx.compose.material3.TextButton(onClick = {
                    mode = if (mode == "WEEK") "MONTH" else "WEEK"
                    val end = LocalDate.parse(endText)
                    model.reload(if (mode == "MONTH") end.withDayOfMonth(1) else end.minusDays(6), end)
                }) { Text(if (mode == "WEEK") "View Month" else "View Week") }
                androidx.compose.material3.TextButton(onClick = { select(if (mode == "MONTH") period.end.plusMonths(1) else period.end.plusWeeks(1)) }) { Text("Next >") }
            }
            PrimaryAction(R.string.current_period) { model.action({ endText = model.repository.logicalDay().toString() }, success = null,
                after = { select(LocalDate.parse(endText)) }) }
            SecondaryAction(R.string.choose_review_date) { calendar = true }
        }
        SectionCard(R.string.review_summary) {
            if("FOCUS" in visibleCards) {
            Text(stringResource(R.string.focus_actual_minutes, focusMillis(period) / 60_000.0))
            val focus = period.sessions.filter { it.type == "FOCUS" && completedInPeriod(it,period) }
            Text(stringResource(R.string.focus_session_count, focus.size))
            period.subjects.forEach { subject ->
                val seconds = activityMillis(period,"FOCUS",subject.id) / 1000
                if (seconds > 0) Text(stringResource(R.string.subject_duration, subject.title, seconds))
            }
            }
            Text(stringResource(R.string.completed_task_count, period.tasks.count { it.state == "COMPLETED" }))
            if("WORKOUTS" in visibleCards) Text(stringResource(R.string.workout_count, period.sessions.count { it.type == "WORKOUT" && completedInPeriod(it,period) }))
            if("WATER" in visibleCards) Text(waterAmount(period.water.sumOf { it.millilitres.toLong() },period.profile.waterUnit))
            if("SLEEP" in visibleCards) { if (period.sleep.isEmpty()) Text(stringResource(R.string.sleep_empty)) else
                Text(stringResource(R.string.sleep_entries_count, period.sleep.size))
            }
            PrimaryAction(R.string.supporting_records) { details = !details }
        }
        if("HABITS" in visibleCards) SectionCard(R.string.habit_history) {
            val dates = generateSequence(period.start) { it.plusDays(1) }.takeWhile { !it.isAfter(period.end) }.toList()
            if (period.habits.isEmpty()) Text(stringResource(R.string.empty_habits))
            period.habits.forEach { habit ->
                dates.forEach { date ->
                    val version = period.versions.filter { it.habitId == habit.id && it.effectiveDay <= date.toString() }.maxByOrNull { it.effectiveDay }
                    if (version != null && (habit.archivedDay == null || date.toString() < habit.archivedDay)) {
                        val due = HabitRules.scheduled(date, LocalDate.parse(version.anchorDay), version.weekdays, version.everyDays)
                        val occurrence = period.occurrences.firstOrNull { it.habitId == habit.id && it.day == date.toString() }
                        val past = state.logicalToday?.let { date < it } ?: false
                        val dayMode = period.days.firstOrNull { it.day == date.toString() }?.mode
                        val suppressed = dayMode == "PAUSED" || dayMode == "MINIMUM" && !version.essential
                        val status = if (!due || suppressed && (occurrence == null || occurrence.state == "PENDING")) "NOT_DUE" else if (occurrence == null) { if(past) "MISSING" else "PENDING" }
                            else if (occurrence.state == "PENDING" && past) "MISSING" else occurrence.state
                        val label = stringResource(R.string.habit_history_row, date.toString(), version.title, stringResource(stateLabel(status)))
                        androidx.compose.material3.OutlinedButton(onClick = { select(date); details = true },modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth().heightIn(min = 56.dp)) { Text(label) }
                    }
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = details,
            enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(240)),
            exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(160)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(160))
        ) {
            SectionCard(R.string.supporting_records) {
                period.occurrences.forEach { occurrence ->
                period.versions.firstOrNull { it.id == occurrence.versionId }?.let { version ->
                    Text(stringResource(R.string.habit_history_row,occurrence.day,version.title,stringResource(stateLabel(occurrence.state))))
                    Text(stringResource(R.string.habit_quantity,occurrence.quantity,version.target,version.unit))
                    SecondaryAction(R.string.edit_action) { habitEditor = occurrence }
                    SecondaryAction(if(occurrence.state == "SKIPPED") R.string.unskip_habit else R.string.skip_habit,!state.busy) {
                        model.action({ model.repository.skipHabit(occurrence.id) })
                    }
                }
            }
            period.tasks.forEach { task -> Text(stringResource(R.string.task_history_row, task.day, task.title, stringResource(stateLabel(task.state))))
                SecondaryAction(R.string.edit_reschedule) { taskEditor = task }
                SecondaryAction(if(task.state == "COMPLETED") R.string.undo_complete else R.string.complete_task,!state.busy) { model.action({ model.repository.toggleTask(task.id) }) }
            }
            period.sessions.filter { it.state !in setOf("RUNNING", "PAUSED", "INTERRUPTED") }.forEach { session ->
                Text(session.title)
                Text(stringResource(R.string.actual_seconds, session.activeMillis / 1000))
                SecondaryAction(R.string.edit_session) { editor = session }
                SecondaryAction(R.string.delete_action, !state.busy) { model.deleteHistory(session.id) }
            }
            period.water.forEach { water -> Text(stringResource(R.string.water_history_row, water.day, water.millilitres))
                SecondaryAction(R.string.edit_action) { waterEditor = water }
                SecondaryAction(R.string.delete_action,!state.busy) { model.action({ model.repository.deleteWater(water.id) }) }
            }
            period.sleep.forEach { sleep -> Text(stringResource(R.string.sleep_duration, (sleep.wake - sleep.bedtime) / 60_000.0))
                SecondaryAction(R.string.edit_action) { sleepEditor = sleep }
                SecondaryAction(R.string.delete_action,!state.busy) { model.action({ model.repository.deleteSleep(sleep.id) }) }
            }
            period.meals.forEach { Text(it.title) }
        }
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
    if(calendar) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = period.end.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(onDismissRequest = { calendar = false },confirmButton = {
            SecondaryAction(R.string.show_period,picker.selectedDateMillis != null) {
                picker.selectedDateMillis?.let { select(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate()) }; calendar = false
            }
        },dismissButton = { SecondaryAction(R.string.cancel) { calendar = false } }) { DatePicker(picker) }
    }
    taskEditor?.let { TaskEditor(model,state,it,onSafety) { taskEditor = null } }
    waterEditor?.let { WaterEditor(model,state,it,onSafety) { waterEditor = null } }
    sleepEditor?.let { SleepEditor(model,state,it,onSafety) { sleepEditor = null } }
    habitEditor?.let { occurrence ->
        val key = "habit-history:${occurrence.id}"
        DraftEditor(model,state,key,R.string.log_habit,mapOf("quantity" to occurrence.quantity.toString(),"note" to occurrence.notes),onSafety,{ habitEditor = null }) { values,close ->
            TextInput(values["quantity"].orEmpty(),R.string.quantity_value,{ model.field(key,"quantity",it) })
            TextInput(values["note"].orEmpty(),R.string.habit_note,{ model.field(key,"note",it) },3)
            PrimaryAction(R.string.save_action,!state.busy) { model.action({ model.repository.correctHabit(occurrence.id,values.getValue("quantity").toDouble(),values["note"].orEmpty()) },after = close) }
        }
    }
    editor?.let { HistoryEditor(model, state, it, onSafety) { editor = null } }
}
