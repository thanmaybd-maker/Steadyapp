package com.thanu.steady

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.thanu.steady.ui.SteadyAppNavigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.thanu.steady.di.AppContainer
import com.thanu.steady.ui.DiagnosticViewModel

class MainActivity : ComponentActivity() {
    private lateinit var appContainer: AppContainer
    private lateinit var diagnosticViewModel: DiagnosticViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        appContainer = AppContainer(this)
        diagnosticViewModel = DiagnosticViewModel(appContainer.database)
        diagnosticViewModel.runDiagnostic()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SteadyAppNavigation(appContainer = appContainer)
                }
            }
        }
    }
}
