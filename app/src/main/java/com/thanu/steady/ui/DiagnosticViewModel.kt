package com.thanu.steady.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.data.SteadyDatabase
import com.thanu.steady.data.DailyPlanEntity
import com.thanu.steady.domain.DailyPlan
import com.thanu.steady.domain.DayMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DiagnosticViewModel(private val database: SteadyDatabase) : ViewModel() {
    private val _status = MutableStateFlow("Initializing...")
    val status: StateFlow<String> = _status.asStateFlow()

    fun runDiagnostic() {
        viewModelScope.launch {
            try {
                val dao = database.dailyPlanDao()
                val today = LocalDate.now()
                val syntheticPlan = DailyPlanEntity.fromDomain(
                    DailyPlan(
                        logicalDay = today,
                        zoneId = ZoneId.of("Asia/Kolkata"),
                        boundaryMinutes = 240,
                        mode = DayMode.STANDARD,
                        healthTask = "Synthetic Health Task (Canary: CANARY_PLAINTEXT_SECRET)",
                        studyTask = "Synthetic Study",
                        buildTask = "Synthetic Build",
                        nextAction = "Do nothing",
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                )
                
                dao.insertPlan(syntheticPlan)
                val retrieved = dao.getPlan(today)
                
                if (retrieved != null && retrieved.healthTask.contains("CANARY")) {
                    _status.value = "Success: Wrote and retrieved synthetic canary record from encrypted DB."
                } else {
                    _status.value = "Error: Could not retrieve valid canary."
                }
            } catch (e: Exception) {
                _status.value = "Error: ${e.message}"
            }
        }
    }
}
