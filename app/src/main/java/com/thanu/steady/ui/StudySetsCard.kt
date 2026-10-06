package com.thanu.steady.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.data.Capture

@Composable fun StudySetsAndReps(model: ExpandedViewModel, state: ExpandedUiState) {
    val period = state.period ?: return
    val current = period.active.firstOrNull { it.type == "FOCUS" }
    val history = period.sessions.filter { it.type == "FOCUS" && completedInPeriod(it, period) }
    val tasks = period.tasks.filter { it.category in setOf("STUDY", "BUILD") && it.state != "ARCHIVED" }
    SectionCard(R.string.study_sets_title) {
        Text(stringResource(R.string.study_sets_summary, history.size, tasks.size), color = MaterialTheme.colorScheme.onSurface)
        current?.let { session ->
            val alpha = if (period.profile.reducedMotion || session.state != "RUNNING") 1f else {
                val transition = rememberInfiniteTransition(label = "current-set")
                val value by transition.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "current-set-pulse")
                value
            }
            Text(stringResource(R.string.current_set_badge), style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.graphicsLayer { this.alpha = alpha })
            Text(session.title, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.actual_seconds, state.activeMillis / 1000))
            ActiveEffort(model, state, session)
        }
        tasks.forEach { task ->
            Text(task.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(stateLabel(task.state)), color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrimaryAction(if (task.state == "COMPLETED") R.string.undo_complete else R.string.complete_task, !state.busy) {
                model.action({ model.repository.toggleTask(task.id) })
            }
        }
        history.forEach { session ->
            Text(session.title, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.actual_seconds, session.activeMillis / 1000))
            session.effort?.let { Text(stringResource(R.string.effort_value, it)) }
        }
        if (tasks.isEmpty() && current == null && history.isEmpty()) Text(stringResource(R.string.empty_tasks))
    }
}

@Composable fun LiveScratchpadCard(state: ExpandedUiState, model: ExpandedViewModel, onSafety: () -> Unit) {
    var capture by remember { mutableStateOf(false) }
    SectionCard(R.string.scratchpad_title) {
        TextInput(state.scratchpad, R.string.note_text, model::scratchpad, 5)
        Text(stringResource(state.noteStatus))
        if (state.noteStatus == R.string.note_retry) SecondaryAction(R.string.retry, onClick = model::retryNote)
        Text(stringResource(R.string.scratchpad_privacy))
        SecondaryAction(R.string.capture_thought) { capture = true }
    }
    if (capture) DraftEditor(model, state, "focus:thought", R.string.capture_thought,
        mapOf("text" to ""), onSafety, { capture = false }) { form, close ->
        TextInput(form["text"].orEmpty(), R.string.note_text, { model.field("focus:thought", "text", it) }, 3)
        PrimaryAction(R.string.save_action, !state.busy && !form["text"].isNullOrBlank()) {
            model.action({ model.repository.saveCapture(Capture(model.repository.newId(), form["text"].orEmpty(), model.repository.clock.millis())) }, after = close)
        }
    }
}
