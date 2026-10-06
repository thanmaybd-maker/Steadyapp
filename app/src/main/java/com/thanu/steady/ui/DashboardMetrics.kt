package com.thanu.steady.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.data.PeriodSnapshot

fun metricLabel(key: String): Int = when(key) {
    "STEPS" -> R.string.steps_metric; "FOCUS" -> R.string.focus_metric
    "HABITS" -> R.string.habits_metric; "WATER" -> R.string.water_title
    "WORKOUTS" -> R.string.movement_title; else -> R.string.sleep_title
}
@Composable fun waterAmount(ml: Long, unit: String): String = if(unit == "FLOZ")
    stringResource(R.string.water_fluid_ounces,ml / 29.5735295625,ml) else stringResource(R.string.water_serving,ml)

@Composable fun DashboardMetrics(period: PeriodSnapshot, onFocus: () -> Unit, onHealth: () -> Unit, onHabits: () -> Unit) {
    val due = period.occurrences.filter { it.state != "SKIPPED" }
    val quantities = period.profile.ringMetrics.split(',').map { key -> when (key) {
        "FOCUS" -> focusMillis(period) / 60_000.0 to period.profile.focusTargetMinutes?.toDouble()
        "HABITS" -> due.count { it.state == "COMPLETED" }.toDouble() to due.size.takeIf { it > 0 }?.toDouble()
        "WATER" -> period.water.sumOf { it.millilitres.toLong() }.toDouble() to period.profile.waterTargetMl?.toDouble()
        "STEPS" -> period.observations.filter { it.steps != null && it.source == "PHONE_STEP_COUNTER" }
            .takeIf { it.isNotEmpty() }?.sumOf { it.steps ?: 0 }?.toDouble() to period.profile.stepTarget?.toDouble()
        "WORKOUTS" -> activityMillis(period, "WORKOUT") / 60_000.0 to null
        else -> period.sleep.takeIf { it.isNotEmpty() }?.sumOf { it.wake - it.bedtime }?.div(60_000.0) to null
    } }
    ConcentricSummary(quantities, period.profile.reducedMotion)
    period.profile.ringMetrics.split(',').forEach { key ->
        when(key) {
            "STEPS" -> {
                val observations = period.observations.filter { it.steps != null && it.source == "PHONE_STEP_COUNTER" }
                val steps = observations.takeIf { it.isNotEmpty() }?.sumOf { it.steps ?: 0 }
                MetricRing(metricLabel(key),steps?.toDouble(),period.profile.stepTarget?.toDouble(),
                    if(steps == null) stringResource(R.string.steps_unavailable) else stringResource(R.string.steps_observed,steps),onHealth)
            }
            "FOCUS" -> MetricRing(metricLabel(key),focusMillis(period)/60_000.0,period.profile.focusTargetMinutes?.toDouble(),
                stringResource(R.string.focus_actual_minutes,focusMillis(period)/60_000.0),onFocus)
            "HABITS" -> {
                val due = period.occurrences.filter { it.state != "SKIPPED" }
                val done = due.count { it.state == "COMPLETED" }
                MetricRing(metricLabel(key),done.toDouble(),due.size.takeIf { it > 0 }?.toDouble(),
                    if(due.isEmpty()) stringResource(R.string.no_habits_due) else stringResource(R.string.habits_count,done,due.size),onHabits)
            }
            "WATER" -> {
                val ml = period.water.sumOf { it.millilitres.toLong() }
                MetricRing(metricLabel(key),ml.toDouble(),period.profile.waterTargetMl?.toDouble(),waterAmount(ml,period.profile.waterUnit),onHealth)
            }
            "WORKOUTS" -> MetricRing(metricLabel(key),activityMillis(period,"WORKOUT")/60_000.0,null,
                stringResource(R.string.workout_actual_minutes,activityMillis(period,"WORKOUT")/60_000.0),onHealth)
            "SLEEP" -> {
                val minutes = period.sleep.sumOf { it.wake-it.bedtime }/60_000.0
                MetricRing(metricLabel(key),if(period.sleep.isEmpty()) null else minutes,null,
                    if(period.sleep.isEmpty()) stringResource(R.string.sleep_empty) else stringResource(R.string.sleep_duration,minutes),onHealth)
            }
        }
    }
}
