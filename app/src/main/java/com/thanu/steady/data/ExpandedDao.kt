package com.thanu.steady.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpandedDao {
    @Query("SELECT * FROM expanded_profile WHERE id = 1") suspend fun profile(): ExpandedProfile?
    @Query("SELECT * FROM expanded_profile WHERE id = 1") fun observeProfile(): Flow<ExpandedProfile?>
    @Upsert suspend fun save(value: ExpandedProfile)
    @Query("SELECT * FROM day_settings WHERE day = :day") suspend fun day(day: String): DaySettings?
    @Upsert suspend fun save(value: DaySettings)
    @Query("SELECT * FROM day_settings WHERE day BETWEEN :start AND :end") suspend fun days(start: String, end: String): List<DaySettings>
    @Query("SELECT * FROM plan_item WHERE day BETWEEN :start AND :end ORDER BY day,position,created")
    suspend fun tasks(start: String, end: String): List<PlanItem>
    @Query("SELECT * FROM plan_item WHERE id = :id") suspend fun task(id: String): PlanItem?
    @Upsert suspend fun save(value: PlanItem)
    @Query("DELETE FROM plan_item WHERE id = :id") suspend fun deleteTask(id: String)
    @Query("UPDATE activity_session SET taskId = NULL WHERE taskId = :id") suspend fun detachTask(id: String)
    @Query("SELECT * FROM habit_definition") suspend fun habits(): List<HabitDefinition>
    @Query("SELECT * FROM habit_definition WHERE id = :id") suspend fun habit(id: String): HabitDefinition?
    @Upsert suspend fun save(value: HabitDefinition)
    @Query("SELECT v.* FROM habit_version v JOIN habit_definition h ON h.id = v.habitId WHERE v.effectiveDay <= :day AND (h.archivedDay IS NULL OR h.archivedDay > :day) AND v.effectiveDay = (SELECT MAX(effectiveDay) FROM habit_version WHERE habitId = h.id AND effectiveDay <= :day)")
    suspend fun versionsForDay(day: String): List<HabitVersion>
    @Query("SELECT * FROM habit_version WHERE effectiveDay <= :end AND (effectiveDay >= :start OR effectiveDay = (SELECT MAX(v.effectiveDay) FROM habit_version v WHERE v.habitId = habit_version.habitId AND v.effectiveDay < :start)) ORDER BY effectiveDay")
    suspend fun versionsForRange(start: String, end: String): List<HabitVersion>
    @Query("SELECT * FROM habit_version WHERE id IN (:ids)") suspend fun versions(ids: List<String>): List<HabitVersion>
    @Upsert suspend fun save(value: HabitVersion)
    @Query("SELECT * FROM habit_version WHERE habitId = :habitId AND effectiveDay = :day")
    suspend fun versionOn(habitId: String, day: String): HabitVersion?
    @Query("SELECT * FROM habit_occurrence WHERE day BETWEEN :start AND :end ORDER BY day")
    suspend fun occurrences(start: String, end: String): List<HabitOccurrence>
    @Query("SELECT * FROM habit_occurrence WHERE id = :id") suspend fun occurrence(id: String): HabitOccurrence?
    @Upsert suspend fun save(value: HabitOccurrence)
    @Query("SELECT * FROM habit_log WHERE occurrenceId = :id ORDER BY at") suspend fun logs(id: String): List<HabitLog>
    @Query("SELECT * FROM habit_log WHERE id = :id") suspend fun habitLog(id: String): HabitLog?
    @Upsert suspend fun save(value: HabitLog)
    @Query("SELECT * FROM subject ORDER BY title") suspend fun subjects(): List<Subject>
    @Query("SELECT * FROM subject WHERE id = :id") suspend fun subject(id: String): Subject?
    @Upsert suspend fun save(value: Subject)
    @Query("SELECT * FROM topic WHERE subjectId = :subjectId ORDER BY title") suspend fun topics(subjectId: String): List<Topic>
    @Upsert suspend fun save(value: Topic)
    @Query("SELECT * FROM activity_session WHERE state IN ('RUNNING','PAUSED','INTERRUPTED') ORDER BY updated DESC")
    suspend fun activeSessions(): List<ActivitySession>
    @Query("SELECT * FROM activity_session WHERE id = :id") suspend fun session(id: String): ActivitySession?
    @Query("SELECT * FROM activity_session WHERE started <= :end AND (ended IS NULL OR ended >= :start) ORDER BY started DESC")
    suspend fun sessions(start: Long, end: Long): List<ActivitySession>
    @Upsert suspend fun save(value: ActivitySession)
    @Query("DELETE FROM activity_session WHERE id = :id") suspend fun deleteSession(id: String)
    @Query("SELECT * FROM activity_segment WHERE sessionId = :id ORDER BY startWall") suspend fun segments(id: String): List<ActivitySegment>
    @Query("SELECT * FROM activity_segment WHERE startWall <= :end AND (endWall IS NULL OR endWall >= :start)")
    suspend fun segmentsInRange(start: Long, end: Long): List<ActivitySegment>
    @Upsert suspend fun save(value: ActivitySegment)
    @Query("DELETE FROM activity_segment WHERE sessionId = :id") suspend fun deleteSegments(id: String)
    @Query("SELECT * FROM session_note WHERE id = :id") suspend fun note(id: String): SessionNote?
    @Query("SELECT * FROM session_note WHERE id LIKE :prefix ORDER BY savedAt") suspend fun notesWithPrefix(prefix: String): List<SessionNote>
    @Query("SELECT * FROM session_note WHERE sessionId = :id ORDER BY savedAt") suspend fun notes(id: String): List<SessionNote>
    @Query("SELECT n.* FROM session_note n LEFT JOIN activity_session s ON s.id = n.sessionId WHERE n.id NOT LIKE 'draft:%' AND n.id NOT LIKE 'program:%' AND n.id NOT LIKE 'rest:%' AND n.id NOT LIKE 'delivery:%' AND n.id NOT LIKE 'ambient:%' AND n.id NOT LIKE 'study-block%' AND ((n.sessionId IS NULL AND n.savedAt BETWEEN :start AND :end) OR (s.started <= :end AND (s.ended IS NULL OR s.ended >= :start))) ORDER BY n.savedAt")
    suspend fun notesInRange(start: Long, end: Long): List<SessionNote>
    @Upsert suspend fun save(value: SessionNote)
    @Query("DELETE FROM session_note WHERE id = :id") suspend fun deleteNote(id: String)
    @Query("SELECT COUNT(*) FROM session_note WHERE id LIKE 'delivery:' || :day || ':%'") suspend fun reminderCount(day: String): Int
    @Query("SELECT * FROM exercise_set WHERE sessionId = :id ORDER BY position,doneAt") suspend fun sets(id: String): List<ExerciseSet>
    @Upsert suspend fun save(value: ExerciseSet)
    @Query("DELETE FROM exercise_set WHERE id = :id") suspend fun deleteSet(id: String)
    @Query("DELETE FROM exercise_set WHERE sessionId = :id") suspend fun deleteSets(id: String)
    @Query("SELECT * FROM workout_template ORDER BY title") suspend fun templates(): List<WorkoutTemplate>
    @Upsert suspend fun save(value: WorkoutTemplate)
    @Query("DELETE FROM workout_template WHERE id = :id") suspend fun deleteTemplate(id: String)
    @Query("SELECT * FROM water_log WHERE day BETWEEN :start AND :end ORDER BY at DESC") suspend fun water(start: String, end: String): List<WaterLog>
    @Upsert suspend fun save(value: WaterLog)
    @Query("DELETE FROM water_log WHERE id = :id") suspend fun deleteWater(id: String)
    @Query("SELECT * FROM sleep_log WHERE day BETWEEN :start AND :end ORDER BY wake DESC") suspend fun sleep(start: String, end: String): List<SleepLog>
    @Upsert suspend fun save(value: SleepLog)
    @Query("DELETE FROM sleep_log WHERE id = :id") suspend fun deleteSleep(id: String)
    @Query("SELECT * FROM food_idea ORDER BY favorite DESC,title") suspend fun foods(): List<FoodIdeaRecord>
    @Query("SELECT * FROM food_idea WHERE id = :id") suspend fun food(id: String): FoodIdeaRecord?
    @Upsert suspend fun save(value: FoodIdeaRecord)
    @Query("DELETE FROM food_idea WHERE id = :id") suspend fun deleteFood(id: String)
    @Query("UPDATE meal_log SET foodId = NULL WHERE foodId = :id") suspend fun detachFood(id: String)
    @Query("SELECT * FROM meal_log WHERE day BETWEEN :start AND :end ORDER BY at DESC") suspend fun meals(start: String, end: String): List<MealLog>
    @Upsert suspend fun save(value: MealLog)
    @Query("DELETE FROM meal_log WHERE id = :id") suspend fun deleteMeal(id: String)
    @Query("SELECT * FROM care_reminder ORDER BY minute") suspend fun care(): List<CareReminder>
    @Upsert suspend fun save(value: CareReminder)
    @Query("SELECT * FROM care_log WHERE day BETWEEN :start AND :end ORDER BY at") suspend fun careLogs(start: String, end: String): List<CareLog>
    @Upsert suspend fun save(value: CareLog)
    @Query("SELECT * FROM reflection WHERE startDay = :start AND endDay = :end") suspend fun reflection(start: String, end: String): Reflection?
    @Upsert suspend fun save(value: Reflection)
    @Query("SELECT * FROM capture WHERE archived = 0 ORDER BY created DESC LIMIT 200") suspend fun captures(): List<Capture>
    @Query("SELECT * FROM capture WHERE created BETWEEN :start AND :end ORDER BY created") suspend fun capturesInRange(start: Long, end: Long): List<Capture>
    @Upsert suspend fun save(value: Capture)
    @Upsert suspend fun save(value: InterruptionEvent)
    @Query("SELECT * FROM interruption_event WHERE at BETWEEN :start AND :end ORDER BY at DESC") suspend fun interruptions(start: Long, end: Long): List<InterruptionEvent>
    @Query("SELECT * FROM activity_observation WHERE day BETWEEN :start AND :end ORDER BY start") suspend fun observations(start: String, end: String): List<ActivityObservation>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertObservation(value: ActivityObservation): Long
    @Query("SELECT * FROM route_point WHERE sessionId = :id ORDER BY at") suspend fun route(id: String): List<RoutePoint>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertRoute(value: RoutePoint): Long
    @Query("DELETE FROM route_point WHERE sessionId = :id") suspend fun deleteRoute(id: String)
    @Query("SELECT * FROM energy_estimate WHERE sessionId = :id") suspend fun estimate(id: String): EnergyEstimate?
    @Upsert suspend fun save(value: EnergyEstimate)
    @Query("DELETE FROM energy_estimate WHERE sessionId = :id") suspend fun deleteEstimate(id: String)

    // Room invalidates this bounded signal when source tables change; snapshots query only the selected period.
    @Query("SELECT (SELECT COUNT(*) FROM plan_item WHERE day BETWEEN :start AND :end) + (SELECT COUNT(*) FROM habit_occurrence WHERE day BETWEEN :start AND :end) + (SELECT COUNT(*) FROM water_log WHERE day BETWEEN :start AND :end) + (SELECT COUNT(*) FROM sleep_log WHERE day BETWEEN :start AND :end) + (SELECT COUNT(*) FROM meal_log WHERE day BETWEEN :start AND :end) + (SELECT COUNT(*) FROM day_settings WHERE day BETWEEN :start AND :end) + (SELECT COUNT(*) FROM activity_session WHERE state IN ('RUNNING','PAUSED','INTERRUPTED')) + (SELECT COUNT(*) FROM subject) + (SELECT COUNT(*) FROM food_idea) + (SELECT COUNT(*) FROM care_reminder) + (SELECT COUNT(*) FROM care_log WHERE day BETWEEN :start AND :end) + (SELECT COUNT(*) FROM capture WHERE archived = 0) + (SELECT COUNT(*) FROM expanded_profile) + (SELECT COUNT(*) FROM habit_version WHERE effectiveDay <= :end) + (SELECT COUNT(*) FROM habit_definition) + (SELECT COUNT(*) FROM activity_segment WHERE startWall BETWEEN :startWall AND :endWall) + (SELECT COUNT(*) FROM session_note) + (SELECT COUNT(*) FROM reflection WHERE startDay = :start AND endDay = :end) + (SELECT COUNT(*) FROM activity_observation WHERE day BETWEEN :start AND :end)")
    fun observeChanges(start: String, end: String, startWall: Long, endWall: Long): Flow<Long>
}
