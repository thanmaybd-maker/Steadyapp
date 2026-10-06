package com.thanu.steady.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// All timestamps are UTC milliseconds; logical dates retain their original policy snapshot.
@Serializable @Entity(tableName = "plan_item", indices = [Index("day"), Index("subjectId")])
data class PlanItem(@PrimaryKey val id: String, val day: String, val title: String, val notes: String = "",
    val category: String = "GENERAL", val subjectId: String? = null, val projectId: String? = null,
    val plannedSeconds: Long? = null, val timeMinutes: Int? = null, val priority: Int = 1,
    val state: String = "PENDING", val essential: Boolean = false, val position: Int = 0,
    val created: Long, val updated: Long, val zone: String, val boundary: Int)

@Serializable @Entity(tableName = "habit_definition")
data class HabitDefinition(@PrimaryKey val id: String, val created: Long, val archivedDay: String? = null)

@Serializable @Entity(tableName = "habit_version", indices = [Index(value = ["habitId", "effectiveDay"], unique = true)])
data class HabitVersion(@PrimaryKey val id: String, val habitId: String, val effectiveDay: String,
    val title: String, val type: String, val unit: String, val target: Double,
    val weekdays: Int = 127, val everyDays: Int = 1, val anchorDay: String,
    val essential: Boolean = false, val reminderMinute: Int? = null, val created: Long)

@Serializable @Entity(tableName = "habit_occurrence", indices = [Index(value = ["habitId", "day"], unique = true), Index("day")])
data class HabitOccurrence(@PrimaryKey val id: String, val habitId: String, val versionId: String,
    val day: String, val state: String = "PENDING", val quantity: Double = 0.0,
    val notes: String = "", val updated: Long)

@Serializable @Entity(tableName = "habit_log", indices = [Index("occurrenceId")])
data class HabitLog(@PrimaryKey val id: String, val occurrenceId: String, val quantity: Double,
    val at: Long, val undoneAt: Long? = null, val source: String = "USER")

@Serializable @Entity(tableName = "subject")
data class Subject(@PrimaryKey val id: String, val title: String, val code: String = "", val archived: Boolean = false)

@Serializable @Entity(tableName = "topic", indices = [Index("subjectId")])
data class Topic(@PrimaryKey val id: String, val subjectId: String, val title: String, val state: String = "NEW")

@Serializable @Entity(tableName = "activity_session", indices = [Index("started"), Index("state")])
data class ActivitySession(@PrimaryKey val id: String, val type: String, val kind: String,
    val title: String, val subjectId: String? = null, val taskId: String? = null,
    val state: String = "IDLE", val plannedSeconds: Long? = null, val breakSeconds: Long = 0,
    val activeMillis: Long = 0, val wallAnchor: Long? = null, val elapsedAnchor: Long? = null,
    val deadlineElapsed: Long? = null, val boot: Long = 0, val generation: Int = 0,
    val started: Long? = null, val ended: Long? = null, val notes: String = "",
    val effort: Int? = null, val zone: String, val boundary: Int, val source: String = "USER",
    val cueFlags: Int = 3, val updated: Long)

@Serializable @Entity(tableName = "activity_segment", indices = [Index("sessionId"), Index("startWall")])
data class ActivitySegment(@PrimaryKey val id: String, val sessionId: String, val startWall: Long,
    val endWall: Long? = null, val elapsedStart: Long, val elapsedEnd: Long? = null,
    val activeMillis: Long = 0, val zone: String, val boundary: Int)

@Serializable @Entity(tableName = "session_note", indices = [Index("sessionId")])
data class SessionNote(@PrimaryKey val id: String, val sessionId: String?, val text: String, val savedAt: Long)

@Serializable @Entity(tableName = "exercise_set", indices = [Index("sessionId")])
data class ExerciseSet(@PrimaryKey val id: String, val sessionId: String, val exercise: String,
    val reps: Int, val load: Double? = null, val unit: String = "kg", val doneAt: Long,
    val notes: String = "", val position: Int = 0)

@Serializable @Entity(tableName = "workout_template")
data class WorkoutTemplate(@PrimaryKey val id: String, val title: String, val mode: String,
    val exercises: String = "", val workSeconds: Int = 30, val restSeconds: Int = 30,
    val rounds: Int = 1, val warmupSeconds: Int = 0, val cooldownSeconds: Int = 0)

@Serializable @Entity(tableName = "water_log", indices = [Index("day")])
data class WaterLog(@PrimaryKey val id: String, val day: String, val millilitres: Int, val at: Long,
    val zone: String, val boundary: Int, val source: String = "USER")

