package com.thanu.steady.data

import androidx.room.withTransaction
import com.thanu.steady.domain.*
import java.time.ZoneId
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ActivityRepository(private val provider: () -> SteadyDatabase, val time: () -> ActivityClock,
    private val preferences: PreferencesRepository) {
    private val engine = ActivityEngine()
    suspend fun active(): ActivitySession? = provider().expandedDao().activeSessions().firstOrNull()
    suspend fun create(type: String, kind: String, title: String, targetSeconds: Long?, breakSeconds: Long = 0,
        subjectId: String? = null, taskId: String? = null, notes: String = "", program: IntervalProgram? = null): ActivitySession {
        require(type in setOf("FOCUS", "WORKOUT", "BREAK", "REST", "INTERVAL"))
        require(targetSeconds == null || targetSeconds in 1..86_400)
        require(breakSeconds in 0..86_400 && title.length <= 500)
        require(notes.length <= 100_000)
        program?.let { it.validate(); require(type == "WORKOUT" && kind == "INTERVALS" && targetSeconds == it.totalSeconds) }
        val p = preferences.get()
        val now = time()
        val session = ActivitySession(UUID.randomUUID().toString(), type, kind, title, subjectId, taskId,
            plannedSeconds = targetSeconds, breakSeconds = breakSeconds, zone = p.zoneId, boundary = p.boundaryMinutes,
            cueFlags = p.cueFlags, notes = notes, updated = now.wall)
        val db = provider()
        return db.withTransaction {
            // Paused work remains explicit: the caller must resume, save or discard it before another session.
            check(db.expandedDao().activeSessions().isEmpty())
            require(subjectId == null || db.expandedDao().subject(subjectId) != null)
            require(taskId == null || db.expandedDao().task(taskId) != null)
            val started = session.applyTiming(engine.resume(session.timing(), now)).copy(started = now.wall)
            db.expandedDao().save(started)
            db.expandedDao().save(segment(started, now))
            program?.let { db.expandedDao().save(SessionNote("program:${started.id}", started.id, Json.encodeToString(it), now.wall)) }
            started
        }
    }
    suspend fun transition(id: String, expectedGeneration: Int, next: ActivityState): ActivitySession {
        val db = provider()
        return db.withTransaction {
            val dao = db.expandedDao()
            val old = requireNotNull(dao.session(id))
            check(old.generation == expectedGeneration)
            val now = time()
            val reconciled = engine.reconcile(old.timing(), now)
            val timing = if (next == ActivityState.RUNNING) engine.resume(reconciled, now)
                else engine.close(reconciled, now, next)
            val fresh = old.applyTiming(timing).copy(updated = now.wall,
                ended = if (next in setOf(ActivityState.COMPLETED, ActivityState.STOPPED, ActivityState.DISCARDED)) now.wall else null)
            closeSegment(dao, old, fresh.activeMillis, now)
            dao.save(fresh)
            if (next != ActivityState.RUNNING) dao.deleteNote("rest:$id")
            if (next == ActivityState.RUNNING) dao.save(segment(fresh, now))
            fresh
        }
    }
    suspend fun complete(id: String, generation: Int): ActivitySession? {
        val db = provider()
        return db.withTransaction {
            val dao = db.expandedDao()
            val current = dao.session(id) ?: return@withTransaction null
            val now = time()
            if (current.state != "RUNNING" || current.generation != generation || current.boot != now.boot ||
                current.deadlineElapsed == null || now.elapsed < current.deadlineElapsed) return@withTransaction null
            val timing = engine.close(current.timing(), now, ActivityState.COMPLETED)
            val fresh = current.applyTiming(timing).copy(ended = now.wall, updated = now.wall)
            closeSegment(dao, current, fresh.activeMillis, now)
            dao.save(fresh)
            dao.deleteNote("rest:$id")
            fresh
        }
    }
    suspend fun reconcile(): List<ActivitySession> {
        val db = provider()
        return db.withTransaction {
            val dao = db.expandedDao()
            dao.activeSessions().map { old ->
                val now = time()
                val reconciled = engine.reconcile(old.timing(), now)
                if (reconciled != old.timing()) {
                    closeSegment(dao, old, old.activeMillis, now)
                    old.applyTiming(reconciled).copy(updated = now.wall).also { dao.save(it) }
                } else old
            }
        }
    }
    // Periodic durability bounds the amount of unknown time after a reboot. It is not a background loop.
    suspend fun checkpoint(id: String, generation: Int) {
        val db = provider()
        db.withTransaction {
            val dao = db.expandedDao()
            val old = dao.session(id) ?: return@withTransaction
            if (old.state != "RUNNING" || old.generation != generation) return@withTransaction
            val now = time()
            if (engine.reconcile(old.timing(), now).state != ActivityState.RUNNING) return@withTransaction
            val active = engine.active(old.timing(), now)
            closeSegment(dao, old, active, now)
            val fresh = old.copy(activeMillis = active, wallAnchor = now.wall, elapsedAnchor = now.elapsed, updated = now.wall)
            dao.save(fresh)
            dao.save(segment(fresh, now))
        }
    }
    private suspend fun closeSegment(dao: ExpandedDao, old: ActivitySession, active: Long, now: ActivityClock) {
        dao.segments(old.id).lastOrNull { it.endWall == null }?.let { span ->
            val delta = (active - old.activeMillis).coerceAtLeast(0)
            dao.save(span.copy(endWall = span.startWall + delta, elapsedEnd = span.elapsedStart + delta, activeMillis = delta))
        }
    }
    private fun segment(session: ActivitySession, now: ActivityClock) = ActivitySegment(UUID.randomUUID().toString(),
        session.id, now.wall, elapsedStart = now.elapsed, zone = session.zone, boundary = session.boundary)
    fun activeMillis(session: ActivitySession): Long = engine.active(session.timing(), time())
    suspend fun editHistory(id: String, actualMillis: Long, notes: String, effort: Int?) {
        require(actualMillis in 0..86_400_000 && notes.length <= 100_000 && (effort == null || effort in 1..10))
        val db = provider()
        db.withTransaction {
            val dao = db.expandedDao()
            val old = requireNotNull(dao.session(id))
            check(old.state in setOf("COMPLETED", "STOPPED", "DISCARDED"))
            dao.deleteSegments(id)
            dao.deleteEstimate(id)
            val start = old.started ?: time().wall
            dao.save(old.copy(activeMillis = actualMillis, notes = notes, effort = effort, source = "USER_CORRECTED", updated = time().wall))
            if (actualMillis > 0) dao.save(ActivitySegment(UUID.randomUUID().toString(), id, start,
                start + actualMillis, 0, actualMillis, actualMillis, old.zone, old.boundary))
        }
    }
    suspend fun deleteHistory(id: String) {
        val db = provider()
        db.withTransaction {
            val dao = db.expandedDao()
            check(dao.session(id)?.state !in setOf("RUNNING", "PAUSED", "INTERRUPTED"))
            dao.deleteSegments(id); dao.deleteSets(id); dao.deleteRoute(id); dao.deleteEstimate(id); dao.deleteSession(id)
            // Session notes are retained as captures rather than silently lost with a history correction.
            dao.notes(id).forEach { dao.save(it.copy(sessionId = null)) }
        }
    }
    suspend fun saveSet(value: ExerciseSet) {
        require(value.exercise.isNotBlank() && value.exercise.length <= 500 && value.reps in 0..10_000 && (value.load == null || value.load.isFinite() && value.load in 0.0..100_000.0))
        require(value.unit in setOf("kg", "lb") && value.notes.length <= 10_000)
        val db = provider()
        db.withTransaction { require(db.expandedDao().session(value.sessionId)?.type == "WORKOUT"); db.expandedDao().save(value) }
    }
    suspend fun sets(id: String) = provider().expandedDao().sets(id)
    suspend fun manualWorkout(kind: String, title: String, actualMinutes: Double, notes: String) {
        require(kind in setOf("WALKING", "RUNNING", "CYCLING", "STRENGTH", "INTERVALS", "MOBILITY", "CUSTOM"))
        require(actualMinutes.isFinite() && actualMinutes > 0 && actualMinutes <= 1440)
        require(title.isNotBlank() && title.length <= 500 && notes.length <= 100_000)
        val now = time(); val p = preferences.get(); val actual = (actualMinutes * 60_000).toLong()
        val id = UUID.randomUUID().toString()
        val db = provider()
        db.withTransaction {
            db.expandedDao().save(ActivitySession(id, "WORKOUT", kind, title, state = "STOPPED",
                activeMillis = actual, started = now.wall - actual, ended = now.wall, notes = notes,
                zone = p.zoneId, boundary = p.boundaryMinutes, source = "USER_ENTERED", updated = now.wall))
            db.expandedDao().save(ActivitySegment(UUID.randomUUID().toString(), id, now.wall - actual,
                now.wall, 0, actual, actual, p.zoneId, p.boundaryMinutes))
        }
    }
    suspend fun deleteSet(id: String) = provider().expandedDao().deleteSet(id)
    suspend fun saveTemplate(value: WorkoutTemplate) {
        require(value.title.isNotBlank() && value.title.length <= 500 && value.exercises.length <= 100_000)
        require(value.mode in setOf("STRENGTH", "INTERVALS"))
        IntervalProgram(value.workSeconds,value.restSeconds,value.rounds,value.warmupSeconds,value.cooldownSeconds).validate()
        provider().expandedDao().save(value)
    }
    suspend fun templates() = provider().expandedDao().templates()
    suspend fun deleteTemplate(id: String) = provider().expandedDao().deleteTemplate(id)
    suspend fun program(id: String): IntervalProgram? = provider().expandedDao().note("program:$id")?.let {
        Json.decodeFromString<IntervalProgram>(it.text).also(IntervalProgram::validate)
    }
    suspend fun effort(id: String, value: Int?) {
        require(value == null || value in 1..10)
        val db = provider()
        db.withTransaction { val session = requireNotNull(db.expandedDao().session(id)); db.expandedDao().save(session.copy(effort = value, updated = time().wall)) }
    }
    suspend fun rest(id: String): WorkoutRest? = provider().expandedDao().note("rest:$id")?.let { Json.decodeFromString<WorkoutRest>(it.text) }
    suspend fun startRest(sessionId: String, seconds: Int): WorkoutRest {
        require(seconds in 1..3600)
        val db = provider()
        return db.withTransaction {
            val session = requireNotNull(db.expandedDao().session(sessionId))
            require(session.type == "WORKOUT" && session.kind == "STRENGTH" && session.state == "RUNNING")
            val now = time()
            val old = rest(sessionId)
            WorkoutRest(sessionId, sessionId, now.elapsed + seconds * 1000L, now.boot, (old?.generation ?: 0) + 1,
                session.cueFlags).also { db.expandedDao().save(SessionNote("rest:$sessionId", sessionId, Json.encodeToString(it), now.wall)) }
        }
    }
    suspend fun completeRest(id: String, generation: Int): WorkoutRest? {
        val db = provider()
        return db.withTransaction {
            val value = rest(id) ?: return@withTransaction null
            val now = time()
            if (value.complete || value.generation != generation || value.boot != now.boot || now.elapsed < value.deadlineElapsed ||
                db.expandedDao().session(value.sessionId)?.state != "RUNNING") return@withTransaction null
            value.copy(complete = true).also { db.expandedDao().save(SessionNote("rest:$id", id, Json.encodeToString(it), now.wall)) }
        }
    }
    suspend fun cancelRest(id: String) = provider().expandedDao().deleteNote("rest:$id")
}

private fun ActivitySession.timing() = ActivityTiming(ActivityState.valueOf(state), plannedSeconds?.times(1000),
    activeMillis, wallAnchor, elapsedAnchor, deadlineElapsed, boot, generation)
private fun ActivitySession.applyTiming(t: ActivityTiming) = copy(state = t.state.name, activeMillis = t.activeMillis,
    wallAnchor = t.wallAnchor, elapsedAnchor = t.elapsedAnchor, deadlineElapsed = t.deadlineElapsed,
    boot = t.boot, generation = t.generation)
