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
fun SettingsScreen(viewModel: SettingsViewModel, onImportCompleted: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    var passwordMode by remember { mutableStateOf<String?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
        state.statusMessage?.let {
            Text(stringResource(it), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (state.isProcessing) {
            CircularProgressIndicator()
            Text(stringResource(R.string.processing))
        }
        Text(stringResource(R.string.markdown_title), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(state.exportDate, viewModel::setExportDate,
            label = { Text(stringResource(R.string.export_date)) }, modifier = Modifier.fillMaxWidth(),
            enabled = !state.isProcessing, singleLine = true)
        Row {
            Checkbox(state.includeSensitiveExport, viewModel::toggleSensitiveExport,
                enabled = !state.isProcessing, modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp))
            Text(stringResource(R.string.export_sensitive), Modifier.padding(top = 12.dp))
        }
        Row {
            Checkbox(state.includeEvidence, viewModel::toggleEvidence, enabled = !state.isProcessing,
                modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp))
            Text(stringResource(R.string.export_evidence), Modifier.padding(top = 12.dp))
        }
        Row {
            Checkbox(state.includeIndicators, viewModel::toggleIndicators, enabled = !state.isProcessing,
                modifier = Modifier.sizeIn(minWidth = 56.dp, minHeight = 56.dp))
            Text(stringResource(R.string.export_indicators), Modifier.padding(top = 12.dp))
        }
        Text(stringResource(R.string.export_disclosure))
        ActionButton(R.string.preview_export, enabled = !state.isProcessing, onClick = viewModel::prepareMarkdown)
        Divider()
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.backup_disclosure))
        ActionButton(R.string.create_backup, enabled = !state.isProcessing, onClick = { passwordMode = "backup" })
        ActionButton(R.string.restore_backup, enabled = !state.isProcessing, onClick = {
            restoreLauncher.launch(arrayOf("application/octet-stream", "application/x-steady-backup", "*/*"))
        })
        Divider()
        Text(stringResource(R.string.delete_disclosure))
        ActionButton(R.string.delete_local_data, enabled = !state.isProcessing, onClick = { showDeleteConfirm = true })
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
    if (passwordMode != null) {
        val creating = passwordMode == "backup"
        AlertDialog(
            onDismissRequest = { passwordMode = null; password = ""; confirmation = ""; restoreUri = null },
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
                password = ""; confirmation = ""; passwordMode = null; restoreUri = null
            }) }
        )
    }
    state.restoreCounts?.let { counts ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Column {
                Text(stringResource(R.string.restore_counts, counts[0], counts[1], counts[2]))
                if (state.restoreRange.isNotBlank()) Text(state.restoreRange)
                Text(stringResource(R.string.restore_replace_disclosure))
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
