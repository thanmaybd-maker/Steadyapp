package com.thanu.steady.data

import androidx.room.withTransaction
import com.thanu.steady.domain.DayMode
import com.thanu.steady.domain.TimerState
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class RecoverySnapshot(
    val plans: List<DailyPlanEntity>,
    val reviews: List<WeeklyReviewEntity>,
    val timers: List<TimerSessionEntity>
) {
    val firstDay get() = plans.minOfOrNull { it.logicalDay }
    val lastDay get() = plans.maxOfOrNull { it.logicalDay }
}

/** Only eligible organiser tables cross this boundary; Safety is never queried. */
class RecoveryRepository(private val databaseProvider: () -> SteadyDatabase) {
    suspend fun markdownRecords(day: LocalDate): Pair<com.thanu.steady.domain.DailyPlan?, com.thanu.steady.domain.WeeklyReview?> {
        val db = databaseProvider()
        return db.withTransaction {
            db.dailyPlanDao().getPlan(day)?.toDomain() to db.reviewDao().getReview(day)?.toDomain()
        }
    }
    suspend fun snapshot(): RecoverySnapshot {
        val db = databaseProvider()
        return db.withTransaction {
            RecoverySnapshot(db.dailyPlanDao().getAll(), db.reviewDao().getAll(), db.timerDao().getAll())
        }
    }

    suspend fun replace(snapshot: RecoverySnapshot): List<String> {
        val db = databaseProvider()
        return db.withTransaction {
            val previousTimerIds = db.timerDao().getAll().map { it.id }
            db.dailyPlanDao().clear()
            db.reviewDao().clear()
            db.timerDao().clear()
            snapshot.plans.forEach { db.dailyPlanDao().insertPlan(it) }
            snapshot.reviews.forEach { db.reviewDao().insertReview(it) }
            snapshot.timers.forEach {
                db.timerDao().insertSession(it.copy(
                    state = TimerState.CANCELLED, targetWallTime = null, targetElapsedTime = null,
                    remainingMs = it.durationMs, generation = it.generation + 1
                ))
            }
            previousTimerIds
        }
    }
}

object RecoveryCodec {
    private const val MAX_RECORDS = 10_000
    private const val MAX_TEXT = 2_000
    private val planKeys = setOf("day", "zone", "boundary", "mode", "health", "study", "build", "next",
        "studyEvidence", "buildEvidence", "shutdown", "created", "updated")
    private val reviewKeys = setOf("end", "sleep", "learning", "building", "health", "connection",
        "helped", "demanding", "evidence", "adjustment", "updated")
    private val timerKeys = setOf("id", "day", "kind", "duration", "remaining", "state", "started", "wall",
        "elapsed", "boot", "cues", "completed", "generation")

    fun encode(snapshot: RecoverySnapshot): String {
        fun objectOf(vararg fields: Pair<String, Any?>) = JSONObject().apply {
            fields.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
        }
        val plans = JSONArray().apply { snapshot.plans.forEach { p -> put(objectOf(
            "day" to p.logicalDay.toString(), "zone" to p.zoneId.id, "boundary" to p.boundaryMinutes,
            "mode" to p.mode.name, "health" to p.healthTask, "study" to p.studyTask, "build" to p.buildTask,
            "next" to p.nextAction, "studyEvidence" to p.studyEvidence, "buildEvidence" to p.buildEvidence,
            "shutdown" to p.shutdownAt?.toEpochMilli(), "created" to p.createdAt.toEpochMilli(),
            "updated" to p.updatedAt.toEpochMilli()
        )) } }
        val reviews = JSONArray().apply { snapshot.reviews.forEach { r -> put(objectOf(
            "end" to r.weekEnd.toString(), "sleep" to r.indicatorSleep, "learning" to r.indicatorLearning,
            "building" to r.indicatorBuilding, "health" to r.indicatorHealth, "connection" to r.indicatorConnection,
            "helped" to r.helped, "demanding" to r.tooDemanding, "evidence" to r.changedEvidence,
            "adjustment" to r.adjustment, "updated" to r.updatedAt.toEpochMilli()
        )) } }
        val timers = JSONArray().apply { snapshot.timers.forEach { t -> put(objectOf(
            "id" to t.id, "day" to t.logicalDay, "kind" to t.kind, "duration" to t.durationMs,
            "remaining" to t.remainingMs, "state" to t.state.name, "started" to t.startedAt?.toEpochMilli(),
            "wall" to t.targetWallTime?.toEpochMilli(), "elapsed" to t.targetElapsedTime, "boot" to t.bootMarker,
            "cues" to t.cueFlags, "completed" to t.completedAt?.toEpochMilli(), "generation" to t.generation
        )) } }
        return objectOf("schema" to 1, "plans" to plans, "reviews" to reviews, "timers" to timers).toString()
            .also { decode(it) } // The archive we create must meet the same validation as restore.
    }

