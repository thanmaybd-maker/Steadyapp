package com.thanu.steady.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.ActivityState
import com.thanu.steady.platform.NotificationAdapter
import com.thanu.steady.platform.AlarmAdapter
import java.util.Locale

@Composable fun ExpandedFocus(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit) {
    val period = state.period ?: return
    val current = period.active.firstOrNull()
    val context = LocalContext.current
    var dim by remember { mutableStateOf(false) }
    var stopOptions by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    var pausePractice by remember { mutableStateOf(false) }
    var subjectEditor by remember { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(NotificationAdapter(context).canNotify()) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = NotificationAdapter(context).canNotify()
    }
    val key = "focus_setup"
    LaunchedEffect(Unit) { model.openDraft(key, mapOf("kind" to "STUDY", "minutes" to "25", "break" to "5", "open" to "false", "subject" to "", "task" to "", "title" to "")) }
    val drafts by model.drafts.collectAsState()
    val form = drafts[key] ?: emptyMap()
    MaterialTheme(colorScheme = if (dim) darkColorScheme(background = Color.Black, surface = Color(0xFF101010),
        primary = Color(0xFF82DAB3), onPrimary = Color(0xFF002114), onSurface = Color.White, onBackground = Color.White)
        else MaterialTheme.colorScheme) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ExpandedPage {
            StateMessages(state)
            if (current == null) SectionCard(R.string.focus_setup_title) {
                ChoiceList(form["kind"] ?: "STUDY", listOf("STUDY" to R.string.study_kind, "BUILD" to R.string.build_kind)) { model.field(key, "kind", it) }
                TextInput(form["title"].orEmpty(), R.string.focus_intention, { model.field(key, "title", it) })
                listOf(15 to 3, 25 to 5, 50 to 10, 90 to 20).forEach { (minutes, rest) ->
                    OutlinedButton(onClick = { model.field(key, "minutes", minutes.toString()); model.field(key, "break", rest.toString()); model.field(key, "open", "false") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(stringResource(R.string.focus_preset, minutes, rest))
                    }
                }
                TextInput(form["minutes"].orEmpty(), R.string.duration_minutes, { model.field(key, "minutes", it) })
                TextInput(form["break"].orEmpty(), R.string.break_minutes, { model.field(key, "break", it) })
                ToggleRow(R.string.open_ended, form["open"] == "true") { model.field(key, "open", it.toString()) }
                SecondaryAction(R.string.no_subject) { model.field(key, "subject", "") }
                period.subjects.filter { !it.archived }.forEach { subject ->
                    OutlinedButton(onClick = { model.field(key, "subject", subject.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text(if (form["subject"] == subject.id) stringResource(R.string.selected_choice, subject.title) else subject.title)
                    }
                }
                SecondaryAction(R.string.add_subject) { subjectEditor = true }
                PrimaryAction(R.string.timer_start, !state.busy) { model.action({
                    val duration = if (form["open"] == "true") null else form["minutes"]!!.toLong().times(60)
                    model.startActivity("FOCUS", form["kind"] ?: "STUDY", form["title"].orEmpty(), duration,
                        form["break"]!!.toLong().times(60), form["subject"]?.takeIf(String::isNotBlank), form["task"]?.takeIf(String::isNotBlank))
                }) }
            } else SectionCard(if (current.type == "WORKOUT") R.string.active_workout else R.string.active_focus) {
                Text(current.title)
                Text(stringResource(stateLabel(current.state)))
                val elapsed = state.activeMillis
                val remaining = current.plannedSeconds?.times(1000)?.minus(elapsed)?.coerceAtLeast(0)
                val displayed = remaining ?: elapsed
                Text(String.format(Locale.ROOT, "%02d:%02d", displayed / 60_000, displayed % 60_000 / 1000),
                    style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.actual_seconds, elapsed / 1000))
                PrimaryAction(if (current.state == "RUNNING") R.string.timer_pause else R.string.timer_resume, !state.busy) {
                    model.transition(if (current.state == "RUNNING") ActivityState.PAUSED else ActivityState.RUNNING)
                }
                PrimaryAction(R.string.stop_session, !state.busy) {
                    if (current.state == "RUNNING") model.transition(ActivityState.PAUSED)
                    stopOptions = true
                }
                SecondaryAction(if (dim) R.string.timer_undim else R.string.timer_dim) { dim = !dim }
                SecondaryAction(R.string.safety_action, onClick = onSafety)
                Text(stringResource(R.string.reboot_limit))
            }
            SectionCard(R.string.scratchpad_title) {
                TextInput(state.scratchpad, R.string.note_text, model::scratchpad, 5)
                Text(stringResource(state.noteStatus))
                if (state.noteStatus == R.string.note_retry) SecondaryAction(R.string.retry, onClick = model::retryNote)
                Text(stringResource(R.string.scratchpad_privacy))
            }
            SectionCard(R.string.task_queue) {
                val queue = period.tasks.filter { it.state == "PENDING" && it.category in setOf("STUDY", "BUILD") }
                if (queue.isEmpty()) Text(stringResource(R.string.empty_tasks))
                queue.forEach { task ->
                    Text(task.title)
                    SecondaryAction(R.string.choose_task) { model.field(key, "task", task.id); model.field(key, "title", task.title); task.subjectId?.let { model.field(key, "subject", it) } }
                }
            }
            SectionCard(R.string.break_tools) {
                if (current == null) period.sessions.firstOrNull { it.type == "FOCUS" && it.state == "COMPLETED" && it.breakSeconds > 0 }?.let { previous ->
                    PrimaryAction(R.string.start_break, !state.busy) { model.start("BREAK", "REST", "", previous.breakSeconds, 0) }
                }
                SecondaryAction(R.string.pocket_reset) { reset = true }
                SecondaryAction(R.string.voluntary_pause) { pausePractice = true }
                if (!permissionGranted) {
                    Text(stringResource(R.string.notification_unavailable))
                    if (Build.VERSION.SDK_INT >= 33) SecondaryAction(R.string.notification_request) { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                }
                Text(stringResource(if (AlarmAdapter(context).hasExactAccess()) R.string.timer_exact else R.string.timer_approximate))
            }
        }
    }
    }
    if (stopOptions && current != null) AlertDialog(onDismissRequest = { stopOptions = false },
        title = { Text(stringResource(R.string.stop_session)) }, text = { Text(stringResource(R.string.partial_session_description, state.activeMillis / 1000)) },
        confirmButton = { TextButton(enabled = !state.busy, onClick = { model.transition(ActivityState.STOPPED); stopOptions = false }, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.save_partial)) } },
        dismissButton = { Column {
            TextButton(enabled = !state.busy, onClick = { model.transition(ActivityState.RUNNING); stopOptions = false }, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.timer_resume)) }
            TextButton(enabled = !state.busy, onClick = { model.transition(ActivityState.DISCARDED); stopOptions = false }, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.discard_session)) }
            TextButton(onClick = { stopOptions = false; onSafety() }, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(R.string.safety_action)) }
        } })
    if (reset) PocketReset(model, onSafety) { reset = false }
    if (pausePractice) VoluntaryPause(model, state, onSafety) { pausePractice = false }
    if (subjectEditor) DraftEditor(model, state, "subject:new", R.string.add_subject, mapOf("title" to "", "code" to ""), onSafety, { subjectEditor = false }) { values, close ->
        TextInput(values["title"].orEmpty(), R.string.subject_title, { model.field("subject:new", "title", it) })
        TextInput(values["code"].orEmpty(), R.string.subject_code, { model.field("subject:new", "code", it) })
        PrimaryAction(R.string.save_action, !state.busy) { model.subject(Subject(model.repository.newId(), values["title"].orEmpty(), values["code"].orEmpty()), close) }
    }
}