@Serializable @Entity(tableName = "sleep_log", indices = [Index("day")])
data class SleepLog(@PrimaryKey val id: String, val day: String, val bedtime: Long, val wake: Long,
    val restedness: Int? = null, val notes: String = "", val zone: String, val boundary: Int,
    val source: String = "USER")

@Serializable @Entity(tableName = "food_idea")
data class FoodIdeaRecord(@PrimaryKey val id: String, val title: String, val ingredients: String,
    val instructions: String, val prepMinutes: Int? = null, val budget: String = "",
    val tags: String = "", val vegetarian: Boolean = true, val favorite: Boolean = false,
    val provenance: String = "USER", val reviewedDay: String? = null, val updated: Long)

@Serializable @Entity(tableName = "meal_log", indices = [Index("day")])
data class MealLog(@PrimaryKey val id: String, val day: String, val title: String, val notes: String = "",
    val foodId: String? = null, val at: Long, val zone: String, val boundary: Int)

@Serializable @Entity(tableName = "care_reminder")
data class CareReminder(@PrimaryKey val id: String, val instruction: String, val weekdays: Int = 127,
    val minute: Int, val enabled: Boolean = true, val updated: Long)

@Serializable @Entity(tableName = "care_log", indices = [Index("day")])
data class CareLog(@PrimaryKey val id: String, val reminderId: String, val day: String,
    val instructionSnapshot: String, val at: Long)

@Serializable @Entity(tableName = "reflection", indices = [Index(value = ["startDay", "endDay"], unique = true)])
data class Reflection(@PrimaryKey val id: String, val startDay: String, val endDay: String,
    val helped: String = "", val demanding: String = "", val evidence: String = "",
    val adjustment: String = "", val highlight: String = "", val obstacle: String = "",
    val tomorrow: String = "", val mood: Int? = null, val energy: Int? = null, val updated: Long)

@Serializable @Entity(tableName = "capture")
data class Capture(@PrimaryKey val id: String, val text: String, val created: Long,
    val archived: Boolean = false, val projectId: String? = null)

@Serializable @Entity(tableName = "interruption_event", indices = [Index("at")])
data class InterruptionEvent(@PrimaryKey val id: String, val mode: String = "VOLUNTARY",
    val outcome: String, val appPackage: String? = null, val at: Long, val pauseSeconds: Int)

@Serializable @Entity(tableName = "activity_observation", indices = [Index(value = ["sourceKey"], unique = true), Index("day")])
data class ActivityObservation(@PrimaryKey val id: String, val sourceKey: String, val day: String,
    val start: Long, val end: Long, val steps: Long? = null, val distanceMetres: Double? = null,
    val source: String, val quality: String, val boot: Long, val zone: String, val boundary: Int)

@Serializable @Entity(tableName = "route_point", indices = [Index(value = ["sessionId", "at"], unique = true)])
data class RoutePoint(@PrimaryKey val id: String, val sessionId: String, val at: Long,
    val latitude: Double, val longitude: Double, val accuracyMetres: Float, val segment: Int)

@Serializable @Entity(tableName = "energy_estimate", indices = [Index(value = ["sessionId"], unique = true)])
data class EnergyEstimate(@PrimaryKey val id: String, val sessionId: String, val kilograms: Double,
    val met: Double, val activeKcal: Double, val grossKcal: Double, val method: String,
    val reference: String, val at: Long)

@Serializable @Entity(tableName = "day_settings")
data class DaySettings(@PrimaryKey val day: String, val mode: String, val nextAction: String = "",
    val zone: String, val boundary: Int, val shutdown: Long? = null, val updated: Long)

@Serializable @Entity(tableName = "expanded_profile")
data class ExpandedProfile(@PrimaryKey val id: Int = 1, val displayName: String = "",
    val onboarded: Boolean = false, val country: String = "IN", val palette: String = "KINETIC",
    val theme: String = "SYSTEM", val highContrast: Boolean = false, val reducedMotion: Boolean = false,
    val textScale: Float = 1f, val modules: String = "PLAN,HABITS,FOCUS,MOVEMENT,FOOD,WATER,SLEEP",
    val dashboard: String = "NEXT,RINGS,ROUTINES,TIMELINE,HABITS,CAPTURE,FOOD", val wideCards: String = "NEXT",
    val focusTargetMinutes: Int? = null, val waterTargetMl: Int? = null, val stepTarget: Long? = null,
    val weightKg: Double? = null, val quietStart: Int = 1320, val quietEnd: Int = 420,
    val alertBudget: Int = 5, val dateStyle: String = "LOCAL", val zoneMode: String = "FIXED")
