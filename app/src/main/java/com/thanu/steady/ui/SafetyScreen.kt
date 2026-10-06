package com.thanu.steady.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thanu.steady.domain.PublicHelp
import com.thanu.steady.platform.DialerAdapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(viewModel: SafetyViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dialer = remember { DialerAdapter(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety") },
                actions = {
                    Button(onClick = { viewModel.toggleEdit() }, modifier = Modifier.padding(end = 8.dp)) {
                        Text(if (state.isEditing) "Cancel" else "Edit Private Plan")
                    }
                    if (state.isEditing) {
                        Button(onClick = { viewModel.savePlan() }, modifier = Modifier.padding(end = 8.dp)) {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Public Help Surface
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Public Help",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        "Verified: ${PublicHelp.verificationDate}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    PublicHelp.numbers.forEach { (name, number) ->
                        Button(
                            onClick = { dialer.openDialer(number) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).semantics { contentDescription = "Call $name" },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Call $name: $number")
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            if (state.errorMessage != null) {
                Text(text = state.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Personal Plan", style = MaterialTheme.typography.titleLarge)

                if (state.isEditing) {
                    OutlinedTextField(
                        value = state.plan.warningSigns,
                        onValueChange = { viewModel.updatePlan("warningSigns", it) },
                        label = { Text("Warning Signs") },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Edit Warning Signs" },
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = state.plan.copingSteps,
                        onValueChange = { viewModel.updatePlan("copingSteps", it) },
                        label = { Text("Coping Steps") },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Edit Coping Steps" },
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = state.plan.safePeoplePlaces,
                        onValueChange = { viewModel.updatePlan("safePeoplePlaces", it) },
                        label = { Text("Safe People & Places") },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Edit Safe People & Places" },
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = state.plan.environmentSteps,
                        onValueChange = { viewModel.updatePlan("environmentSteps", it) },
                        label = { Text("Environment Steps") },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Edit Environment Steps" },
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = state.plan.clinicName,
                        onValueChange = { viewModel.updatePlan("clinicName", it) },
                        label = { Text("Clinic Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.plan.clinicPhone,
                        onValueChange = { viewModel.updatePlan("clinicPhone", it) },
                        label = { Text("Clinic Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Warning Signs:\n${state.plan.warningSigns.ifEmpty { "None recorded" }}")
                    Text("Coping Steps:\n${state.plan.copingSteps.ifEmpty { "None recorded" }}")
                    Text("Safe People/Places:\n${state.plan.safePeoplePlaces.ifEmpty { "None recorded" }}")
                    Text("Environment Steps:\n${state.plan.environmentSteps.ifEmpty { "None recorded" }}")
                    Text("Clinic: ${state.plan.clinicName.ifEmpty { "N/A" }} (${state.plan.clinicPhone.ifEmpty { "N/A" }})")
                    
                    if (state.plan.clinicPhone.isNotEmpty()) {
                        Button(onClick = { dialer.openDialer(state.plan.clinicPhone) }) {
                            Text("Call Clinic")
                        }
                    }
                }
            }
        }
    }
}
