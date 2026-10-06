package com.thanu.steady.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExpandedLearnBuildModules() {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Learn & Build Modules", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Active Project: Pending implementation", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ExpandedPeople() {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("People & Relationships", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Manage connection reminders and contact notes here.")
    }
}

@Composable
fun ExpandedMoneyGuard() {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Money Guard", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Budget tracking pending implementation.")
    }
}

@Composable
fun ExpandedVoiceOcr() {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Voice & OCR Capture", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Offline text recognition pending implementation.")
    }
}
