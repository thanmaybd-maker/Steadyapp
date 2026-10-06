package com.thanu.steady.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showFoodSheet by remember { mutableStateOf(false) }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today: ${state.logicalDay}") },
                actions = {
                    Button(onClick = { viewModel.togglePause() }, modifier = Modifier.padding(end = 8.dp)) {
                        Text(if (state.isPaused) "Resume" else "Pause")
                    }
                    Button(onClick = { showFoodSheet = true }) {
                        Text("Food")
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
            if (state.errorMessage != null) {
                Text(text = state.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            if (state.isSaved) {
                Text(text = "Saved", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Minimum Day")
                Switch(
                    checked = state.mode == com.thanu.steady.domain.DayMode.MINIMUM,
                    onCheckedChange = { viewModel.toggleMode() },
                    modifier = Modifier.semantics { contentDescription = "Toggle minimum day mode" }
                )
            }

            OutlinedTextField(
                value = state.healthTask,
                onValueChange = { viewModel.updateTask("health", it) },
                label = { Text("Health (10 min movement/rest)") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Health priority input" }
            )

            OutlinedTextField(
                value = state.studyTask,
                onValueChange = { viewModel.updateTask("study", it) },
                label = { Text("Study (10 min review)") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Study priority input" }
            )

            OutlinedTextField(
                value = state.buildTask,
                onValueChange = { viewModel.updateTask("build", it) },
                label = { Text("Build (10 min code)") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Build priority input" }
            )

            OutlinedTextField(
                value = state.nextAction,
                onValueChange = { viewModel.updateTask("nextAction", it) },
                label = { Text("Next Action") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Next action input" }
            )

            OutlinedTextField(
                value = state.evidence,
                onValueChange = { viewModel.updateTask("evidence", it) },
                label = { Text("Optional Evidence") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Optional evidence input" },
                minLines = 3
            )

            Button(
                onClick = { viewModel.savePlan() },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save Plan")
            }
        }
    }

    if (showFoodSheet) {
        FoodIdeasSheet(onDismiss = { showFoodSheet = false })
    }
}
