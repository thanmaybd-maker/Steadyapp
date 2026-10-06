package com.thanu.steady.data

import androidx.room.withTransaction
import com.thanu.steady.domain.TimerSession
import com.thanu.steady.domain.TimerState
import com.thanu.steady.domain.TimerStateMachine
import kotlinx.coroutines.flow.map
import java.time.Instant

data class TimerTime(val wall: Instant, val elapsed: Long, val boot: Long)

class TimerRepository(private val databaseProvider: () -> SteadyDatabase, val time: () -> TimerTime) {
    fun observe() = databaseProvider().timerDao().observeLatest().map { it?.toDomain() }
    suspend fun transition(expected: TimerSession, next: TimerSession) {
        val db = databaseProvider()
        db.withTransaction {
            val stored = db.timerDao().getSession(expected.id)
            if (stored != null) check(stored.generation == expected.generation && stored.state == expected.state)
            else check(expected.startedAt == null && expected.generation == 0)
            if (next.state == TimerState.RUNNING) check(db.timerDao().getRunning().all { it.id == expected.id })
            db.timerDao().insertSession(next.toEntity())
        }
    }
    suspend fun complete(id: String, generation: Int): TimerSession? {
        val db = databaseProvider()
        return db.withTransaction {
            val current = db.timerDao().getSession(id)?.toDomain() ?: return@withTransaction null
            val now = time()
            if (current.state != TimerState.RUNNING || current.generation != generation || current.bootMarker != now.boot ||
                current.targetElapsedTime == null || now.elapsed < current.targetElapsedTime) return@withTransaction null
            TimerStateMachine().complete(current, now.wall).also { db.timerDao().insertSession(it.toEntity()) }
        }
    }
    suspend fun reconcile(interrupt: Boolean = false): List<TimerSession> {
        val db = databaseProvider()
        return db.withTransaction {
            val now = time()
            db.timerDao().getRunning().map { row ->
                val timer = row.toDomain()
                if (interrupt || timer.bootMarker != now.boot || timer.targetElapsedTime == null ||
                    timer.targetWallTime == null || kotlin.math.abs(java.time.Duration.between(now.wall, timer.targetWallTime).toMillis() -
                        (timer.targetElapsedTime - now.elapsed)) > 60_000) {
                    TimerStateMachine().interrupt(timer, now.wall).also { db.timerDao().insertSession(it.toEntity()) }
                } else timer
            }
        }
    }
}

fun TimerSessionEntity.toDomain() = TimerSession(id, logicalDay, kind, durationMs, remainingMs, state, startedAt,
    targetWallTime, targetElapsedTime, bootMarker, cueFlags, completedAt, generation)
fun TimerSession.toEntity() = TimerSessionEntity(id, logicalDay, kind, durationMs, remainingMs, state, startedAt,
    targetWallTime, targetElapsedTime, bootMarker, cueFlags, completedAt, generation)
