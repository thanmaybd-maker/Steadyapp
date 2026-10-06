package com.thanu.steady.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.ActivityState
import com.thanu.steady.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class ExpandedUiState(val period: PeriodSnapshot? = null, val loading: Boolean = true,
    val busy: Boolean = false, val message: Int? = null, val error: Int? = null,
    val activeMillis: Long = 0, val scratchpad: String = "", val noteStatus: Int = R.string.saved)

class ExpandedViewModel(val repository: ExpandedRepository, private val activity: ActivityRepository,
    private val preferences: PreferencesRepository, private val alarms: ActivityAlarmAdapter,
    private val notifications: NotificationAdapter, private val bootstrap: BootstrapStore,
    private val isForeground: () -> Boolean) : ViewModel() {
    private val _state = MutableStateFlow(ExpandedUiState())
    val state = _state.asStateFlow()
    private var watching: Job? = null
    private var noteSave: Job? = null
    private var noteVersion = 0
    private var noteSession: String? = null
    private var checkpointTick = 0
    init {
        reload()
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!isForeground()) continue
                val session = _state.value.period?.active?.firstOrNull() ?: continue
                _state.update { it.copy(activeMillis = activity.activeMillis(session)) }
                if (session.state == "RUNNING" && session.deadlineElapsed != null && activity.time().elapsed >= session.deadlineElapsed)
                    complete(session)
                if (++checkpointTick % 30 == 0 && session.state == "RUNNING") {
                    try { withContext(Dispatchers.IO) { activity.checkpoint(session.id, session.generation) } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { _state.update { it.copy(error = R.string.checkpoint_failed) } }
                }
            }
        }
    }
    fun reload(start: LocalDate? = null, end: LocalDate? = null) {
        watching?.cancel()
        _state.update { it.copy(loading = true, error = null) }
        watching = viewModelScope.launch {
            try {
                val range = withContext(Dispatchers.IO) {
                    activity.reconcile().forEach { if (it.state == "RUNNING") alarms.schedule(it) else alarms.cancel(it.id) }
                    val today = repository.logicalDay()
                    repository.prepareDay(today)
                    (start ?: today) to (end ?: start ?: today)
                }
                repository.observe(range.first, range.second).collect { period ->
                    _state.update { it.copy(period = period, loading = false,
                        activeMillis = period.active.firstOrNull()?.let(activity::activeMillis) ?: 0) }
                    val sessionId = period.active.firstOrNull()?.id
                    if (sessionId != noteSession) {
                        noteSession = sessionId
                        val note = withContext(Dispatchers.IO) { repository.note("scratchpad:${sessionId ?: "general"}") }
                        // A pending draft is never replaced by an observer refresh.
                        if (noteSave?.isActive != true) _state.update { it.copy(scratchpad = note?.text ?: "", noteStatus = R.string.saved) }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(loading = false, error = R.string.storage_unavailable) } }
        }
    }
    fun action(block: suspend () -> Unit, success: Int? = R.string.saved, after: () -> Unit = {}) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, message = null) }
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { block() }; _state.update { it.copy(message = success) }; after() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = R.string.action_failed) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun onboard(profile: ExpandedProfile, zone: String, boundary: Int) = action({
        require(boundary in 0..1439); java.time.ZoneId.of(zone)
        preferences.update { it.copy(zoneId = zone, boundaryMinutes = boundary) }
        repository.saveProfile(profile.copy(onboarded = true))
        bootstrap.setCountry(profile.country)
    }, after = { reload() })
    fun updateProfile(profile: ExpandedProfile) = action({ repository.saveProfile(profile) })
    fun mode(mode: String) {
        val day = _state.value.period?.end ?: return
        action({ repository.setDay(day, mode = mode) })
    }
    fun task(item: PlanItem, after: () -> Unit) = action({ repository.saveTask(item) }, after = after)
    fun habit(version: HabitVersion, after: () -> Unit) = action({ repository.saveHabit(version) }, after = after)
    fun start(type: String, kind: String, title: String, seconds: Long?, breakSeconds: Long,
        subjectId: String? = null, taskId: String? = null, after: () -> Unit = {}) = action({
        val started = activity.create(type, kind, title, seconds, breakSeconds, subjectId, taskId)
        alarms.schedule(started)
    }, after = after)
    fun transition(next: ActivityState) {
        val current = _state.value.period?.active?.firstOrNull() ?: return
        action({ val fresh = activity.transition(current.id, current.generation, next)
            alarms.cancel(current.id); if (fresh.state == "RUNNING") alarms.schedule(fresh) })
    }
    private fun complete(session: ActivitySession) = action({
        activity.complete(session.id, session.generation)?.let {
            alarms.cancel(it.id); notifications.showTimerCompleteNotification(it.id, it.cueFlags)
        }
    }, success = R.string.timer_complete)
    fun scratchpad(value: String) {
        if (value.length > 100_000) return
        val version = ++noteVersion
        val sessionId = noteSession
        _state.update { it.copy(scratchpad = value, noteStatus = R.string.saving) }
        noteSave?.cancel()
        noteSave = viewModelScope.launch {
            delay(400)
            try {
                withContext(Dispatchers.IO) { repository.saveNote(SessionNote("scratchpad:${sessionId ?: "general"}", sessionId, value, repository.clock.millis())) }
                if (version == noteVersion) _state.update { it.copy(noteStatus = R.string.saved) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (version == noteVersion) _state.update { it.copy(noteStatus = R.string.note_retry) } }
        }
    }
    fun retryNote() = scratchpad(_state.value.scratchpad)
    fun subject(value: Subject, after: () -> Unit) = action({ repository.saveSubject(value) }, after = after)
    fun water(value: WaterLog, after: () -> Unit) = action({ repository.saveWater(value) }, after = after)
    fun sleep(value: SleepLog, after: () -> Unit) = action({ repository.saveSleep(value) }, after = after)
    fun food(value: FoodIdeaRecord, after: () -> Unit) = action({ repository.saveFood(value) }, after = after)
    fun set(value: ExerciseSet, after: () -> Unit) = action({ activity.saveSet(value) }, after = after)
    suspend fun sets(id: String) = withContext(Dispatchers.IO) { activity.sets(id) }
    fun editHistory(id: String, millis: Long, note: String, effort: Int?, after: () -> Unit) = action({ activity.editHistory(id, millis, note, effort) }, after = after)
    fun deleteHistory(id: String) = action({ activity.deleteHistory(id) })
}
