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
import com.thanu.steady.domain.ThirtyDaySequence

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: ReviewViewModel) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Week Ending: ${state.weekEnd}") },
                actions = {
                    Button(onClick = { viewModel.toggleThirtyDay() }) {
                        Text("30-Day")
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
            if (state.showThirtyDay) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("30-Day Sequence", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        ThirtyDaySequence.periods.forEach { period ->
                            Text(period, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
                Divider()
            }

            Text("Daily Plans in Window", style = MaterialTheme.typography.titleLarge)
            if (state.plansInWindow.isEmpty()) {
                Text("Not recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.plansInWindow.forEach { plan ->
                    Text("${plan.logicalDay}: Health: ${plan.healthTask.ifEmpty { "None" }} | Study: ${plan.studyTask.ifEmpty { "None" }}")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Optional Indicators", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.review.indicatorSleep,
                onValueChange = { viewModel.updateReviewField("sleep", it) },
                label = { Text("Sleep") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Sleep indicator" }
            )
            OutlinedTextField(
                value = state.review.indicatorLearning,
                onValueChange = { viewModel.updateReviewField("learning", it) },
                label = { Text("Learning") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Learning indicator" }
            )
            OutlinedTextField(
                value = state.review.indicatorBuilding,
                onValueChange = { viewModel.updateReviewField("building", it) },
                label = { Text("Building") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Building indicator" }
            )
            OutlinedTextField(
                value = state.review.indicatorHealth,
                onValueChange = { viewModel.updateReviewField("health", it) },
                label = { Text("Health/Movement") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Health indicator" }
            )
            OutlinedTextField(
                value = state.review.indicatorConnection,
                onValueChange = { viewModel.updateReviewField("connection", it) },
                label = { Text("Connection") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Connection indicator" }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Review Questions", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.review.helped,
                onValueChange = { viewModel.updateReviewField("helped", it) },
                label = { Text("What helped?") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "What helped question" },
                minLines = 2
            )
            OutlinedTextField(
                value = state.review.tooDemanding,
                onValueChange = { viewModel.updateReviewField("tooDemanding", it) },
                label = { Text("What was too demanding?") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "What was too demanding question" },
                minLines = 2
            )
            OutlinedTextField(
                value = state.review.changedEvidence,
                onValueChange = { viewModel.updateReviewField("changedEvidence", it) },
                label = { Text("What evidence changed?") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "What evidence changed question" },
                minLines = 2
            )
            OutlinedTextField(
                value = state.review.adjustment,
                onValueChange = { viewModel.updateReviewField("adjustment", it) },
                label = { Text("One adjustment for next week") },
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "One adjustment question" },
                minLines = 2
            )

            Button(
                onClick = { viewModel.saveReview() },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)
            ) {
                Text(if (state.isSaved) "Saved" else "Save Review")
            }
        }
    }
}
