package com.thanu.steady.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.domain.*
import com.thanu.steady.platform.NotificationAdapter
import kotlinx.coroutines.*
import java.time.*
import java.util.Locale

@Composable fun StudyBlockPlanner(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit) {
    val period = state.period ?: return
    val context = LocalContext.current
    val notifier = remember(context) { NotificationAdapter(context) }
    var blocks by remember(period.end) { mutableStateOf<List<StudyBlockProposal>?>(null) }
    var settings by remember { mutableStateOf(StudyBlockSettings()) }
    var loadFailed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    var permission by remember { mutableStateOf(notifier.canNotify()) }
    var testResult by remember { mutableStateOf<Int?>(null) }
    var editor by remember { mutableStateOf<StudyBlockProposal?>(null) }
    val titles = listOf(stringResource(R.string.block_morning), stringResource(R.string.block_afternoon), stringResource(R.string.block_evening))
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permission = notifier.canNotify()
        model.action({}, success = null) // Reschedules only still-valid saved proposals after grant/denial.
    }
    LaunchedEffect(period, state.busy, retry) {
        permission = notifier.canNotify(); loadFailed = false
        try {
            val loaded = withContext(Dispatchers.IO) { model.repository.studyBlocks(period.end) to model.repository.studyBlockSettings() }
            settings = loaded.second
            val templates = StudyBlockRules.templates(period.end, period.preferences.zoneId, period.preferences.boundaryMinutes, titles)
            blocks = loaded.first + templates.filter { template -> loaded.first.none { it.window == template.window } }
        } catch(cancelled: CancellationException) { throw cancelled }
        catch(_: Exception) { loadFailed = true }
    }
    SectionCard(R.string.study_block_planner) {
        Text(stringResource(R.string.study_block_template_notice))
        Text(stringResource(if(permission) R.string.study_block_notifications_ready else R.string.notification_unavailable))
        SecondaryAction(R.string.notification_request) {
            if(Build.VERSION.SDK_INT >= 33) permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            else {
                try { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)) }
                catch(_: android.content.ActivityNotFoundException) { testResult = R.string.notification_unavailable }
                catch(_: SecurityException) { testResult = R.string.notification_unavailable }
            }
        }
        ToggleRow(R.string.study_block_global_reminders, settings.reminders) { enabled ->
            model.action({ model.repository.studyBlockSettings(settings.copy(reminders = enabled)) })
        }
        SecondaryAction(R.string.study_block_test_notification) { testResult = if(notifier.showRoutineReminder()) R.string.study_block_test_sent else R.string.notification_unavailable }
        testResult?.let { Text(stringResource(it)) }
        if(loadFailed) { Text(stringResource(R.string.study_block_load_failed)); SecondaryAction(R.string.retry) { retry++ } }
        else if(blocks == null) Text(stringResource(R.string.loading_records))
        blocks?.forEach { block ->
            val plan = period.tasks.firstOrNull { it.id == block.adoptedPlanId }
            val presented = if(plan != null) block.copy(title = plan.title, day = plan.day, minute = plan.timeMinutes ?: block.minute,
                durationMinutes = plan.plannedSeconds?.let { (it / 60).toInt().coerceAtLeast(1) } ?: block.durationMinutes,
                category = if(plan.category == "BUILD") "BUILD" else "STUDY", primer = plan.notes, zone = plan.zone, boundary = plan.boundary) else block
            val slots = period.tasks.filter { it.state != "ARCHIVED" && it.timeMinutes != null }.map { item ->
                val start = ReminderRules.instant(LocalDate.parse(item.day), item.timeMinutes!!, ZoneId.of(item.zone), item.boundary)
                item.id to WallInterval(start, start + (item.plannedSeconds ?: 0) * 1000)
            }
            Text(presented.title, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.study_block_summary, presented.day, String.format(Locale.ROOT,"%02d:%02d",presented.minute / 60,presented.minute % 60), presented.durationMinutes))
            val conflicts = StudyBlockRules.conflicts(presented, slots)
            if(conflicts > 0) Text(stringResource(R.string.study_block_conflicts, conflicts))
            if(block.adoptedPlanId != null) Text(stringResource(if(plan == null) R.string.study_block_plan_removed else R.string.study_block_adopted))
            SecondaryAction(R.string.study_block_edit) { editor = presented }
            if(block.adoptedPlanId == null) PrimaryAction(R.string.study_block_adopt, !state.busy) { model.action({ model.repository.adoptStudyBlock(block) }) }
            Text(stringResource(if(block.reminder) R.string.study_block_reminder_on else R.string.study_block_reminder_off))
        }
    }
    editor?.let { block ->
        val key = "study-block-editor:${block.id}"
        DraftEditor(model, state, key, R.string.study_block_edit, mapOf("title" to block.title,
            "time" to String.format(Locale.ROOT,"%02d:%02d",block.minute / 60,block.minute % 60), "minutes" to block.durationMinutes.toString(),
            "category" to block.category, "primer" to block.primer, "reminder" to block.reminder.toString(), "lead" to block.leadMinutes.toString()),
            onSafety, { editor = null }) { form, close ->
            TextInput(form["title"].orEmpty(), R.string.task_title, { model.field(key,"title",it) })
            Text(stringResource(R.string.study_block_day_notice, block.day))
            TextInput(form["time"].orEmpty(), R.string.time_optional, { model.field(key,"time",it) })
            TextInput(form["minutes"].orEmpty(), R.string.duration_minutes_optional, { model.field(key,"minutes",it) })
            ChoiceList(form["category"] ?: "STUDY", listOf("STUDY" to R.string.study_kind, "BUILD" to R.string.build_kind)) { model.field(key,"category",it) }
            TextInput(form["primer"].orEmpty(), R.string.study_block_primer, { model.field(key,"primer",it) }, 2)
            ToggleRow(R.string.study_block_reminder_enabled, form["reminder"] == "true") { model.field(key,"reminder",it.toString()) }
            TextInput(form["lead"].orEmpty(), R.string.study_block_lead, { model.field(key,"lead",it) })
            PrimaryAction(R.string.save_action, !state.busy) { model.action({
                val time = LocalTime.parse(form.getValue("time"))
                model.repository.saveStudyBlock(block.copy(title = form.getValue("title"),
                    minute = time.hour * 60 + time.minute, durationMinutes = form.getValue("minutes").toInt(), category = form.getValue("category"),
                    primer = form["primer"].orEmpty(), reminder = form["reminder"] == "true", leadMinutes = form.getValue("lead").toInt()))
            }, after = close) }
        }
    }
}
