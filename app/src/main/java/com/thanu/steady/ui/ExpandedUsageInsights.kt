package com.thanu.steady.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.thanu.steady.R
import com.thanu.steady.platform.UsageInterceptor
import com.thanu.steady.domain.ActivityTotals
import kotlinx.coroutines.*
import java.time.ZoneId

private data class UsageUiState(val loading: Boolean = true, val granted: Boolean = false,
    val failed: Boolean = false, val durations: Map<String, Long> = emptyMap())

@Composable fun ExpandedUsageInsights(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit, onBack: () -> Unit) {
    val period = state.period ?: return
    val context = LocalContext.current
    val adapter = remember(context) { UsageInterceptor(context.applicationContext) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var refresh by remember { mutableStateOf(0) }
    var usage by remember { mutableStateOf(UsageUiState()) }
    var settingsFailed by remember { mutableStateOf(false) }
    val day = state.logicalToday ?: period.end
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if(event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(refresh, day, period.preferences.zoneId, period.preferences.boundaryMinutes) {
        usage = usage.copy(loading = true, failed = false)
        try {
            usage = withContext(Dispatchers.IO) {
                val granted = adapter.checkPermission()
                val range = ActivityTotals.dayBounds(day, ZoneId.of(period.preferences.zoneId), period.preferences.boundaryMinutes)
                val end = minOf(range.end, model.repository.clock.millis()).coerceAtLeast(range.start)
                UsageUiState(loading = false, granted = granted, durations = if(granted) adapter.getUsageInsights(range.start, end) else emptyMap())
            }
        } catch(cancelled: CancellationException) { throw cancelled }
        catch(_: Exception) { usage = UsageUiState(loading = false, failed = true) }
    }
    ExpandedPage {
        SecondaryAction(R.string.safety_action, onClick = onSafety)
        SecondaryAction(R.string.back_action, onClick = onBack)
        SectionCard(R.string.usage_insights_title) {
            Text(stringResource(R.string.usage_range, day.toString(), period.preferences.zoneId))
            Text(stringResource(R.string.interception_unavailable))
            Text(stringResource(R.string.usage_source_notice))
            when {
                usage.loading -> Text(stringResource(R.string.loading_records))
                usage.failed -> Text(stringResource(R.string.usage_unavailable), color = MaterialTheme.colorScheme.error)
                !usage.granted -> Text(stringResource(R.string.usage_permission_required))
                else -> {
                    Text(stringResource(R.string.usage_access_enabled))
                    if(usage.durations.isEmpty()) Text(stringResource(R.string.usage_no_events))
                    usage.durations.entries.sortedByDescending { it.value }.forEach { (name, millis) ->
                        Text(stringResource(R.string.usage_duration_row, name, millis / 60_000.0))
                    }
                }
            }
            if(settingsFailed) Text(stringResource(R.string.usage_settings_unavailable), color = MaterialTheme.colorScheme.error)
            SecondaryAction(R.string.usage_open_settings) { settingsFailed = !adapter.requestPermission() }
            SecondaryAction(R.string.refresh_action) { refresh++ }
        }
    }
}
