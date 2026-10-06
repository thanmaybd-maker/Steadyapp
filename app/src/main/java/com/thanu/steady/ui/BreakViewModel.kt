package com.thanu.steady.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.data.SteadyDatabase
import com.thanu.steady.data.TimerSessionEntity
import com.thanu.steady.domain.TimerSession
import com.thanu.steady.domain.TimerState
import com.thanu.steady.domain.TimerStateMachine
import com.thanu.steady.platform.AlarmAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class BreakUiState(
    val session: TimerSession = TimerSession(durationMs = 25 * 60 * 1000, remainingMs = 25 * 60 * 1000),
    val displayRemainingMs: Long = 25 * 60 * 1000,
    val isDimmed: Boolean = false,
    val permissionDenied: Boolean = false
)

class BreakViewModel(
    private val databaseProvider: () -> SteadyDatabase,
    private val alarmAdapter: AlarmAdapter
) : ViewModel() {
    private val database get() = databaseProvider()

    private val stateMachine = TimerStateMachine()
    private val _uiState = MutableStateFlow(BreakUiState())
    val uiState: StateFlow<BreakUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val latest = database.timerDao().getLatestSession()
                if (latest != null) {
                    val session = TimerSession(
                        id = latest.id,
                        logicalDay = latest.logicalDay,
                        kind = latest.kind,
                        durationMs = latest.durationMs,
                        remainingMs = latest.remainingMs,
                        state = latest.state,
                        startedAt = latest.startedAt,
                        targetWallTime = latest.targetWallTime,
                        targetElapsedTime = latest.targetElapsedTime,
                        bootMarker = latest.bootMarker,
                        cueFlags = latest.cueFlags,
                        completedAt = latest.completedAt,
                        generation = latest.generation
                    )
                    _uiState.update { it.copy(session = session, displayRemainingMs = session.remainingMs) }
                }
            } catch (e: Exception) {
                // Ignore DB error for UI fallback
            }
        }
        startTicker()
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value.session
                if (current.state == TimerState.RUNNING && current.targetElapsedTime != null) {
                    val remaining = current.targetElapsedTime - SystemClock.elapsedRealtime()
                    if (remaining <= 0) {
                        completeSession()
                    } else {
                        _uiState.update { it.copy(displayRemainingMs = remaining) }
                    }
                }
            }
        }
    }

    fun setDuration(minutes: Long) {
        if (_uiState.value.session.state == TimerState.RUNNING) return
        val ms = minutes * 60 * 1000
        val updated = _uiState.value.session.copy(durationMs = ms, remainingMs = ms, state = TimerState.IDLE)
        _uiState.update { it.copy(session = updated, displayRemainingMs = ms) }
    }

    fun start() {
        val current = _uiState.value.session
        val nowWall = Instant.now()
        val nowElapsed = SystemClock.elapsedRealtime()
        // Boot count requires API 24+, fallback to 0 for stub
        val bootMarker = 0L 
        
        val newSession = stateMachine.start(current, nowWall, nowElapsed, bootMarker)
        saveAndSchedule(newSession)
    }

    fun pause() {
        val current = _uiState.value.session
        val newSession = stateMachine.pause(current, SystemClock.elapsedRealtime())
        saveAndSchedule(newSession)
    }

    fun stop() {
        val current = _uiState.value.session
        val newSession = stateMachine.stop(current)
        saveAndSchedule(newSession)
    }

    private fun completeSession() {
        val current = _uiState.value.session
        val newSession = stateMachine.complete(current, Instant.now())
        saveAndSchedule(newSession)
    }

    fun toggleDim() {
        _uiState.update { it.copy(isDimmed = !it.isDimmed) }
    }

    private fun saveAndSchedule(session: TimerSession) {
        _uiState.update { it.copy(session = session, displayRemainingMs = session.remainingMs) }
        viewModelScope.launch {
            try {
                val entity = TimerSessionEntity(
                    id = session.id,
                    logicalDay = session.logicalDay,
                    kind = session.kind,
                    durationMs = session.durationMs,
                    remainingMs = session.remainingMs,
                    state = session.state,
                    startedAt = session.startedAt,
                    targetWallTime = session.targetWallTime,
                    targetElapsedTime = session.targetElapsedTime,
                    bootMarker = session.bootMarker,
                    cueFlags = session.cueFlags,
                    completedAt = session.completedAt,
                    generation = session.generation
                )
                database.timerDao().insertSession(entity)
                
                if (session.state == TimerState.RUNNING && session.targetElapsedTime != null) {
                    alarmAdapter.scheduleExactAlarm(session.targetElapsedTime, session.id, session.generation)
                } else if (session.state != TimerState.COMPLETED) {
                    // Do not cancel if COMPLETED, let the AlarmReceiver fire the notification
                    alarmAdapter.cancelAlarm(session.id)
                }
            } catch (e: Exception) {
                // Ignore save errors for diagnostic purposes
            }
        }
    }
}
