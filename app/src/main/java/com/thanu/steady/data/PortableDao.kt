package com.thanu.steady.data

import androidx.room.*

/** Explicit table allowlist used only for portable recovery. */
@Dao
interface PortableDao {
    @Query("SELECT * FROM plan_item") suspend fun allTasks(): List<PlanItem>
    @Upsert suspend fun restoreTasks(values: List<PlanItem>)
    @Query("DELETE FROM plan_item") suspend fun clearTasks()
    @Query("SELECT * FROM habit_definition") suspend fun allHabits(): List<HabitDefinition>
    @Upsert suspend fun restoreHabits(values: List<HabitDefinition>)
    @Query("DELETE FROM habit_definition") suspend fun clearHabits()
    @Query("SELECT * FROM habit_version") suspend fun allVersions(): List<HabitVersion>
    @Upsert suspend fun restoreVersions(values: List<HabitVersion>)
    @Query("DELETE FROM habit_version") suspend fun clearVersions()
    @Query("SELECT * FROM habit_occurrence") suspend fun allOccurrences(): List<HabitOccurrence>
    @Upsert suspend fun restoreOccurrences(values: List<HabitOccurrence>)
    @Query("DELETE FROM habit_occurrence") suspend fun clearOccurrences()
    @Query("SELECT * FROM habit_log") suspend fun allLogs(): List<HabitLog>
    @Upsert suspend fun restoreLogs(values: List<HabitLog>)
    @Query("DELETE FROM habit_log") suspend fun clearLogs()
    @Query("SELECT * FROM subject") suspend fun allSubjects(): List<Subject>
    @Upsert suspend fun restoreSubjects(values: List<Subject>)
    @Query("DELETE FROM subject") suspend fun clearSubjects()
    @Query("SELECT * FROM topic") suspend fun allTopics(): List<Topic>
    @Upsert suspend fun restoreTopics(values: List<Topic>)
    @Query("DELETE FROM topic") suspend fun clearTopics()
    @Query("SELECT * FROM activity_session") suspend fun allSessions(): List<ActivitySession>
    @Upsert suspend fun restoreSessions(values: List<ActivitySession>)
    @Query("DELETE FROM activity_session") suspend fun clearSessions()
    @Query("SELECT * FROM activity_segment") suspend fun allSegments(): List<ActivitySegment>
    @Upsert suspend fun restoreSegments(values: List<ActivitySegment>)
    @Query("DELETE FROM activity_segment") suspend fun clearSegments()
    @Query("SELECT * FROM session_note WHERE id NOT LIKE 'draft:%' AND id NOT LIKE 'rest:%' AND id NOT LIKE 'delivery:%'") suspend fun allNotes(): List<SessionNote>
    @Upsert suspend fun restoreNotes(values: List<SessionNote>)
    @Query("DELETE FROM session_note") suspend fun clearNotes()
    @Query("SELECT * FROM exercise_set") suspend fun allSets(): List<ExerciseSet>
    @Upsert suspend fun restoreSets(values: List<ExerciseSet>)
    @Query("DELETE FROM exercise_set") suspend fun clearSets()
    @Query("SELECT * FROM workout_template") suspend fun allTemplates(): List<WorkoutTemplate>
    @Upsert suspend fun restoreTemplates(values: List<WorkoutTemplate>)
    @Query("DELETE FROM workout_template") suspend fun clearTemplates()
    @Query("SELECT * FROM water_log") suspend fun allWater(): List<WaterLog>
    @Upsert suspend fun restoreWater(values: List<WaterLog>)
    @Query("DELETE FROM water_log") suspend fun clearWater()
    @Query("SELECT * FROM sleep_log") suspend fun allSleep(): List<SleepLog>
    @Upsert suspend fun restoreSleep(values: List<SleepLog>)
    @Query("DELETE FROM sleep_log") suspend fun clearSleep()
    @Query("SELECT * FROM food_idea") suspend fun allFoods(): List<FoodIdeaRecord>
    @Upsert suspend fun restoreFoods(values: List<FoodIdeaRecord>)
    @Query("DELETE FROM food_idea") suspend fun clearFoods()
    @Query("SELECT * FROM meal_log") suspend fun allMeals(): List<MealLog>
    @Upsert suspend fun restoreMeals(values: List<MealLog>)
    @Query("DELETE FROM meal_log") suspend fun clearMeals()
    @Query("SELECT * FROM care_reminder") suspend fun allCare(): List<CareReminder>
    @Upsert suspend fun restoreCare(values: List<CareReminder>)
    @Query("DELETE FROM care_reminder") suspend fun clearCare()
    @Query("SELECT * FROM care_log") suspend fun allCareLogs(): List<CareLog>
    @Upsert suspend fun restoreCareLogs(values: List<CareLog>)
    @Query("DELETE FROM care_log") suspend fun clearCareLogs()
    @Query("SELECT * FROM reflection") suspend fun allReflections(): List<Reflection>
    @Upsert suspend fun restoreReflections(values: List<Reflection>)
    @Query("DELETE FROM reflection") suspend fun clearReflections()
    @Query("SELECT * FROM capture") suspend fun allCaptures(): List<Capture>
    @Upsert suspend fun restoreCaptures(values: List<Capture>)
    @Query("DELETE FROM capture") suspend fun clearCaptures()
    @Query("SELECT * FROM interruption_event") suspend fun allInterruptions(): List<InterruptionEvent>
    @Upsert suspend fun restoreInterruptions(values: List<InterruptionEvent>)
    @Query("DELETE FROM interruption_event") suspend fun clearInterruptions()
    @Query("SELECT * FROM activity_observation") suspend fun allObservations(): List<ActivityObservation>
    @Upsert suspend fun restoreObservations(values: List<ActivityObservation>)
    @Query("DELETE FROM activity_observation") suspend fun clearObservations()
    @Query("SELECT * FROM route_point") suspend fun allRoutes(): List<RoutePoint>
    @Upsert suspend fun restoreRoutes(values: List<RoutePoint>)
    @Query("DELETE FROM route_point") suspend fun clearRoutes()
    @Query("SELECT * FROM energy_estimate") suspend fun allEstimates(): List<EnergyEstimate>
    @Upsert suspend fun restoreEstimates(values: List<EnergyEstimate>)
    @Query("DELETE FROM energy_estimate") suspend fun clearEstimates()
    @Query("SELECT * FROM day_settings") suspend fun allDays(): List<DaySettings>
    @Upsert suspend fun restoreDays(values: List<DaySettings>)
    @Query("DELETE FROM day_settings") suspend fun clearDays()
    @Query("DELETE FROM expanded_profile") suspend fun clearProfile()
    @Query("DELETE FROM app_preferences") suspend fun clearPreferences()
    @Query("SELECT (SELECT COUNT(*) FROM plan_item) + (SELECT COUNT(*) FROM habit_definition) + (SELECT COUNT(*) FROM habit_version) + (SELECT COUNT(*) FROM habit_occurrence) + (SELECT COUNT(*) FROM habit_log) + (SELECT COUNT(*) FROM subject) + (SELECT COUNT(*) FROM topic) + (SELECT COUNT(*) FROM activity_session) + (SELECT COUNT(*) FROM activity_segment) + (SELECT COUNT(*) FROM session_note WHERE id NOT LIKE 'draft:%') + (SELECT COUNT(*) FROM exercise_set) + (SELECT COUNT(*) FROM workout_template) + (SELECT COUNT(*) FROM water_log) + (SELECT COUNT(*) FROM sleep_log) + (SELECT COUNT(*) FROM food_idea) + (SELECT COUNT(*) FROM meal_log) + (SELECT COUNT(*) FROM care_reminder) + (SELECT COUNT(*) FROM care_log) + (SELECT COUNT(*) FROM reflection) + (SELECT COUNT(*) FROM capture) + (SELECT COUNT(*) FROM interruption_event) + (SELECT COUNT(*) FROM activity_observation) + (SELECT COUNT(*) FROM route_point) + (SELECT COUNT(*) FROM energy_estimate) + (SELECT COUNT(*) FROM day_settings)") suspend fun count(): Long
}
