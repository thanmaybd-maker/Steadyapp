package com.thanu.steady.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.ActivityTotals
import com.thanu.steady.domain.WallInterval
import java.time.ZoneId

/** OG's four-card matrix, with actual records and independent readable details. */
@Composable fun RecordedMiniMatrix(model: ExpandedViewModel,state: ExpandedUiState,onSafety: () -> Unit) {
    val period = state.period ?: return
    var selected by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<ActivitySession?>(null) }
    val rows = focusRestRows(period)
    val choices = listOf("WATER" to R.string.water_title,"FOCUS" to R.string.focus_tab,"REST" to R.string.recorded_matrix_rest,
        "WORKOUT" to R.string.movement_title)
    val totalWater = period.water.sumOf { it.millilitres.toLong() }
    val labels = mapOf("WATER" to waterAmount(totalWater,period.profile.waterUnit),
        "FOCUS" to stringResource(R.string.recorded_matrix_minutes,rows.sumOf { it.focusMillis } / 60_000.0),
        "REST" to stringResource(R.string.recorded_matrix_minutes,rows.sumOf { it.restMillis } / 60_000.0),
        "WORKOUT" to stringResource(R.string.recorded_matrix_minutes,activityMillis(period,"WORKOUT") / 60_000.0))
    @Composable fun cell(kind: String,title: Int,modifier: Modifier) {
        Card(modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(stringResource(title),style=MaterialTheme.typography.titleMedium)
                Text(labels.getValue(kind),style=MaterialTheme.typography.headlineSmall)
                if(kind == "WATER") {
                    val progress = com.thanu.steady.domain.ChartRules.progress(totalWater.toDouble(),period.profile.waterTargetMl?.toDouble())
                    if(progress != null) LinearProgressIndicator(progress,Modifier.fillMaxWidth())
                }
                OutlinedButton(onClick={ selected=kind },modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {
                    Text(stringResource(R.string.recorded_matrix_open,stringResource(title)))
                }
            }
        }
    }
    SectionCard(R.string.recorded_matrix_title) {
        Text(stringResource(R.string.recorded_matrix_notice))
        if(LocalDensity.current.fontScale > 1.4f) choices.forEach { (kind,title) -> cell(kind,title,Modifier.fillMaxWidth()) }
        else choices.chunked(2).forEach { pair -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            pair.forEach { (kind,title) -> cell(kind,title,Modifier.weight(1f)) }
        } }
    }
    selected?.let { kind ->
        Dialog(onDismissRequest={ selected=null },properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
            DialogSurface { ExpandedPage {
                SecondaryAction(R.string.safety_action,onClick=onSafety)
                SecondaryAction(R.string.close_action) { selected=null }
                Text(stringResource(choices.first { it.first == kind }.second),style=MaterialTheme.typography.headlineSmall)
                Text(labels.getValue(kind))
                if(kind == "WATER") {
                    if(period.water.isEmpty()) Text(stringResource(R.string.recorded_matrix_empty))
                    period.water.forEach { log -> Text(log.day); Text(stringResource(R.string.water_serving,log.millilitres)) }
                } else {
                    val sessions = period.sessions.filter { it.state != "DISCARDED" && if(kind == "REST") it.type in setOf("BREAK","REST") else it.type == kind }
                    val records = sessions.mapNotNull { session ->
                        val pieces = period.segments.filter { it.sessionId == session.id && it.endWall != null && it.activeMillis > 0 }.mapNotNull { span ->
                            val start = maxOf(span.startWall,ActivityTotals.dayBounds(period.start,ZoneId.of(span.zone),span.boundary).start)
                            val end = minOf(span.endWall!!,ActivityTotals.dayBounds(period.end,ZoneId.of(span.zone),span.boundary).end)
                            if(end > start) WallInterval(start,end) else null
                        }
                        ActivityTotals.unionMillis(pieces).takeIf { it > 0 }?.let { session to it }
                    }
                    if(records.isEmpty()) Text(stringResource(R.string.recorded_matrix_empty))
                    records.forEach { (session,millis) ->
                        Text(session.title.ifBlank { stringResource(R.string.recorded_matrix_unnamed) })
                        Text(stringResource(R.string.actual_seconds,millis / 1000))
                        if(session.state in setOf("STOPPED","COMPLETED")) SecondaryAction(R.string.edit_action) { editing=session }
                    }
                }
            } }
        }
    }
    editing?.let { HistoryEditor(model,state,it,onSafety) { editing=null } }
}
