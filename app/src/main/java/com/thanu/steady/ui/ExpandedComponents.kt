package com.thanu.steady.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thanu.steady.R

@Composable fun ExpandedPage(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp), content = content)
}
val LocalRoomyCard = staticCompositionLocalOf { true }
@Composable fun SectionCard(@StringRes title: Int, content: @Composable ColumnScope.() -> Unit) {
    val roomy = LocalRoomyCard.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if(roomy) 24.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(if(roomy) 16.dp else 8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}
@Composable fun PrimaryAction(@StringRes label: Int, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(label)) }
}
@Composable fun SecondaryAction(@StringRes label: Int, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(label)) }
}
@Composable fun TextInput(value: String, @StringRes label: Int, onChange: (String) -> Unit,
    minLines: Int = 1, supporting: String? = null) {
    OutlinedTextField(value, onChange, label = { Text(stringResource(label)) }, minLines = minLines,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), supportingText = supporting?.let { { Text(it) } })
}
@Composable fun ChoiceList(value: String, choices: List<Pair<String, Int>>, onChange: (String) -> Unit) {
    choices.forEach { (key, label) ->
        OutlinedButton(onClick = { onChange(key) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (key == value)
                MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
            Text(stringResource(if (key == value) R.string.selected_choice else R.string.available_choice, stringResource(label)))
        }
    }
}
@Composable fun ToggleRow(@StringRes label: Int, value: Boolean, onChange: (Boolean) -> Unit) {
    val name = stringResource(label)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(label), modifier = Modifier.weight(1f))
        Switch(value, onChange, modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp).semantics { contentDescription = name })
    }
}
@Composable fun MetricRing(@StringRes title: Int, value: Double?, target: Double?, summary: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline
    OutlinedButton(onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Canvas(Modifier.size(56.dp)) {
                val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                val inset = stroke.width / 2
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
                if (value != null && target != null && target > 0) drawArc(primary, -90f,
                    ((value / target).coerceIn(0.0, 1.0) * 360).toFloat(), false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            }
            Column(Modifier.weight(1f)) { Text(stringResource(title), style = MaterialTheme.typography.titleMedium); Text(summary) }
        }
    }
}
@Composable fun StateMessages(state: ExpandedUiState) {
    if (state.loading) { CircularProgressIndicator(); Text(stringResource(R.string.loading_records)) }
    if (state.busy) Text(stringResource(R.string.saving))
    state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
    state.message?.let { Text(stringResource(it)) }
}

fun stateLabel(value: String): Int = when(value) {
    "NORMAL" -> R.string.normal_day; "MINIMUM" -> R.string.minimum_day; "PAUSED" -> R.string.timer_paused
    "PENDING" -> R.string.pending; "PARTIAL" -> R.string.partial; "COMPLETED" -> R.string.completed
    "SKIPPED" -> R.string.skipped; "MISSING" -> R.string.missing; "NOT_DUE" -> R.string.not_scheduled
    "RUNNING" -> R.string.timer_running; "INTERRUPTED" -> R.string.timer_interrupted
    "STOPPED" -> R.string.timer_cancelled; "DISCARDED" -> R.string.discarded
    else -> R.string.timer_idle
}
