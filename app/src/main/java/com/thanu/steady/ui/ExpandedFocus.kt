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
            SubjectTopics(model,state,onSafety)
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

@Composable fun VoluntaryPause(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit, onClose: () -> Unit) {
    var seconds by remember { mutableStateOf(10) }
    var started by remember { mutableStateOf<Long?>(null) }
    var elapsed by remember { mutableStateOf(0) }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(started) {
        val beginning = started ?: return@LaunchedEffect
        while (true) {
            elapsed = ((android.os.SystemClock.elapsedRealtime() - beginning) / 1000).toInt().coerceAtLeast(0)
            if (elapsed >= seconds) break
            kotlinx.coroutines.delay(200)
        }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        DialogSurface { ExpandedPage {
            SecondaryAction(R.string.safety_action, onClick = onSafety)
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Voluntary Pause", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Take a moment to reflect before proceeding", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(160.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(100)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (started == null) {
                                Text("Inhale 1s", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            } else {
                                val remaining = (seconds - elapsed).coerceAtLeast(0)
                                Text(String.format(java.util.Locale.ROOT, "00:%02d", remaining), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                                Text("${seconds}s Breath Hold to Unlock", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(5, 10, 15).forEach { value ->
                        OutlinedButton(onClick = { seconds = value }, enabled = started == null, modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Text(if (value == 5) "5s Quick" else if (value == 10) "10s Calm" else "15s Fort")
                        }
                    }
                }

                androidx.compose.material3.OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Conscious Reason [Required]") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())) {
                    listOf("Check quick syllabus", "Urgent message", "Audio control", "Impulse check").forEach { chip ->
                        androidx.compose.material3.FilterChip(selected = reason == chip, onClick = { reason = chip }, label = { Text(chip) })
                    }
                }

                // Removed mock telemetry about impulses

                fun outcome(value: String) { model.action({ model.repository.interruption(InterruptionEvent(model.repository.newId(), outcome = value, at = model.repository.clock.millis(), pauseSeconds = started?.let { ((android.os.SystemClock.elapsedRealtime() - it)/1000).toInt().coerceIn(0,60) } ?: 0)) }, after = onClose) }

                if (started == null) {
                    Button(onClick = { started = android.os.SystemClock.elapsedRealtime() }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Initiate Breath Hold to Proceed")
                    }
                } else if (elapsed >= seconds) {
                    Button(onClick = { outcome("CONTINUED") }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = reason.isNotBlank(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Emergency 1-Minute Micro-Check (60s auto-exit)")
                    }
                }

                Button(onClick = { outcome("RETURNED") }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Return to Deep Study")
                        Text("Target: 45 min focus block remaining", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } }
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
                FocusRings(modifier = Modifier.fillMaxSize(), outerProgress = outerProgress, innerProgress = 0.75f)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (remaining == null) "ELAPSED" else "REMAINING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(timerText, style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold))
                    // Removed mocked telemetry
                }
            }
            Spacer(Modifier.height(16.dp))
            val engine = model.audioSoundscapeEngine
            val isPlaying by engine.isPlaying.collectAsState()
            val currentType by engine.currentSoundscape.collectAsState()
            val volume by engine.volume.collectAsState()
            val scope = rememberCoroutineScope()

            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Ambient Audio", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = {
                        if (isPlaying) engine.stop()
                        else {
                            engine.start(scope)
                            if (currentType == null) engine.playSoundscape(com.thanu.steady.platform.AmbientSoundType.FOREST.name)
                        }
                    }) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                    }
                }
                if (isPlaying) {
                    var expandedSound by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expandedSound = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(currentType?.let { com.thanu.steady.platform.AmbientSoundType.valueOf(it).title } ?: "Select Sound")
                        }
                        DropdownMenu(expanded = expandedSound, onDismissRequest = { expandedSound = false }) {
                            com.thanu.steady.platform.AmbientSoundType.entries.forEach { type ->
                                DropdownMenuItem(text = { Text("${type.iconEmoji} ${type.title}") }, onClick = {
                                    engine.playSoundscape(type.name)
                                    expandedSound = false
                                })
                            }
                        }
                    }
                    Slider(value = volume, onValueChange = engine::setVolume, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { model.transition(if (current.state == "RUNNING") ActivityState.PAUSED else ActivityState.RUNNING) }, modifier = Modifier.weight(1f).height(48.dp), enabled = !state.busy) {
                    Icon(if (current.state == "RUNNING") Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (current.state == "RUNNING") "Pause Set" else "Resume Set")
                }
                Button(onClick = {
                    if (current.state == "RUNNING") model.transition(ActivityState.PAUSED)
                    onStop()
                }, modifier = Modifier.height(48.dp), enabled = !state.busy, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) {
                    Icon(Icons.Filled.SelfImprovement, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop Session")
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

@Composable
fun StudySetsAndReps() {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondary.copy(alpha=0.1f), shape = RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                }
                Text("Study Sets & Reps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text("Block 2 of 4 Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Set 1
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    }
                    Column {
                        Text("Carbonyl Condensations", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Text("Set 1 • 45m Focused Rep", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("RPE 6", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), shape = RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp))
            }

            // Set 3 ACTIVE
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                        Text("3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Stereochemistry Problems", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Active Current Rep • 28m Left", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("NOW", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.background(MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }

        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SET 3 SUBJECTIVE LOAD (RPE 1-10)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Text("RPE 7 • Hard Steady", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            androidx.compose.material3.Slider(value = 7f, onValueChange = {}, valueRange = 1f..10f, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AmbientSoundscape() {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("Neural Synthesizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Isochronic & Binaural Layering", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(176.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50)).padding(12.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(128.dp).background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Waves, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("40 Hz", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("GAMMA FOCUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveScratchpadCard(state: ExpandedUiState, model: ExpandedViewModel) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp)).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), shape = RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Text("Live Scratchpad", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("# Active Proof Scratchpad", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("[✓]", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Nucleophilic attack at C2 carbonyl", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("[ ]", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Text("Verify stereochemical inversion (R -> S)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
        }

        androidx.compose.material3.OutlinedTextField(value = state.scratchpad, onValueChange = model::scratchpad, placeholder = { Text("Park intrusive thought or doubt...") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50))
    }
}

@Composable
fun BioBreakPrompt() {
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.LocalCafe, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        }
        Column {
            Text("Upcoming Bio-Reset", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
            Text("20-20-20 Optical Rest & Electrolytes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("In 16 mins: Gaze 20ft away for 20 seconds, hydrate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
