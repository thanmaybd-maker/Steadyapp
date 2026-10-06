package com.thanu.steady.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thanu.steady.R
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
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
            } else {
                KineticFocusTimer(current, state, model, dim, { dim = !dim }, { stopOptions = true }, onSafety)
            }
            LiveScratchpadCard(state, model, onSafety)
            StudySetsAndReps(model, state)
            AmbientSoundscape(model, state, onSafety)
            SectionCard(R.string.task_queue) {
                val queue = period.tasks.filter { it.state == "PENDING" && it.category in setOf("STUDY", "BUILD") }
                if (queue.isEmpty()) Text(stringResource(R.string.empty_tasks))
                queue.forEach { task ->
                    Text(task.title)
                    SecondaryAction(R.string.choose_task) { model.field(key, "task", task.id); model.field(key, "title", task.title); task.subjectId?.let { model.field(key, "subject", it) } }
                }
            }
            SubjectTopics(model,state,onSafety)
            SectionCard(R.string.break_tools) {
                val pauses by produceState<Int?>(null, period.end, state.busy) {
                    try { value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { model.repository.interruptionCount(period.end) } }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { value = null }
                }
                pauses?.let { Text(stringResource(R.string.pause_decisions_count, it)) }
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
    if (stopOptions && current != null) Dialog(onDismissRequest = { stopOptions = false },
        properties = DialogProperties(usePlatformDefaultWidth = false,decorFitsSystemWindows = false)) {
        DialogSurface { ExpandedPage {
            SecondaryAction(R.string.safety_action) { stopOptions = false; onSafety() }
            SectionCard(R.string.stop_session) {
                Text(stringResource(R.string.partial_session_description,state.activeMillis/1000))
                PrimaryAction(R.string.save_partial,!state.busy) { model.transition(ActivityState.STOPPED); stopOptions = false }
                SecondaryAction(R.string.timer_resume,!state.busy) { model.transition(ActivityState.RUNNING); stopOptions = false }
                SecondaryAction(R.string.discard_session,!state.busy) { model.transition(ActivityState.DISCARDED); stopOptions = false }
                SecondaryAction(R.string.close_action) { stopOptions = false }
            }
        } }
    }
    if (reset) PocketReset(model, onSafety) { reset = false }
    if (pausePractice) VoluntaryPause(model, state, onSafety) { pausePractice = false }
    if (subjectEditor) DraftEditor(model, state, "subject:new", R.string.add_subject, mapOf("title" to "", "code" to ""), onSafety, { subjectEditor = false }) { values, close ->
        TextInput(values["title"].orEmpty(), R.string.subject_title, { model.field("subject:new", "title", it) })
        TextInput(values["code"].orEmpty(), R.string.subject_code, { model.field("subject:new", "code", it) })
        PrimaryAction(R.string.save_action, !state.busy) { model.subject(Subject(model.repository.newId(), values["title"].orEmpty(), values["code"].orEmpty()), close) }
    }
}

@Composable
fun KineticFocusTimer(current: com.thanu.steady.data.ActivitySession, state: ExpandedUiState, model: ExpandedViewModel, dim: Boolean, onDimToggle: () -> Unit, onStop: () -> Unit, onSafety: () -> Unit) {
    val elapsed = state.activeMillis
    val remaining = current.plannedSeconds?.times(1000)?.minus(elapsed)?.coerceAtLeast(0)
    val displayed = remaining ?: elapsed
    val timerText = String.format(java.util.Locale.ROOT, "%02d:%02d", displayed / 60_000, displayed % 60_000 / 1000)
    val outerProgress = current.plannedSeconds?.let { if (it > 0) (elapsed.toFloat() / (it * 1000f)).coerceIn(0f, 1f) else 0f } ?: 0f
    val view = androidx.compose.ui.platform.LocalView.current
    val readout = androidx.compose.ui.res.stringResource(if(remaining == null) R.string.read_elapsed_time else R.string.read_remaining_time,displayed/1000)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                FocusRings(modifier = Modifier.fillMaxSize(), outerProgress = outerProgress, innerProgress = com.thanu.steady.domain.ChartRules.progress((focusMillis(state.period!!) + if (current.type == "FOCUS") (state.activeMillis - current.activeMillis).coerceAtLeast(0) else 0) / 60_000.0, state.period.profile.focusTargetMinutes?.toDouble()) ?: 0f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(if (remaining == null) R.string.timer_elapsed_label else R.string.timer_remaining_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(timerText, style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold))
                    // Removed mocked telemetry
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { model.transition(if (current.state == "RUNNING") ActivityState.PAUSED else ActivityState.RUNNING) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp), enabled = !state.busy) {
                    Icon(if (current.state == "RUNNING") Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(if (current.state == "RUNNING") R.string.timer_pause else R.string.timer_resume))
                }
                Button(onClick = {
                    if (current.state == "RUNNING") model.transition(ActivityState.PAUSED)
                    onStop()
                }, modifier = Modifier.heightIn(min = 56.dp), enabled = !state.busy, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Icon(Icons.Filled.SelfImprovement, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.stop_session))
                }
            }
        }

        SectionCard(R.string.active_focus) {
            SecondaryAction(R.string.read_timer_time) { view.announceForAccessibility(readout) }
            SecondaryAction(if (dim) R.string.timer_undim else R.string.timer_dim, onClick = onDimToggle)
            SecondaryAction(R.string.safety_action, onClick = onSafety)
        }
    }
}

@Composable
fun FocusRings(modifier: Modifier = Modifier, outerProgress: Float, innerProgress: Float) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryTrackColor = MaterialTheme.colorScheme.surfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val secondaryTrackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width / 2f * (102f / 120f)
        val innerRadius = size.width / 2f * (86f / 120f)

        drawCircle(color = primaryTrackColor, radius = outerRadius, center = center, style = Stroke(width = 6.dp.toPx()))
        drawArc(color = primaryColor, startAngle = -90f, sweepAngle = 360f * outerProgress, useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius), size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color = secondaryTrackColor, radius = innerRadius, center = center, style = Stroke(width = 4.dp.toPx()))
        drawArc(color = secondaryColor, startAngle = -90f, sweepAngle = 360f * innerProgress, useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius), size = Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
    }
}
