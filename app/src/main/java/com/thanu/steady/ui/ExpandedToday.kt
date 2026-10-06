package com.thanu.steady.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun focusMillis(period: PeriodSnapshot): Long = activityMillis(period,"FOCUS")
fun completedInPeriod(session: ActivitySession, period: PeriodSnapshot): Boolean {
    if(session.state !in setOf("STOPPED","COMPLETED") || session.activeMillis <= 0 || session.ended == null) return false
    val day = LogicalDayPolicy().getLogicalDay(Instant.ofEpochMilli(session.ended),ZoneId.of(session.zone),session.boundary)
    return day in period.start..period.end
}
fun activityMillis(period: PeriodSnapshot,type: String,subject: String? = null): Long {
    val ids = period.sessions.filter { it.type == type && it.state != "DISCARDED" && (subject == null || it.subjectId == subject) }.map { it.id }.toSet()
    val pieces = period.segments.filter { it.sessionId in ids && it.activeMillis > 0 }.mapNotNull { segment ->
        val end = segment.endWall ?: return@mapNotNull null
        val zone = ZoneId.of(segment.zone)
        val range = ActivityTotals.dayBounds(period.start, zone, segment.boundary).start to ActivityTotals.dayBounds(period.end, zone, segment.boundary).end
        val begin = maxOf(segment.startWall, range.first); val stop = minOf(end, range.second)
        if (stop > begin) WallInterval(begin, stop) else null
    }
    return ActivityTotals.unionMillis(pieces)
}

