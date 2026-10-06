package com.thanu.steady.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thanu.steady.R
import com.thanu.steady.domain.TimerState
import com.thanu.steady.platform.AlarmAdapter
import com.thanu.steady.platform.NotificationAdapter
import java.util.Locale

@Composable
fun BreakScreen(viewModel: BreakViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val notifications = remember(context) { NotificationAdapter(context) }
    val alarms = remember(context) { AlarmAdapter(context) }
    var canNotify by remember { mutableStateOf(notifications.canNotify()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = notifications.canNotify()
    }
    val minutes = state.displayRemainingMs / 60_000
    val seconds = state.displayRemainingMs % 60_000 / 1000
    val remainingLabel = stringResource(R.string.time_remaining, minutes, seconds)
    val stateLabel = stringResource(when (state.session.state) {
        TimerState.IDLE -> R.string.timer_idle
        TimerState.RUNNING -> R.string.timer_running
        TimerState.PAUSED -> R.string.timer_paused
        TimerState.COMPLETED -> R.string.timer_complete
        TimerState.CANCELLED -> R.string.timer_cancelled
        TimerState.INTERRUPTED -> R.string.timer_interrupted
    })
    val colour = if (state.isDimmed) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground
    Column(Modifier.fillMaxSize().background(if (state.isDimmed) Color.Black else MaterialTheme.colorScheme.background)
        .verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(String.format(Locale.ROOT, "%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.displayMedium, color = colour,
            modifier = Modifier.semantics { contentDescription = remainingLabel })
        Text(stringResource(R.string.timer_state, stateLabel), color = colour)
        state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        if (state.busy) CircularProgressIndicator()
        val running = state.session.state == TimerState.RUNNING
        Button(onClick = { viewModel.setDuration(5) }, enabled = !state.busy && !running,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.preset_five)) }
        Button(onClick = { viewModel.setDuration(25) }, enabled = !state.busy && !running,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.preset_twenty_five)) }
        Button(onClick = { if (running) viewModel.pause() else viewModel.start() }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(if (running) R.string.timer_pause else if (state.session.state in
                listOf(TimerState.PAUSED, TimerState.INTERRUPTED)) R.string.timer_resume else R.string.timer_start))
        }
        OutlinedButton(onClick = { viewModel.stop() }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.timer_stop)) }
        OutlinedButton(onClick = viewModel::toggleDim, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(if (state.isDimmed) R.string.timer_undim else R.string.timer_dim))
        }
        Text(stringResource(if (alarms.hasExactAccess()) R.string.timer_exact else R.string.timer_approximate), color = colour)
        if (!canNotify) {
            Text(stringResource(R.string.notification_unavailable), color = colour)
            if (Build.VERSION.SDK_INT >= 33) OutlinedButton(onClick = { permission.launch(Manifest.permission.POST_NOTIFICATIONS) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(stringResource(R.string.notification_request)) }
        }
    }
}
