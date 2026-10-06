package com.thanu.steady.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thanu.steady.R
import com.thanu.steady.data.InterruptionEvent
import kotlin.math.sin

/** Optional friction practice: no app-blocking claim and no breath-hold requirement. */
@Composable fun VoluntaryPause(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit, onClose: () -> Unit) {
    var seconds by remember { mutableStateOf(10) }
    var started by remember { mutableStateOf<Long?>(null) }
    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(started) {
        val beginning = started ?: return@LaunchedEffect
        while (true) {
            elapsed = (android.os.SystemClock.elapsedRealtime() - beginning).coerceAtLeast(0)
            if (elapsed >= seconds * 1000) break
            kotlinx.coroutines.delay(200)
        }
    }
    val reduced = state.period?.profile?.reducedMotion ?: true
    val scale by animateFloatAsState(if (reduced || started == null) 1f else
        0.8f + sin(elapsed / 2000.0).toFloat() * 0.2f, tween(if (reduced) 0 else 200), label = "optional-pause")
    val primary = MaterialTheme.colorScheme.primary
    val key = "pause:reason"
    DraftEditor(model, state, key, R.string.voluntary_pause, mapOf("reason" to ""), onSafety, onClose) { form, close ->
        Text(stringResource(R.string.pause_comfortable), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(156.dp)) { drawCircle(primary.copy(alpha = 0.25f), size.minDimension / 2 * scale) }
        }
        if (started != null) Text(stringResource(if (elapsed >= seconds * 1000) R.string.pause_timer_done else R.string.pause_seconds_remaining,
            (seconds - elapsed / 1000).coerceAtLeast(0)))
        listOf(5, 10, 15).forEach { duration ->
            OutlinedButton(onClick = { seconds = duration }, enabled = started == null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.pause_duration, duration)) }
        }
        TextInput(form["reason"].orEmpty(), R.string.pause_reason, { model.field(key, "reason", it) }, 2)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.pause_reason_study, R.string.pause_reason_message, R.string.pause_reason_audio, R.string.pause_reason_check).forEach { label ->
                val text = stringResource(label)
                FilterChip(selected = form["reason"] == text, onClick = { model.field(key, "reason", text) },
                    modifier = Modifier.heightIn(min = 56.dp), label = { Text(text) })
            }
        }
        if (started == null) SecondaryAction(R.string.pause_begin) { started = android.os.SystemClock.elapsedRealtime() }
        fun outcome(value: String) {
            val event = InterruptionEvent(model.repository.newId(), outcome = value, at = model.repository.clock.millis(),
                pauseSeconds = started?.let { ((android.os.SystemClock.elapsedRealtime() - it) / 1000).toInt().coerceIn(0, 60) } ?: 0)
            model.action({ model.repository.interruption(event, form["reason"].orEmpty()) }, after = close)
        }
        PrimaryAction(R.string.pause_return, !state.busy) { outcome("RETURNED") }
        SecondaryAction(R.string.pause_continue, !state.busy) { outcome("CONTINUED") }
    }
}