@Composable fun ExpandedToday(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit,
    onFocus: () -> Unit, onHealth: () -> Unit, quickAction: String? = null, onQuickActionHandled: () -> Unit = {}) {
    val period = state.period ?: return
    val day = period.end
    val daySettings = period.days.firstOrNull { it.day == day.toString() }
    val mode = daySettings?.mode ?: "NORMAL"
    var editor by remember { mutableStateOf<String?>(null) }
    var editedTask by remember { mutableStateOf<PlanItem?>(null) }
    var editedHabit by remember { mutableStateOf<HabitVersion?>(null) }
    var undoneTask by remember { mutableStateOf<PlanItem?>(null) }
    var lastHabitLog by remember { mutableStateOf<String?>(null) }
    var optionalShown by remember { mutableStateOf(false) }
    LaunchedEffect(quickAction) { if(quickAction != null) { editedTask = null; editedHabit = null; editor = quickAction; onQuickActionHandled() } }
    val visibleTasks = period.tasks.filter { it.state != "ARCHIVED" && (mode == "NORMAL" || optionalShown || it.essential) }
    val versions = period.versions.associateBy { it.id }
    val occurrences = period.occurrences.filter { mode == "NORMAL" || optionalShown || versions[it.versionId]?.essential == true }
    ExpandedPage {
        Text(if (period.profile.displayName.isBlank()) stringResource(R.string.today_heading) else
            stringResource(R.string.greeting_name, period.profile.displayName), style = MaterialTheme.typography.headlineLarge)
        Text(if(period.profile.dateStyle == "ISO") day.toString() else day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)))
        StateMessages(state)
        ChoiceList(mode, listOf("NORMAL" to R.string.normal_day, "MINIMUM" to R.string.minimum_day, "PAUSED" to R.string.pause_day), model::mode)
        if (mode == "MINIMUM") Text(stringResource(R.string.minimum_description))
        if (mode == "PAUSED") Text(stringResource(R.string.pause_description))
        if (mode != "NORMAL") SecondaryAction(R.string.show_optional) { optionalShown = !optionalShown }
        period.profile.dashboard.split(',').forEach { card -> CompositionLocalProvider(LocalRoomyCard provides (card in period.profile.wideCards.split(','))) { when (card) {
            "ROUTINES" -> if(mode != "PAUSED" && "HABITS" in period.profile.modules.split(',')) SectionCard(R.string.routine_anchors) {
                Text(stringResource(R.string.routine_examples))
                listOf(Triple(R.string.add_study_anchor,R.string.study_anchor_title,10.0),
                    Triple(R.string.add_build_anchor,R.string.build_anchor_title,10.0),
                    Triple(R.string.add_movement_anchor,R.string.movement_anchor_title,5.0)).forEach { (label,title,target) ->
                    SecondaryAction(label,!state.busy) { editor = "anchor:$title:${target.toInt()}" }
                }
            }
            "NEXT" -> SectionCard(R.string.next_action_title) {
                Text(daySettings?.nextAction?.ifBlank { stringResource(R.string.next_empty) } ?: stringResource(R.string.next_empty))
                PrimaryAction(R.string.edit_next_action) { editor = "next" }
                SecondaryAction(R.string.pocket_reset) { editor = "reset" }
            }
            "RINGS" -> if (mode != "PAUSED") SectionCard(R.string.today_summaries) {
                DashboardMetrics(period,onFocus,onHealth) { editor = "habit" }
            }
            "TIMELINE" -> if ("PLAN" in period.profile.modules.split(',')) SectionCard(R.string.timeline_title) {
                PrimaryAction(R.string.add_task) { editedTask = null; editor = "task" }
                StudyBlockPlanner(model, state, onSafety)
                TaskTimeline(period.copy(tasks = visibleTasks),onFocus,onHealth,model.repository.clock)
                if (visibleTasks.isEmpty()) Text(stringResource(R.string.empty_tasks))
                visibleTasks.sortedWith(compareBy<PlanItem> { item -> item.timeMinutes?.let { time -> Math.floorMod(time-item.boundary,1440) } ?: 1440 }.thenBy { it.position }).forEach { task ->
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(240)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(240)),
                        exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(160)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(160))
                    ) {
                        Column {
                            Text(task.title, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(stateLabel(task.state)))
                            task.timeMinutes?.let { Text(stringResource(R.string.planned_time, "%02d:%02d".format(java.util.Locale.ROOT, it / 60, it % 60))) }
                            task.plannedSeconds?.let { Text(stringResource(R.string.planned_minutes, it / 60.0)) }
                            PrimaryAction(if (task.state == "COMPLETED") R.string.undo_complete else R.string.complete_task, !state.busy) { model.action({ model.repository.toggleTask(task.id) }) }
                            SecondaryAction(R.string.edit_reschedule) { editedTask = task; editor = "task" }
                            SecondaryAction(R.string.start_task_focus, !state.busy) {
                                if (period.active.isNotEmpty()) onFocus() else model.start("FOCUS", if (task.category == "BUILD") "BUILD" else "STUDY", task.title,
                                    task.plannedSeconds ?: 1500, 300, task.subjectId, task.id, onFocus)
                            }
                            SecondaryAction(R.string.delete_action, !state.busy) { model.action({ model.repository.deleteTask(task.id) }, after = { undoneTask = task }) }
                            HorizontalDivider()
                        }
                    }
                }
                undoneTask?.let { task -> SecondaryAction(R.string.undo_delete, !state.busy) { model.action({ model.repository.saveTask(task) }, after = { undoneTask = null }) } }
            }
            "HABITS" -> if ("HABITS" in period.profile.modules.split(',') && (mode != "PAUSED" || optionalShown)) SectionCard(R.string.habits_title) {
                PrimaryAction(R.string.add_habit) { editedHabit = null; editor = "habit" }
                HabitWeekHistory(model, state)
                if (occurrences.isEmpty()) Text(stringResource(R.string.empty_habits))
                occurrences.forEach { occurrence -> versions[occurrence.versionId]?.let { version ->
                    Text(version.title, style = MaterialTheme.typography.titleMedium)
                    HabitProgressPetal(version.title, occurrence.quantity, version.target, version.unit, period.profile.reducedMotion)
                    Text(stringResource(R.string.habit_quantity, occurrence.quantity, version.target, version.unit))
                    Text(stringResource(stateLabel(occurrence.state)))
                    PrimaryAction(if (occurrence.state == "COMPLETED") R.string.undo_complete else R.string.log_habit, !state.busy) {
                        if (version.type == "CHECKBOX") model.action({
                            val log = model.repository.logHabit(occurrence.id, if (occurrence.quantity >= version.target) 0.0 else version.target, model.repository.newId())
                            lastHabitLog = log.id
                        }) else { editor = "quantity:${occurrence.id}" }
                    }
                    SecondaryAction(if (occurrence.state == "SKIPPED") R.string.unskip_habit else R.string.skip_habit, !state.busy) { model.action({ model.repository.skipHabit(occurrence.id) }) }
                    SecondaryAction(R.string.edit_action) { editedHabit = version; editor = "habit" }
                    SecondaryAction(R.string.habit_note) { editor = "habitnote:${occurrence.id}" }
                    SecondaryAction(R.string.archive_habit, !state.busy) { model.action({ model.repository.archiveHabit(version.habitId, day) }) }
                } }
                lastHabitLog?.let { id -> SecondaryAction(R.string.undo_action, !state.busy) { model.action({ model.repository.undoHabit(id) }, after = { lastHabitLog = null }) } }
            }
            "CAPTURE" -> SectionCard(R.string.capture_title) {
                PrimaryAction(R.string.capture_idea) { editor = "capture" }
                period.captures.forEach { capture ->
                    Text(capture.text)
                    SecondaryAction(R.string.archive_capture, !state.busy) { model.action({ model.repository.saveCapture(capture.copy(archived = true)) }) }
                }
            }
            "FOOD" -> if (mode != "PAUSED" && period.profile.modules.contains("FOOD")) SectionCard(R.string.food_title) {
                Text(stringResource(R.string.food_reference_description)); SecondaryAction(R.string.open_health, onClick = onHealth)
            }
        } } }
        SecondaryAction(R.string.shutdown_day, !state.busy) { model.action({ model.repository.setDay(day, shutdown = true) }) }
        daySettings?.shutdown?.let { Text(stringResource(R.string.shutdown_recorded)) }
    }
    when {
        editor == "task" -> TaskEditor(model, state, editedTask, onSafety) { editor = null }
        editor == "habit" -> HabitEditor(model, state, editedHabit, onSafety) { editor = null }
        editor == "reset" -> PocketReset(model, onSafety) { editor = null }
        editor?.startsWith("anchor:") == true -> RoutineAnchorEditor(model,state,editor!!,onSafety) { editor = null }
        editor?.startsWith("habitnote:") == true -> {
            val id = editor!!.substringAfter(':'); val key = editor!!
            DraftEditor(model,state,key,R.string.habit_note,mapOf("text" to period.occurrences.firstOrNull { it.id == id }?.notes.orEmpty()),onSafety,{ editor = null }) { form,close ->
                TextInput(form["text"].orEmpty(),R.string.habit_note,{ model.field(key,"text",it) },3)
                PrimaryAction(R.string.save_action,!state.busy) { model.action({ model.repository.habitNote(id,form["text"].orEmpty()) },after = close) }
            }
        }
        editor == "next" || editor == "capture" || editor?.startsWith("quantity:") == true -> {
            val key = editor!!
            DraftEditor(model, state, key, if (key == "next") R.string.next_action_title else if (key == "capture") R.string.capture_title else R.string.log_habit,
                mapOf("text" to if (key == "next") daySettings?.nextAction.orEmpty() else ""), onSafety, { editor = null }) { form, close ->
                TextInput(form["text"].orEmpty(), if (key.startsWith("quantity:")) R.string.quantity_value else R.string.note_text, { model.field(key, "text", it) }, 2)
                PrimaryAction(R.string.save_action, !state.busy) { model.action({
                    when {
                        key == "next" -> model.repository.setDay(day, next = form["text"].orEmpty())
                        key == "capture" -> model.repository.saveCapture(Capture(model.repository.newId(), form["text"].orEmpty(), model.repository.clock.millis()))
                        else -> { val log = model.repository.logHabit(key.substringAfter(':'), form["text"]!!.toDouble(), model.repository.newId()); lastHabitLog = log.id }
                    }
                }, after = close) }
            }
        }
    }
}

