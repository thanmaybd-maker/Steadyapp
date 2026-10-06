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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

data class ExpandedUiState(val period: PeriodSnapshot? = null, val loading: Boolean = true,
    val busy: Boolean = false, val message: Int? = null, val error: Int? = null,
    val activeMillis: Long = 0, val scratchpad: String = "", val noteStatus: Int = R.string.saved,
    val logicalToday: LocalDate? = null, val interval: com.thanu.steady.domain.IntervalProgram? = null,
    val rest: com.thanu.steady.domain.WorkoutRest? = null, val restRemaining: Long = 0)

class ExpandedViewModel(val repository: ExpandedRepository, private val activity: ActivityRepository,
    private val preferences: PreferencesRepository, private val alarms: ActivityAlarmAdapter,
    private val notifications: NotificationAdapter, private val bootstrap: BootstrapStore,
    val platformSensors: PlatformSensors, val audioSoundscapeEngine: AudioSoundscapeEngine,
    private val isForeground: () -> Boolean, private val refreshReminders: suspend () -> Unit = {},
    private val interruptLegacy: suspend () -> Unit = {}) : ViewModel() {
    private val _state = MutableStateFlow(ExpandedUiState())
    val state = _state.asStateFlow()
    private var watching: Job? = null
    private var noteSave: Job? = null
    private var noteVersion = 0
    private var noteSession: String? = null
    private var noteLoaded = false
    private var checkpointTick = 0
    private val _drafts = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val drafts = _drafts.asStateFlow()
    private val draftJobs = mutableMapOf<String, Job>()
    private val draftVersions = mutableMapOf<String, Int>()
    private val _ambient = MutableStateFlow(AmbientPreferences())
    val ambient = _ambient.asStateFlow()
    private val _ambientSaved = MutableStateFlow(true)
    val ambientSaved = _ambientSaved.asStateFlow()
    private var ambientVersion = 0
    private var ambientSave: Job? = null
    private var ambientLoad: Job? = null
    fun ambient(transform: (AmbientPreferences) -> AmbientPreferences) {
        val next = transform(_ambient.value).also { it.validate() }
        val version = ++ambientVersion
        _ambient.value = next
        audioSoundscapeEngine.playSoundscape(next.sound)
        audioSoundscapeEngine.setVolume(next.volume)
        audioSoundscapeEngine.setModulation(next.modulation)
        _ambientSaved.value = false
        ambientSave?.cancel()
        ambientSave = viewModelScope.launch {
            delay(400)
            try {
                withContext(Dispatchers.IO) { repository.saveNote(SessionNote("ambient:preferences", null, Json.encodeToString(next), repository.clock.millis())) }
                if (version == ambientVersion) _ambientSaved.value = true
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = R.string.audio_settings_save_failed) } }
        }
    }
    fun openDraft(key: String, initial: Map<String, String>) {
        if (_drafts.value.containsKey(key)) return
        _drafts.update { it + (key to initial) }
        val version = draftVersions[key] ?: 0
        viewModelScope.launch {
            try {
                val stored = withContext(Dispatchers.IO) { repository.note("draft:$key") }
                if (stored != null && (draftVersions[key] ?: 0) == version && _drafts.value.containsKey(key)) {
                    val values = Json.decodeFromString<Map<String, String>>(stored.text)
                    _drafts.update { it + (key to values) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = R.string.draft_load_failed) } }
        }
    }
    fun field(key: String, name: String, value: String) {
        if (value.length > 100_000) return
        draftVersions[key] = (draftVersions[key] ?: 0) + 1
        val values = (_drafts.value[key] ?: emptyMap()) + (name to value)
        _drafts.update { it + (key to values) }
        draftJobs[key]?.cancel()
        draftJobs[key] = viewModelScope.launch {
            delay(400)
            try { withContext(Dispatchers.IO) { repository.saveNote(SessionNote("draft:$key", null, Json.encodeToString(values), repository.clock.millis())) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = R.string.draft_save_failed) } }
        }
    }
    fun clearDraft(key: String) {
        draftVersions[key] = (draftVersions[key] ?: 0) + 1
        draftJobs.remove(key)?.cancel()
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { repository.deleteNote("draft:$key") }; _drafts.update { it - key } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = R.string.draft_save_failed) } }
        }
    }
    private fun loadAmbient() {
        ambientLoad?.cancel()
        val version = ambientVersion
        ambientLoad = viewModelScope.launch {
            try {
                val stored = withContext(Dispatchers.IO) { repository.note("ambient:preferences") }
                if (ambientVersion == version) {
                    val settings = stored?.let { Json.decodeFromString<AmbientPreferences>(it.text).also { value -> value.validate() } }
                        ?: AmbientPreferences()
                    _ambient.value = settings
                    audioSoundscapeEngine.playSoundscape(settings.sound)
                    audioSoundscapeEngine.setVolume(settings.volume)
                    audioSoundscapeEngine.setModulation(settings.modulation)
                    _ambientSaved.value = true
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _state.update { it.copy(error = R.string.audio_settings_load_failed) } }
        }
    }
    init {
        loadAmbient()
        reload()
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!isForeground()) continue
                val session = _state.value.period?.active?.firstOrNull() ?: continue
                _state.update { it.copy(activeMillis = activity.activeMillis(session)) }
                _state.value.rest?.let { rest ->
                    val now = activity.time()
                    if(rest.boot != now.boot) {
                        withContext(Dispatchers.IO) { activity.cancelRest(session.id) }; alarms.cancelRest(session.id)
                        _state.update { it.copy(rest = null, restRemaining = 0) }
                    } else {
                        _state.update { it.copy(restRemaining = (rest.deadlineElapsed - now.elapsed).coerceAtLeast(0)) }
                        if(!rest.complete && now.elapsed >= rest.deadlineElapsed) action({
                            activity.completeRest(rest.id,rest.generation)?.let {
                                if(alarms.claim("REST", it.id, it.generation) != null) notifications.showTimerCompleteNotification("rest:${it.id}",it.cueFlags)
                            }
                            alarms.cancelRest(session.id)
                        }, success = R.string.rest_complete)
                    }
                }
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
                    if(repository.profile().zoneMode == "DEVICE") {
                        val zone = java.time.ZoneId.systemDefault().id
                        if(preferences.get().zoneId != zone) preferences.update { it.copy(zoneId = zone) }
                    }
                    interruptLegacy()
                    activity.reconcile().forEach { if (it.state == "RUNNING") alarms.schedule(it) else alarms.cancel(it.id) }
                    val today = repository.logicalDay()
                    _state.update { it.copy(logicalToday = today) }
                    repository.prepareDay(today)
                    try { refreshReminders() } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { _state.update { it.copy(error = R.string.reminder_schedule_failed) } }
                    (start ?: today) to (end ?: start ?: today)
                }
                repository.observe(range.first, range.second).collect { period ->
                    val active = period.active.firstOrNull()
                    val interval = active?.let { withContext(Dispatchers.IO) { activity.program(it.id) } }
                    val rest = active?.let { withContext(Dispatchers.IO) { activity.rest(it.id) } }
                    _state.update { it.copy(period = period, loading = false,
                        activeMillis = active?.let(activity::activeMillis) ?: 0, interval = interval, rest = rest,
                        restRemaining = rest?.let { (it.deadlineElapsed - activity.time().elapsed).coerceAtLeast(0) } ?: 0) }
                    val sessionId = period.active.firstOrNull()?.id
                    if (!noteLoaded || sessionId != noteSession) {
                        noteSave?.join()
                        if (_state.value.noteStatus == R.string.note_retry) return@collect
                        noteLoaded = true
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
            try { withContext(Dispatchers.IO) { block() }; _state.update { it.copy(message = success) }; after()
                try { withContext(Dispatchers.IO) { refreshReminders() } } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { _state.update { it.copy(error = R.string.reminder_schedule_failed) } }
            }
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
    suspend fun completeOnboarding(profile: ExpandedProfile, zone: String, boundary: Int) {
        require(boundary in 0..1439); java.time.ZoneId.of(zone)
        preferences.update { it.copy(zoneId = zone, boundaryMinutes = boundary) }
        repository.saveProfile(profile.copy(onboarded = true)); bootstrap.setCountry(profile.country)
    }
    fun updateProfile(profile: ExpandedProfile) = action({ repository.saveProfile(profile) })
    fun mode(mode: String) {
        val day = _state.value.period?.end ?: return
        action({ repository.setDay(day, mode = mode) })
    }
    fun task(item: PlanItem, after: () -> Unit) = action({ repository.saveTask(item) }, after = after)
    fun habit(version: HabitVersion, after: () -> Unit) = action({ repository.saveHabit(version) }, after = after)
    fun start(type: String, kind: String, title: String, seconds: Long?, breakSeconds: Long,
        subjectId: String? = null, taskId: String? = null, after: () -> Unit = {}) = action({
        startActivity(type, kind, title, seconds, breakSeconds, subjectId, taskId)
    }, after = after)
    suspend fun startActivity(type: String, kind: String, title: String, seconds: Long?, breakSeconds: Long,
        subjectId: String? = null, taskId: String? = null, notes: String = "", program: com.thanu.steady.domain.IntervalProgram? = null) {
        flushNote()
        val started = activity.create(type, kind, title, seconds, breakSeconds, subjectId, taskId, notes, program)
        alarms.schedule(started)
    }
    fun transition(next: ActivityState) {
        val current = _state.value.period?.active?.firstOrNull() ?: return
        action({ flushNote(); alarms.cancel(current.id); alarms.cancelRest(current.id)
            try { val fresh = activity.transition(current.id, current.generation, next); if(fresh.state == "RUNNING") alarms.schedule(fresh) }
            catch(error: Exception) {
                withContext(NonCancellable + Dispatchers.IO) { activity.active()?.let { fresh ->
                    if(fresh.state == "RUNNING") { alarms.schedule(fresh); activity.rest(fresh.id)?.let { alarms.scheduleRest(it) } }
                } }
                throw error
            }
        })
    }
    private fun complete(session: ActivitySession) = action({
        activity.complete(session.id, session.generation)?.let {
            val cue = alarms.claim("ACTIVITY", it.id, session.generation)
            alarms.cancel(it.id); if(cue != null) notifications.showTimerCompleteNotification(it.id, it.cueFlags)
            alarms.cancelRest(it.id)
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
    private suspend fun flushNote() {
        noteSave?.cancelAndJoin()
        if (noteLoaded) repository.saveNote(SessionNote("scratchpad:${noteSession ?: "general"}", noteSession,
            _state.value.scratchpad, repository.clock.millis()))
        _state.update { it.copy(noteStatus = R.string.saved) }
    }
    fun subject(value: Subject, after: () -> Unit) = action({ repository.saveSubject(value) }, after = after)
    fun water(value: WaterLog, after: () -> Unit) = action({ repository.saveWater(value) }, after = after)
    fun sleep(value: SleepLog, after: () -> Unit) = action({ repository.saveSleep(value) }, after = after)
    fun food(value: FoodIdeaRecord, after: () -> Unit) = action({ repository.saveFood(value) }, after = after)
    fun set(value: ExerciseSet, after: () -> Unit) = action({ activity.saveSet(value) }, after = after)
    suspend fun saveSet(value: ExerciseSet) = activity.saveSet(value)
    suspend fun saveTemplate(value: WorkoutTemplate) = activity.saveTemplate(value)
    suspend fun sets(id: String) = withContext(Dispatchers.IO) { activity.sets(id) }
    suspend fun templates() = withContext(Dispatchers.IO) { activity.templates() }
    fun template(value: WorkoutTemplate, after: () -> Unit) = action({ activity.saveTemplate(value) }, after = after)
    fun deleteTemplate(id: String) = action({ activity.deleteTemplate(id) })
    fun effort(id: String, value: Int?) = action({ activity.effort(id,value) })
    fun rest(sessionId: String, seconds: Int) = action({ beginRest(sessionId,seconds) })
    suspend fun beginRest(sessionId: String, seconds: Int) {
        alarms.cancelRest(sessionId)
        try { alarms.scheduleRest(activity.startRest(sessionId,seconds)) }
        catch(error: Exception) {
            withContext(NonCancellable + Dispatchers.IO) { activity.rest(sessionId)?.let { alarms.scheduleRest(it) } }
            throw error
        }
    }
    fun cancelRest(sessionId: String) = action({
        alarms.cancelRest(sessionId)
        try { activity.cancelRest(sessionId) }
        catch(error: Exception) {
            withContext(NonCancellable + Dispatchers.IO) { activity.rest(sessionId)?.let { alarms.scheduleRest(it) } }
            throw error
        }
    })
    fun deleteSet(id: String) = action({ activity.deleteSet(id) })
    suspend fun manualWorkout(kind: String, title: String, actualMinutes: Double, notes: String) = activity.manualWorkout(kind, title, actualMinutes, notes)
    suspend fun correctHistory(id: String, millis: Long, notes: String, effort: Int?) = activity.editHistory(id, millis, notes, effort)
    suspend fun setTimePolicy(zone: String, boundary: Int) {
        java.time.ZoneId.of(zone); require(boundary in 0..1439)
        preferences.update { it.copy(zoneId = zone, boundaryMinutes = boundary) }
    }
    suspend fun setFoodPreferences(vegetarian: Boolean, avoid: String) = preferences.update { it.copy(vegetarian = vegetarian, avoidFoods = avoid) }
    fun editHistory(id: String, millis: Long, note: String, effort: Int?, after: () -> Unit) = action({ activity.editHistory(id, millis, note, effort) }, after = after)
    fun deleteHistory(id: String) = action({ activity.deleteHistory(id) })
    fun afterRecovery() {
        noteSave?.cancel(); draftJobs.values.forEach { it.cancel() }; draftJobs.clear()
        ambientVersion++; ambientSave?.cancel(); audioSoundscapeEngine.stop()
        loadAmbient()
        _drafts.value = emptyMap(); noteLoaded = false; noteSession = null
        _state.update { it.copy(scratchpad = "",noteStatus = R.string.saved) }
        reload()
    }
    override fun onCleared() {
        audioSoundscapeEngine.release()
        platformSensors.stopStepTracking()
        platformSensors.stopGpsTracking()
        super.onCleared()
    }
}