    fun decode(json: String): RecoverySnapshot {
        require(json.toByteArray(Charsets.UTF_8).size <= com.thanu.steady.domain.BackupService.MAX_PAYLOAD_BYTES)
        checkNesting(json)
        val root = JSONObject(json).checked(setOf("schema", "plans", "reviews", "timers"))
        require(root.number("schema") == 1L)
        fun objects(key: String): List<JSONObject> {
            val array = root.get(key) as? JSONArray ?: error("Invalid archive")
            require(array.length() <= MAX_RECORDS)
            return (0 until array.length()).map { array.get(it) as? JSONObject ?: error("Invalid archive") }
        }
        val plans = objects("plans").map { o ->
            o.checked(planKeys)
            val boundary = o.number("boundary").also { require(it in 0..1439) }.toInt()
            DailyPlanEntity(o.day("day"), ZoneId.of(o.text("zone")), boundary, DayMode.valueOf(o.text("mode")),
                o.text("health"), o.text("study"), o.text("build"), o.text("next"),
                o.optionalText("studyEvidence"), o.optionalText("buildEvidence"), o.instant("shutdown"),
                o.instant("created") ?: error("Invalid archive"), o.instant("updated") ?: error("Invalid archive"))
        }
        val reviews = objects("reviews").map { o ->
            o.checked(reviewKeys)
            WeeklyReviewEntity(o.day("end"), o.text("sleep"), o.text("learning"), o.text("building"),
                o.text("health"), o.text("connection"), o.text("helped"), o.text("demanding"),
                o.text("evidence"), o.text("adjustment"), o.instant("updated") ?: error("Invalid archive"))
        }
        val timers = objects("timers").map { o ->
            o.checked(timerKeys)
            val id = o.text("id").also { require(UUID.fromString(it).toString() == it) }
            val day = o.optionalText("day")?.also { parseDay(it) }
            val duration = o.number("duration").also { require(it in 1_000..86_400_000) }
            val remaining = o.number("remaining").also { require(it in 0..duration) }
            val generation = o.number("generation").also { require(it in 0 until Int.MAX_VALUE.toLong()) }.toInt()
            val cues = o.number("cues").also { require(it in 0..7) }.toInt()
            val kind = o.text("kind").also { require(it in setOf("break", "focus")) }
            TimerSessionEntity(id, day, kind, duration, remaining, TimerState.valueOf(o.text("state")),
                o.instant("started"), o.instant("wall"), o.optionalNumber("elapsed")?.also { require(it >= 0) },
                o.number("boot").also { require(it >= 0) }, cues, o.instant("completed"), generation)
        }
        require(plans.map { it.logicalDay }.toSet().size == plans.size)
        require(reviews.map { it.weekEnd }.toSet().size == reviews.size)
        require(timers.map { it.id }.toSet().size == timers.size)
        return RecoverySnapshot(plans, reviews, timers)
    }

    private fun JSONObject.checked(keys: Set<String>): JSONObject {
        require(keys().asSequence().toSet() == keys)
        return this
    }
    private fun JSONObject.text(key: String): String = (get(key) as? String ?: error("Invalid archive"))
        .also { require(it.length <= MAX_TEXT) }
    private fun JSONObject.optionalText(key: String) = if (isNull(key)) null else text(key)
    private fun JSONObject.number(key: String): Long {
        val value = get(key)
        require(value is Int || value is Long)
        return (value as Number).toLong()
    }
    private fun JSONObject.optionalNumber(key: String) = if (isNull(key)) null else number(key)
    private fun JSONObject.instant(key: String) = optionalNumber(key)?.let(Instant::ofEpochMilli)
    private fun JSONObject.day(key: String) = parseDay(text(key))
    private fun parseDay(value: String): LocalDate {
        require(value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        return LocalDate.parse(value)
    }
    private fun checkNesting(json: String) {
        var depth = 0
        var quoted = false
        var escaped = false
        json.forEach { ch ->
            if (quoted) {
                if (escaped) escaped = false else if (ch == '\\') escaped = true else if (ch == '"') quoted = false
            } else when (ch) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 4) }
                '}', ']' -> { depth--; require(depth >= 0) }
            }
        }
        require(depth == 0 && !quoted)
    }
}