@Composable fun DraftEditor(model: ExpandedViewModel, state: ExpandedUiState, key: String, title: Int,
    initial: Map<String, String>, onSafety: () -> Unit, onClose: () -> Unit,
    body: @Composable ColumnScope.(Map<String, String>, () -> Unit) -> Unit) {
    LaunchedEffect(key) { model.openDraft(key, initial) }
    val drafts by model.drafts.collectAsState()
    val form = drafts[key] ?: initial
    val savedClose = { model.clearDraft(key); onClose() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        DialogSurface { ExpandedPage {
            SecondaryAction(R.string.safety_action, onClick = onSafety)
            SecondaryAction(R.string.close_keep_draft, onClick = onClose)
            SectionCard(title) { StateMessages(state); body(form, savedClose) }
        } }
    }
}

@Composable fun TaskEditor(model: ExpandedViewModel, state: ExpandedUiState, original: PlanItem?, onSafety: () -> Unit, onClose: () -> Unit) {
    val period = state.period ?: return
    val key = "task:${original?.id ?: "new"}"
    val initial = mapOf("title" to original?.title.orEmpty(), "notes" to original?.notes.orEmpty(), "day" to (original?.day ?: period.end.toString()),
        "category" to (original?.category ?: "STUDY"), "minutes" to (original?.plannedSeconds?.div(60)?.toString() ?: ""),
        "time" to (original?.timeMinutes?.let { "%02d:%02d".format(java.util.Locale.ROOT, it/60, it%60) } ?: ""),
        "priority" to (original?.priority?.toString() ?: "1"), "essential" to (original?.essential?.toString() ?: "false"), "subject" to original?.subjectId.orEmpty(),"project" to original?.projectId.orEmpty())
    DraftEditor(model, state, key, R.string.task_editor, initial, onSafety, onClose) { form, close ->
        TextInput(form["title"].orEmpty(), R.string.task_title, { model.field(key, "title", it) })
        TextInput(form["notes"].orEmpty(), R.string.note_text, { model.field(key, "notes", it) }, 3)
        TextInput(form["project"].orEmpty(),R.string.project_tag,{ model.field(key,"project",it) })
        TextInput(form["day"].orEmpty(), R.string.logical_date, { model.field(key, "day", it) })
        ChoiceList(form["category"] ?: "STUDY", listOf("STUDY" to R.string.study_kind, "BUILD" to R.string.build_kind, "MOVEMENT" to R.string.movement_kind, "GENERAL" to R.string.general_kind)) { model.field(key, "category", it) }
        TextInput(form["minutes"].orEmpty(), R.string.duration_minutes_optional, { model.field(key, "minutes", it) })
        TextInput(form["time"].orEmpty(), R.string.time_optional, { model.field(key, "time", it) })
        TextInput(form["priority"].orEmpty(), R.string.priority_value, { model.field(key, "priority", it) })
        ToggleRow(R.string.essential_task, form["essential"] == "true") { model.field(key, "essential", it.toString()) }
        SecondaryAction(R.string.no_subject) { model.field(key, "subject", "") }
        period.subjects.filter { !it.archived }.forEach { subject ->
            OutlinedButton(onClick = { model.field(key, "subject", subject.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (form["subject"] == subject.id) stringResource(R.string.selected_choice, subject.title) else subject.title)
            }
        }
        PrimaryAction(R.string.save_action, !state.busy) { model.action({
            val date = LocalDate.parse(form["day"]!!)
            val minutes = form["minutes"]?.takeIf { it.isNotBlank() }?.toLong()
            val time = form["time"]?.takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) }?.let { it.hour * 60 + it.minute }
            model.repository.saveTask(PlanItem(original?.id ?: model.repository.newId(), date.toString(), form["title"].orEmpty(), form["notes"].orEmpty(),
                form["category"] ?: "STUDY", form["subject"]?.takeIf { it.isNotBlank() }, form["project"]?.takeIf(String::isNotBlank), minutes?.times(60), time,
                form["priority"]!!.toInt(), original?.state ?: "PENDING", form["essential"] == "true", original?.position ?: period.tasks.size,
                original?.created ?: model.repository.clock.millis(), model.repository.clock.millis(), original?.zone ?: period.preferences.zoneId,
                original?.boundary ?: period.preferences.boundaryMinutes))
        }, after = close) }
    }
}

