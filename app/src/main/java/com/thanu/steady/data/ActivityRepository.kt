package com.thanu.steady.data

import androidx.room.withTransaction
import com.thanu.steady.domain.*
import java.time.ZoneId
import java.util.UUID

class ActivityRepository(private val provider: () -> SteadyDatabase, val time: () -> ActivityClock,
    private val preferences: PreferencesRepository) {
    private val engine = ActivityEngine()
    suspend fun active(): ActivitySession? = provider().expandedDao().activeSessions().firstOrNull()
    suspend fun create(type: String, kind: String, title: String, targetSeconds: Long?, breakSeconds: Long = 0,
        subjectId: String? = null, taskId: String? = null): ActivitySession {
        require(type in setOf("FOCUS", "WORKOUT", "BREAK", "REST", "INTERVAL"))
        require(targetSeconds == null || targetSeconds in 1..86_400)
        require(breakSeconds in 0..86_400 && title.length <= 500)
        val p = preferences.get()
        val now = time()
        val session = ActivitySession(UUID.randomUUID().toString(), type, kind, title, subjectId, taskId,
            plannedSeconds = targetSeconds, breakSeconds = breakSeconds, zone = p.zoneId, boundary = p.boundaryMinutes,
            cueFlags = p.cueFlags, updated = now.wall)
        val db = provider()
        return db.withTransaction {
            // Paused work remains explicit: the caller must resume, save or discard it before another session.
            check(db.expandedDao().activeSessions().isEmpty())
            require(subjectId == null || db.expandedDao().subject(subjectId) != null)
            require(taskId == null || db.expandedDao().task(taskId) != null)
            val started = session.applyTiming(engine.resume(session.timing(), now)).copy(started = now.wall)
            db.expandedDao().save(started)
            db.expandedDao().save(segment(started, now))
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
        require(value.exercise.isNotBlank() && value.reps in 0..10_000 && (value.load == null || value.load.isFinite() && value.load >= 0))
        require(value.unit in setOf("kg", "lb") && value.notes.length <= 10_000)
        val db = provider()
        db.withTransaction { require(db.expandedDao().session(value.sessionId)?.type == "WORKOUT"); db.expandedDao().save(value) }
    }
    suspend fun sets(id: String) = provider().expandedDao().sets(id)
    suspend fun manualWorkout(kind: String, title: String, actualMinutes: Double, notes: String) {
        require(kind in setOf("WALK", "RUN", "CYCLE", "STRENGTH", "INTERVALS", "MOBILITY", "CUSTOM"))
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
        require(value.title.isNotBlank() && value.rounds in 1..100 && value.workSeconds in 1..86_400 && value.restSeconds in 0..86_400)
        require(value.warmupSeconds in 0..86_400 && value.cooldownSeconds in 0..86_400)
        provider().expandedDao().save(value)
    }
    suspend fun templates() = provider().expandedDao().templates()
}

private fun ActivitySession.timing() = ActivityTiming(ActivityState.valueOf(state), plannedSeconds?.times(1000),
    activeMillis, wallAnchor, elapsedAnchor, deadlineElapsed, boot, generation)
private fun ActivitySession.applyTiming(t: ActivityTiming) = copy(state = t.state.name, activeMillis = t.activeMillis,
    wallAnchor = t.wallAnchor, elapsedAnchor = t.elapsedAnchor, deadlineElapsed = t.deadlineElapsed,
    boot = t.boot, generation = t.generation)
