package com.thanu.steady.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings & Privacy") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (state.statusMessage != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(state.statusMessage!!, modifier = Modifier.padding(16.dp))
                }
            }

            if (state.isProcessing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            // Export Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Markdown Export", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.includeSensitiveExport,
                        onCheckedChange = { viewModel.toggleSensitiveExport(it) },
                        modifier = Modifier.semantics { contentDescription = "Include sensitive fields in export" }
                    )
                    Text("Include sensitive fields (Health, Sleep, Journals)")
                }
                Text(
                    "Safety plan is always excluded. Remember that standard Markdown is not encrypted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { /* In real app: launch SAF ACTION_CREATE_DOCUMENT */ },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Select export destination" }
                ) {
                    Text("Select Destination & Export Today")
                }
            }
            
            Divider()

            // Backup Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Portable Backup", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Creates an encrypted archive of your plans, reviews, and timers. Safety plan and device settings are excluded.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { /* Launch SAF to save .steady_backup file */ },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Create encrypted backup" }
                ) {
                    Text("Create Encrypted Backup")
                }
                OutlinedButton(
                    onClick = { /* Launch SAF to open .steady_backup file */ },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Restore from backup" }
                ) {
                    Text("Restore Backup")
                }
            }

            Divider()

            // Danger Zone
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Danger Zone", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                Text(
                    "This permanently wipes all local plans, contacts, safety records, and database keys from this device. Exported Markdown and provider backups are not affected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Delete all local data" },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Local Data")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Wipe everything?") },
            text = { Text("This deletes all records and encryption keys immediately. You cannot undo this.") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.clearLocalData()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