@Composable fun HabitEditor(model: ExpandedViewModel, state: ExpandedUiState, original: HabitVersion?, onSafety: () -> Unit, onClose: () -> Unit) {
    val period = state.period ?: return
    val key = "habit:${original?.habitId ?: "new"}"
    val initial = mapOf("title" to original?.title.orEmpty(), "type" to (original?.type ?: "CHECKBOX"), "unit" to (original?.unit ?: stringResource(R.string.times_unit)),
        "target" to (original?.target?.toString() ?: "1"), "weekdays" to (original?.weekdays?.toString() ?: "127"), "interval" to (original?.everyDays?.toString() ?: "1"),
        "essential" to (original?.essential?.toString() ?: "false"), "reminder" to (original?.reminderMinute?.let { "%02d:%02d".format(java.util.Locale.ROOT,it/60,it%60) } ?: ""))
    DraftEditor(model, state, key, R.string.habit_editor, initial, onSafety, onClose) { form, close ->
        TextInput(form["title"].orEmpty(), R.string.habit_title, { model.field(key, "title", it) })
        ChoiceList(form["type"] ?: "CHECKBOX", listOf("CHECKBOX" to R.string.checkbox_habit, "COUNT" to R.string.count_habit, "DURATION" to R.string.duration_habit, "QUANTITY" to R.string.quantity_habit)) { model.field(key, "type", it) }
        TextInput(form["unit"].orEmpty(), R.string.unit_value, { model.field(key, "unit", it) })
        TextInput(form["target"].orEmpty(), R.string.target_value, { model.field(key, "target", it) })
        Text(stringResource(R.string.weekdays_description))
        val mask = form["weekdays"]?.toIntOrNull() ?: 127
        listOf(R.string.monday,R.string.tuesday,R.string.wednesday,R.string.thursday,R.string.friday,R.string.saturday,R.string.sunday).forEachIndexed { index, label ->
            ToggleRow(label, mask and (1 shl index) != 0) { checked -> model.field(key, "weekdays", (if (checked) mask or (1 shl index) else mask and (1 shl index).inv()).toString()) }
        }
        TextInput(form["interval"].orEmpty(), R.string.repeat_days, { model.field(key, "interval", it) })
        TextInput(form["reminder"].orEmpty(), R.string.reminder_optional, { model.field(key, "reminder", it) })
        ToggleRow(R.string.essential_task, form["essential"] == "true") { model.field(key, "essential", it.toString()) }
        Text(stringResource(R.string.habit_history_notice))
        PrimaryAction(R.string.save_action, !state.busy) { model.action({
            val reminder = form["reminder"]?.takeIf { it.isNotBlank() }?.let(LocalTime::parse)?.let { it.hour * 60 + it.minute }
            model.repository.saveHabit(HabitVersion(model.repository.newId(), original?.habitId ?: model.repository.newId(), period.end.toString(),
                form["title"].orEmpty(), form["type"] ?: "CHECKBOX", form["unit"].orEmpty(), form["target"]!!.toDouble(),
                form["weekdays"]!!.toInt(), form["interval"]!!.toInt(), original?.anchorDay ?: period.end.toString(),
                form["essential"] == "true", reminder, model.repository.clock.millis()))
        }, after = close) }
    }
}

@Composable fun PocketReset(model: ExpandedViewModel, onSafety: () -> Unit, onClose: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var next by remember { mutableStateOf("") }
    val state by model.state.collectAsState()
    val labels = listOf(R.string.reset_pause, R.string.reset_breathe, R.string.reset_next)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        DialogSurface { Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) { ExpandedPage {
            SecondaryAction(R.string.safety_action, onClick = onSafety)
            SectionCard(labels[step]) {
                Text(stringResource(R.string.reset_optional))
                if(step == 2) {
                    TextInput(next,R.string.next_action_title,{ next = it },2)
                    PrimaryAction(R.string.save_next_step,!state.busy && next.isNotBlank()) { model.action({
                        model.repository.setDay(model.repository.logicalDay(),next = next)
                    },after = onClose) }
                    StateMessages(state)
                }
                PrimaryAction(if (step == 2) R.string.close_action else R.string.continue_action) { if (step == 2) onClose() else step++ }
                SecondaryAction(R.string.skip_step) { if (step == 2) onClose() else step++ }
                SecondaryAction(R.string.close_action, onClick = onClose)
            }
        } } }
    }
}