@Composable fun VoluntaryPause(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit, onClose: () -> Unit) {
    var seconds by remember { mutableStateOf(5) }
    var started by remember { mutableStateOf<Long?>(null) }
    var elapsed by remember { mutableStateOf(0) }
    LaunchedEffect(started) {
        val beginning = started ?: return@LaunchedEffect
        while (true) {
            elapsed = ((android.os.SystemClock.elapsedRealtime() - beginning) / 1000).toInt().coerceAtLeast(0)
            if (elapsed >= seconds) break
            kotlinx.coroutines.delay(200)
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) { ExpandedPage {
            SecondaryAction(R.string.safety_action, onClick = onSafety)
            SectionCard(R.string.voluntary_pause) {
                Text(stringResource(R.string.voluntary_description))
                listOf(3, 5, 10, 15).forEach { value -> OutlinedButton(enabled = started == null, onClick = { seconds = value }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.pause_seconds, value))
                } }
                Text(stringResource(R.string.voluntary_no_blocking))
                if (started == null) PrimaryAction(R.string.begin_pause) { started = android.os.SystemClock.elapsedRealtime() }
                else Text(stringResource(R.string.pause_remaining, (seconds - elapsed).coerceAtLeast(0)))
                fun outcome(value: String) { model.action({ model.repository.interruption(InterruptionEvent(model.repository.newId(), outcome = value,
                    at = model.repository.clock.millis(), pauseSeconds = started?.let { ((android.os.SystemClock.elapsedRealtime() - it)/1000).toInt().coerceIn(0,60) } ?: 0)) }, after = onClose) }
                PrimaryAction(R.string.return_focus, !state.busy) { outcome("RETURNED") }
                SecondaryAction(R.string.continue_temporarily, !state.busy) { outcome("CONTINUED") }
                SecondaryAction(R.string.disable_pause, !state.busy) { outcome("DISABLED") }
            }
        } }
    }
}
