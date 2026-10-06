package com.thanu.steady.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.thanu.steady.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: (() -> Unit)? = null, onImportCompleted: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    var passwordMode by remember { mutableStateOf<String?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val legacyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(),viewModel::prepareLegacy)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) {
        viewModel.exportMarkdown(it)
    }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {
        viewModel.writeBackup(it)
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) viewModel.status(R.string.file_cancelled) else {
            restoreUri = uri; passwordMode = "restore"
        }
    }
    LaunchedEffect(state.backupReady) {
        if (state.backupReady) {
            viewModel.backupPickerLaunched()
            backupLauncher.launch("steady-${state.exportDate}.steadybackup")
        }
    }
    LaunchedEffect(state.importCompleted) {
        if (state.importCompleted) {
            viewModel.importHandled()
            onImportCompleted()
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeading(R.string.settings_title)
        onBack?.let { SecondaryAction(R.string.back_action, onClick = it) }
        state.statusMessage?.let {
            Text(stringResource(it), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (state.isProcessing) {
            CircularProgressIndicator()
            Text(stringResource(R.string.processing))
        }
        state.legacyResult?.let { Text(stringResource(R.string.legacy_og_result,it[0],it[1])) }
        Text(stringResource(R.string.markdown_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(state.exportDate, viewModel::setExportDate,
            label = { Text(stringResource(R.string.export_date)) }, modifier = Modifier.fillMaxWidth(),
            enabled = !state.isProcessing, singleLine = true)
        TextInput(state.exportEnd,R.string.export_end_optional,viewModel::setExportEnd)
        listOf("TASKS" to R.string.timeline_title,"HABITS" to R.string.habits_title,"FOCUS" to R.string.focus_tab,
            "WORKOUTS" to R.string.movement_title,"WATER" to R.string.water_title,"SLEEP" to R.string.sleep_title,
            "FOOD" to R.string.food_title,"CARE" to R.string.care_title,"REFLECTION" to R.string.reflection_title,
            "CAPTURE" to R.string.capture_title).forEach { (key,label) -> ToggleRow(label,key in state.categories) { viewModel.category(key,it) } }
        Row {
            Checkbox(state.includeEvidence, viewModel::toggleEvidence, enabled = !state.isProcessing,
                modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp))
            Text(stringResource(R.string.export_evidence), Modifier.padding(top = 12.dp))
        }
        Text(stringResource(R.string.export_disclosure))
        ActionButton(R.string.preview_export, enabled = !state.isProcessing, onClick = viewModel::prepareMarkdown)
        Divider()
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.backup_disclosure))
        ToggleRow(R.string.backup_routes,state.includeRoutes,viewModel::routes)
        ActionButton(R.string.create_backup, enabled = !state.isProcessing, onClick = viewModel::prepareBackupScope)
        ActionButton(R.string.restore_backup, enabled = !state.isProcessing, onClick = {
            restoreLauncher.launch(arrayOf("application/octet-stream", "application/x-steady-backup", "*/*"))
        })
        Divider()
        Text(stringResource(R.string.legacy_og_title),style=MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.legacy_og_disclosure))
        ActionButton(R.string.legacy_og_choose,enabled=!state.isProcessing) { legacyLauncher.launch(arrayOf("application/json","text/plain","*/*")) }
        Divider()
        Text(stringResource(R.string.delete_disclosure))
        ActionButton(R.string.delete_local_data, enabled = !state.isProcessing, onClick = { showDeleteConfirm = true })
    }

    state.legacyPreview?.let { preview ->
        val rows = remember(preview) {
            val root = kotlinx.serialization.json.Json.parseToJsonElement(preview) as kotlinx.serialization.json.JsonObject
            root.values.flatMap { (it as kotlinx.serialization.json.JsonArray).toList() }.map { row ->
                kotlinx.serialization.json.Json { prettyPrint=true }.encodeToString(kotlinx.serialization.json.JsonElement.serializer(),row)
            }
        }
        var page by remember(preview) { mutableIntStateOf(0) }
        AlertDialog(onDismissRequest={ if(!state.isProcessing) viewModel.cancelLegacy() },
            title={ Text(stringResource(R.string.legacy_og_preview)) },
            text={ Column(Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                state.legacyCounts?.let { Text(stringResource(R.string.legacy_og_counts,it[0],it[1],it[2],it[3])) }
                Text(stringResource(R.string.legacy_og_policy,state.legacyZone)); Text(state.legacyRange)
                Text(stringResource(R.string.legacy_og_disclosure))
                Text(stringResource(R.string.legacy_og_row,page+1,rows.size)); Text(rows[page])
                ActionButton(R.string.review_previous,enabled=page>0 && !state.isProcessing) { page-- }
                ActionButton(R.string.review_next,enabled=page<rows.lastIndex && !state.isProcessing) { page++ }
            } },
            confirmButton={ ActionButton(R.string.legacy_og_apply,enabled=!state.isProcessing,onClick=viewModel::confirmLegacy) },
            dismissButton={ ActionButton(R.string.cancel,enabled=!state.isProcessing,onClick=viewModel::cancelLegacy) })
    }

    state.markdownPreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::clearPreview,
            title = { Text(stringResource(R.string.exact_preview)) },
            text = { Text(preview, Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { ActionButton(R.string.choose_destination, onClick = {
                exportLauncher.launch("${state.exportDate}.md")
            }) },
            dismissButton = { ActionButton(R.string.cancel, onClick = viewModel::clearPreview) }
        )
    }
    state.backupCounts?.let { counts -> AlertDialog(onDismissRequest=viewModel::cancelBackupScope,
        title={ Text(stringResource(R.string.backup_scope_title)) },text={ Column {
            Text(stringResource(R.string.backup_scope_counts,counts[0],counts[1],counts[2],counts[3],counts[4],counts[5]))
            Text(stringResource(R.string.backup_scope_exclusions))
        } },confirmButton={ ActionButton(R.string.continue_action) { viewModel.scopeConfirmed(); passwordMode="backup" } },
        dismissButton={ ActionButton(R.string.cancel,onClick=viewModel::cancelBackupScope) }) }
    if (passwordMode != null) {
        val creating = passwordMode == "backup"
        AlertDialog(
            onDismissRequest = { if(creating) viewModel.cancelBackupScope(); passwordMode = null; password = ""; confirmation = ""; restoreUri = null },
            title = { Text(stringResource(if (creating) R.string.create_backup else R.string.restore_backup)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.password_disclosure))
                    OutlinedTextField(password, { password = it.take(1024) },
                        label = { Text(stringResource(R.string.backup_password)) },
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                    if (creating) OutlinedTextField(confirmation, { confirmation = it.take(1024) },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                ActionButton(R.string.continue_action, enabled = password.length >= 12 && (!creating || password == confirmation), onClick = {
                    val chars = password.toCharArray()
                    if (creating) viewModel.prepareBackup(chars) else restoreUri?.let { viewModel.prepareRestore(it, chars) }
                    password = ""; confirmation = ""; restoreUri = null; passwordMode = null
                })
            },
            dismissButton = { ActionButton(R.string.cancel, onClick = {
                if(creating) viewModel.cancelBackupScope()
                password = ""; confirmation = ""; passwordMode = null; restoreUri = null
            }) }
        )
    }
    state.restoreCounts?.let { counts ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Column {
                Text(stringResource(R.string.restore_scope_summary, counts[0], counts[1], counts[2]))
                if (state.restoreRange.isNotBlank()) Text(state.restoreRange)
                Text(stringResource(if(state.legacyRestore) R.string.legacy_restore_scope else R.string.restore_replace_disclosure))
            } },
            confirmButton = { ActionButton(R.string.replace_records, enabled = !state.isProcessing, onClick = viewModel::confirmRestore) },
            dismissButton = { ActionButton(R.string.cancel, enabled = !state.isProcessing, onClick = viewModel::cancelRestore) }
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_disclosure)) },
            confirmButton = { ActionButton(R.string.delete_local_data, onClick = {
                viewModel.clearLocalData(); showDeleteConfirm = false
            }) },
            dismissButton = { ActionButton(R.string.cancel, onClick = { showDeleteConfirm = false }) })
    }
}

@Composable
fun ActionButton(label: Int, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = enabled) { Text(stringResource(label)) }
}

@Composable
fun ScreenHeading(label: Int) {
    Text(stringResource(label), style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.semantics { heading() })
}
