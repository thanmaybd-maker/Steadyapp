package com.thanu.steady.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thanu.steady.domain.TimerState

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

@Composable
fun BreakScreen(viewModel: BreakViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (!isGranted) {
                // permission denied, handled by UI state below
            }
        }
    )
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    val backgroundColor = if (state.isDimmed) Color.Black else MaterialTheme.colorScheme.background
    // Ensure at least 7:1 contrast ratio against black for low vision accessibility
    val contentColor = if (state.isDimmed) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val minutes = state.displayRemainingMs / 60000
            val seconds = (state.displayRemainingMs % 60000) / 1000
            val timeText = String.format("%02d:%02d", minutes, seconds)
            
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge,
                color = contentColor,
                modifier = Modifier.semantics { contentDescription = "Time remaining: $minutes minutes and $seconds seconds" }
            )

            Text(
                text = "State: ${state.session.state.name}",
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                modifier = Modifier.semantics { contentDescription = "Current state: ${state.session.state.name}" }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.setDuration(5) },
                    enabled = state.session.state != TimerState.RUNNING,
                    modifier = Modifier.semantics { contentDescription = "Preset 5 minutes" }
                ) {
                    Text("5m")
                }
                Button(
                    onClick = { viewModel.setDuration(25) },
                    enabled = state.session.state != TimerState.RUNNING,
                    modifier = Modifier.semantics { contentDescription = "Preset 25 minutes" }
                ) {
                    Text("25m")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.session.state == TimerState.RUNNING) {
                    Button(
                        onClick = { viewModel.pause() },
                        modifier = Modifier.size(width = 120.dp, height = 80.dp).semantics { contentDescription = "Pause timer" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Pause", style = MaterialTheme.typography.titleLarge)
                    }
                } else {
                    Button(
                        onClick = { viewModel.start() },
                        modifier = Modifier.size(width = 120.dp, height = 80.dp).semantics { contentDescription = "Start timer" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (state.session.state == TimerState.PAUSED) "Resume" else "Start", style = MaterialTheme.typography.titleLarge)
                    }
                }

                Button(
                    onClick = { viewModel.stop() },
                    modifier = Modifier.size(width = 120.dp, height = 80.dp).semantics { contentDescription = "Stop and reset timer" },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop", style = MaterialTheme.typography.titleLarge)
                }
            }

            OutlinedButton(
                onClick = { viewModel.toggleDim() },
                modifier = Modifier.semantics { contentDescription = "Toggle dim screen mode" }
            ) {
                Text(if (state.isDimmed) "Restore Brightness" else "Dim Screen (Eyes Closed)")
            }
            
            val isPermissionDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
            if (isPermissionDenied) {
                Text(
                    text = "Notification permissions denied. Alarm will only play locally while app is open.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
