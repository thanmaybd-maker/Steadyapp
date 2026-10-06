package com.thanu.steady.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.R
import com.thanu.steady.data.PreferencesRepository
import com.thanu.steady.data.TimerRepository
import com.thanu.steady.domain.*
import com.thanu.steady.platform.AlarmAdapter
import com.thanu.steady.platform.NotificationAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BreakUiState(
    val session: TimerSession = TimerSession(durationMs = 25 * 60_000L, remainingMs = 25 * 60_000L),
    val displayRemainingMs: Long = 25 * 60_000L,
    val isDimmed: Boolean = false,
    val busy: Boolean = true,
    val error: Int? = null
)

class BreakViewModel(private val repository: TimerRepository, private val alarms: AlarmAdapter,
    private val notifications: NotificationAdapter, private val preferences: PreferencesRepository) : ViewModel() {
    private val machine = TimerStateMachine()
    private val _uiState = MutableStateFlow(BreakUiState())
    val uiState = _uiState.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                repository.reconcile().forEach { if (it.state != TimerState.RUNNING) alarms.cancelAlarm(it.id)
                    else it.targetElapsedTime?.let { deadline -> alarms.scheduleExactAlarm(deadline, it.id, it.generation) } }
                repository.observe().collect { timer ->
                    if (timer != null) _uiState.update { it.copy(session = timer, busy = false,
                        displayRemainingMs = remaining(timer), error = null) }
                    else _uiState.update { it.copy(busy = false) }
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(busy = false, error = R.string.storage_unavailable) } }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val timer = _uiState.value.session
                if (timer.state == TimerState.RUNNING && !_uiState.value.busy) {
                    _uiState.update { it.copy(displayRemainingMs = remaining(timer)) }
                    if (remaining(timer) == 0L) finish(timer)
                }
            }
        }
    }
    private fun remaining(timer: TimerSession) = if (timer.state == TimerState.RUNNING)
        ((timer.targetElapsedTime ?: repository.time().elapsed) - repository.time().elapsed).coerceAtLeast(0) else timer.remainingMs
    fun setDuration(minutes: Long) {
        if (_uiState.value.busy || _uiState.value.session.state == TimerState.RUNNING) return
        require(minutes in 1..1440)
        val ms = minutes * 60_000
        _uiState.update { it.copy(session = TimerSession(durationMs = ms, remainingMs = ms), displayRemainingMs = ms) }
    }
    fun start() = operation {
        val old = _uiState.value.session
        val now = repository.time()
        val next = machine.start(old.copy(cueFlags = preferences.get().cueFlags), now.wall, now.elapsed, now.boot)
        repository.transition(old, next)
        alarms.scheduleExactAlarm(next.targetElapsedTime!!, next.id, next.generation)
    }
    fun pause() = operation {
        val old = _uiState.value.session
        repository.transition(old, machine.pause(old, repository.time().elapsed))
        alarms.cancelAlarm(old.id)
    }
    fun stop() = operation {
        val old = _uiState.value.session
        repository.transition(old, machine.stop(old))
        alarms.cancelAlarm(old.id)
    }
    private fun finish(timer: TimerSession) = operation {
        repository.complete(timer.id, timer.generation)?.let {
            alarms.cancelAlarm(it.id)
            notifications.showTimerCompleteNotification(it.id, it.cueFlags)
        }
    }
    fun toggleDim() { _uiState.update { it.copy(isDimmed = !it.isDimmed) } }
    private fun operation(block: suspend () -> Unit) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { block() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(error = R.string.timer_save_failed) } }
            finally { _uiState.update { it.copy(busy = false) } }
        }
    }
}
