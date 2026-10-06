package com.thanu.steady.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.data.SteadyDatabase
import com.thanu.steady.data.DailyPlanEntity
import com.thanu.steady.domain.DailyPlan
import com.thanu.steady.domain.DayMode
import com.thanu.steady.domain.LogicalDayPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class TodayUiState(
    val isLoading: Boolean = true,
    val logicalDay: LocalDate = LocalDate.now(),
    val mode: DayMode = DayMode.STANDARD,
    val isPaused: Boolean = false,
    val healthTask: String = "",
    val studyTask: String = "",
    val buildTask: String = "",
    val nextAction: String = "",
    val evidence: String = "",
    val errorMessage: String? = null,
    val isSaved: Boolean = false
)

class TodayViewModel(private val databaseProvider: () -> SteadyDatabase) : ViewModel() {
    private val database get() = databaseProvider()
    private val policy = LogicalDayPolicy()
    
    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        loadTodayPlan()
    }

    private fun loadTodayPlan() {
        viewModelScope.launch {
            try {
                val today = policy.getLogicalDay(Instant.now())
                val entity = database.dailyPlanDao().getPlan(today)
                
                if (entity != null) {
                    val plan = entity.toDomain()
                    _uiState.update { it.copy(
                        isLoading = false,
                        logicalDay = plan.logicalDay,
                        mode = plan.mode,
                        healthTask = plan.healthTask,
                        studyTask = plan.studyTask,
                        buildTask = plan.buildTask,
                        nextAction = plan.nextAction,
                        evidence = plan.studyEvidence ?: ""
                    )}
                } else {
                    _uiState.update { it.copy(isLoading = false, logicalDay = today) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load plan") }
            }
        }
    }

    fun updateTask(field: String, value: String) {
        _uiState.update { state ->
            when (field) {
                "health" -> state.copy(healthTask = value, isSaved = false)
                "study" -> state.copy(studyTask = value, isSaved = false)
                "build" -> state.copy(buildTask = value, isSaved = false)
                "nextAction" -> state.copy(nextAction = value, isSaved = false)
                "evidence" -> state.copy(evidence = value, isSaved = false)
                else -> state
            }
        }
    }

    fun toggleMode() {
        _uiState.update { 
            val newMode = if (it.mode == DayMode.STANDARD) DayMode.MINIMUM else DayMode.STANDARD
            it.copy(mode = newMode, isSaved = false)
        }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun savePlan() {
        viewModelScope.launch {
            val state = _uiState.value
            val plan = DailyPlan(
                logicalDay = state.logicalDay,
                zoneId = ZoneId.of("Asia/Kolkata"),
                boundaryMinutes = 240,
                mode = state.mode,
                healthTask = state.healthTask,
                studyTask = state.studyTask,
                buildTask = state.buildTask,
                nextAction = state.nextAction,
                studyEvidence = state.evidence.takeIf { it.isNotBlank() },
                buildEvidence = null,
                shutdownAt = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            try {
                database.dailyPlanDao().insertPlan(DailyPlanEntity.fromDomain(plan))
                _uiState.update { it.copy(isSaved = true, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Save failed: ${e.message}") }
            }
        }
    }
}
