package com.thanu.steady.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.ActivityState
import com.thanu.steady.domain.IntervalProgram
import com.thanu.steady.domain.RecipeContext
import com.thanu.steady.domain.PantryRules
import kotlinx.coroutines.*
import java.time.*

val workoutModes = listOf("WALKING" to R.string.walking_mode, "RUNNING" to R.string.running_mode,
    "CYCLING" to R.string.cycling_mode, "STRENGTH" to R.string.strength_mode, "INTERVALS" to R.string.intervals_mode,
    "MOBILITY" to R.string.mobility_mode, "CUSTOM" to R.string.custom_mode)

@Composable fun ExpandedHealth(model: ExpandedViewModel, state: ExpandedUiState, onSafety: () -> Unit,
    quickAction: String? = null, onQuickActionHandled: () -> Unit = {}) {
    val period = state.period ?: return
    var editor by remember { mutableStateOf<String?>(null) }
    var food by remember { mutableStateOf<FoodIdeaRecord?>(null) }
    var water by remember { mutableStateOf<WaterLog?>(null) }
    var sleep by remember { mutableStateOf<SleepLog?>(null) }
    var care by remember { mutableStateOf<CareReminder?>(null) }
    var sets by remember { mutableStateOf<List<ExerciseSet>>(emptyList()) }
    var templates by remember { mutableStateOf<List<WorkoutTemplate>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf<WorkoutTemplate?>(null) }
    var selectedSet by remember { mutableStateOf<ExerciseSet?>(null) }
    var restSeconds by remember { mutableStateOf("60") }
    var undoneWater by remember { mutableStateOf<WaterLog?>(null) }
    var quickWater by remember { mutableStateOf<WaterLog?>(null) }
    LaunchedEffect(quickAction) { if(quickAction != null) { water = null; sleep = null; editor = quickAction; onQuickActionHandled() } }
    val active = period.active.firstOrNull()
    LaunchedEffect(active?.id, state.busy) {
        try { if (active?.type == "WORKOUT") sets = model.sets(active.id); templates = model.templates() }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { model.action({ error("Storage unavailable") }) }
    }
    ExpandedPage {
        StateMessages(state)
        SectionCard(R.string.day_horizon_title) { TaskTimeline(period, {}, {}, model.repository.clock, includeActions = false) }
        if (period.profile.modules.contains("MOVEMENT")) SectionCard(R.string.movement_title) {
            Text(stringResource(R.string.manual_workout_description))
            PrimaryAction(R.string.start_workout) { editor = "workout" }
            SecondaryAction(R.string.log_manual_workout) { editor = "manual_workout" }
            SecondaryAction(R.string.add_workout_template) { selectedTemplate = null; editor = "template" }
            templates.forEach { template ->
                Text(template.title, style = MaterialTheme.typography.titleMedium)
                if(template.exercises.isNotBlank()) Text(template.exercises)
                PrimaryAction(R.string.start_template, !state.busy && active == null) { model.action({
                    val program = if(template.mode == "INTERVALS") IntervalProgram(template.workSeconds,template.restSeconds,template.rounds,
                        template.warmupSeconds,template.cooldownSeconds) else null
                    model.startActivity("WORKOUT",template.mode,template.title,program?.totalSeconds,0,
                        notes = template.exercises, program = program)
                }) }
                SecondaryAction(R.string.edit_action) { selectedTemplate = template; editor = "template" }
                SecondaryAction(R.string.delete_action, !state.busy) { model.deleteTemplate(template.id) }
            }
            if (active?.type == "WORKOUT") {
                Text(active.title)
                Text(stringResource(R.string.actual_seconds, state.activeMillis / 1000))
                state.interval?.let { program ->
                    val phase = program.phase(state.activeMillis)
                    Text(stringResource(R.string.interval_phase, stringResource(when(phase.kind) {
                        "WARMUP" -> R.string.warmup_phase; "WORK" -> R.string.work_phase; "REST" -> R.string.rest_phase
                        "COOLDOWN" -> R.string.cooldown_phase; else -> R.string.completed
                    }),phase.round,program.rounds,phase.remainingSeconds))
                }
                ActiveEffort(model,state,active)
                PrimaryAction(if (active.state == "RUNNING") R.string.timer_pause else R.string.timer_resume, !state.busy) {
                    model.transition(if (active.state == "RUNNING") ActivityState.PAUSED else ActivityState.RUNNING)
                }
                PrimaryAction(R.string.save_partial, !state.busy) { model.transition(ActivityState.STOPPED) }
                SecondaryAction(R.string.discard_session, !state.busy) { model.transition(ActivityState.DISCARDED) }
                SecondaryAction(R.string.safety_action, onClick = onSafety)
                if (active.kind == "STRENGTH") {
                    PrimaryAction(R.string.add_set) { selectedSet = null; editor = "set" }
                    sets.forEach { set ->
                        Text(stringResource(R.string.exercise_set_summary, set.exercise, set.reps, set.load?.toString() ?: stringResource(R.string.bodyweight), set.unit))
                        SecondaryAction(R.string.edit_action) { selectedSet = set; editor = "set" }
                        SecondaryAction(R.string.delete_action, !state.busy) { model.deleteSet(set.id) }
                    }
                    TextInput(restSeconds,R.string.rest_seconds,{ restSeconds = it })
                    PrimaryAction(R.string.start_rest,!state.busy && active.state == "RUNNING") { model.action({
                        val seconds = restSeconds.toInt(); require(seconds in 1..3600)
                        model.beginRest(active.id,seconds)
                    }) }
                    state.rest?.let { rest ->
                        Text(stringResource(if(rest.complete) R.string.rest_complete else R.string.rest_remaining, state.restRemaining / 1000))
                        SecondaryAction(R.string.cancel, !state.busy) { model.cancelRest(active.id) }
                    }
                }
            } else if (active != null) {
                Text(stringResource(R.string.focus_workout_conflict))
                SecondaryAction(R.string.save_current_focus, !state.busy) { model.transition(ActivityState.STOPPED) }
                SecondaryAction(R.string.discard_session, !state.busy) { model.transition(ActivityState.DISCARDED) }
            }
            if (period.observations.isEmpty()) Text(stringResource(R.string.motion_unavailable))
            period.sessions.filter { it.type == "WORKOUT" && it.state in setOf("STOPPED", "COMPLETED") && it.activeMillis > 0 }.forEach { session ->
                Text(session.title.ifBlank { stringResource(workoutModes.firstOrNull { it.first == session.kind }?.second ?: R.string.custom_mode) })
                Text(stringResource(R.string.actual_seconds, session.activeMillis / 1000))
                Text(stringResource(R.string.user_entered_source))
                SecondaryAction(R.string.edit_action) { editor = "history:${session.id}" }
                SecondaryAction(R.string.delete_action, !state.busy) { model.deleteHistory(session.id) }
            }
        }
        if (period.profile.modules.contains("WATER")) SectionCard(R.string.water_title) {
            val totalMl = period.water.sumOf { it.millilitres.toLong() }
            AnimatedWaterFill(totalMl, period.profile.waterTargetMl, period.profile.waterUnit, period.profile.reducedMotion)
            PrimaryAction(R.string.log_water) { water = null; editor = "water" }
            com.thanu.steady.domain.PersonalizationRules.waterQuantities(period.profile.waterQuickMl).forEach { ml ->
                androidx.compose.material3.OutlinedButton(onClick = {
                    val zone = period.preferences.zoneId; val boundary = period.preferences.boundaryMinutes
                    val entry = WaterLog(model.repository.newId(),period.end.toString(),ml,model.repository.clock.millis(),zone,boundary)
                    model.action({ model.repository.saveWater(entry) },after = { quickWater = entry })
                },enabled = !state.busy,modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.quick_water,waterAmount(ml.toLong(),period.profile.waterUnit)))
                }
            }
            quickWater?.let { entry -> SecondaryAction(R.string.undo_water_log,!state.busy) {
                model.action({ model.repository.deleteWater(entry.id) },after = { quickWater = null })
            } }
            period.water.forEach { entry ->
                Text(stringResource(R.string.water_serving, entry.millilitres))
                SecondaryAction(R.string.edit_action) { water = entry; editor = "water" }
                SecondaryAction(R.string.delete_action, !state.busy) { model.action({ model.repository.deleteWater(entry.id) }, after = { undoneWater = entry }) }
            }
            undoneWater?.let { entry -> SecondaryAction(R.string.undo_action, !state.busy) { model.action({ model.repository.saveWater(entry) }, after = { undoneWater = null }) } }
        }
        if (period.profile.modules.contains("SLEEP")) SectionCard(R.string.sleep_title) {
            PrimaryAction(R.string.log_sleep) { sleep = null; editor = "sleep" }
            if (period.sleep.isEmpty()) Text(stringResource(R.string.sleep_empty))
            period.sleep.forEach { entry ->
                Text(stringResource(R.string.sleep_duration, (entry.wake - entry.bedtime) / 60_000.0))
                Text(stringResource(R.string.user_entered_source))
                SecondaryAction(R.string.edit_action) { sleep = entry; editor = "sleep" }
                SecondaryAction(R.string.delete_action, !state.busy) { model.action({ model.repository.deleteSleep(entry.id) }) }
            }
        }
        if (period.profile.modules.contains("FOOD")) SectionCard(R.string.food_title) {
            CognitiveNutritionCard(period)
            Text(stringResource(R.string.food_filter_disclosure))
            PrimaryAction(R.string.add_food) { food = null; editor = "food" }
            val avoid = period.preferences.avoidFoods.split(',').map { it.trim().lowercase(java.util.Locale.ROOT) }.filter(String::isNotBlank)
            val foods = period.foods.filter { (!period.preferences.vegetarian || it.vegetarian) && avoid.none { tag ->
                (it.ingredients + "," + it.tags).lowercase(java.util.Locale.ROOT).contains(tag) } }
            if (foods.isEmpty()) Text(stringResource(R.string.food_empty))
            foods.forEach { recipe ->
                Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                RecipeContextSummary(model,recipe,state.busy)
                Text(recipe.ingredients); Text(recipe.instructions)
                recipe.prepMinutes?.let { Text(stringResource(R.string.preparation_minutes, it)) }
                Text(recipe.budget)
                PrimaryAction(if (recipe.favorite) R.string.unfavorite_food else R.string.favorite_food, !state.busy) {
                    model.action({ model.repository.saveFood(recipe.copy(favorite = !recipe.favorite)) })
                }
                SecondaryAction(R.string.edit_action) { food = recipe; editor = "food" }
                SecondaryAction(R.string.schedule_meal) { food = recipe; editor = "meal_plan" }
                SecondaryAction(R.string.log_meal, !state.busy) { model.action({ model.repository.saveMeal(MealLog(model.repository.newId(), period.end.toString(), recipe.title,
                    foodId = recipe.id, at = model.repository.clock.millis(), zone = period.preferences.zoneId, boundary = period.preferences.boundaryMinutes)) }) }
                SecondaryAction(R.string.delete_action, !state.busy) { model.action({ model.repository.deleteFood(recipe.id) }) }
            }
            period.meals.forEach { meal -> Text(meal.title); SecondaryAction(R.string.delete_meal, !state.busy) { model.action({ model.repository.deleteMeal(meal.id) }) } }
        }
        if("FOOD" in period.profile.modules.split(',')) FoodReferenceCard(model,state)
        SectionCard(R.string.care_title) {
            Text(stringResource(R.string.care_disclosure))
            PrimaryAction(R.string.add_care) { care = null; editor = "care" }
            period.care.forEach { reminder ->
                Text(reminder.instruction)
                Text("%02d:%02d".format(java.util.Locale.ROOT, reminder.minute / 60, reminder.minute % 60))
                ToggleRow(R.string.reminder_enabled, reminder.enabled) { enabled -> model.action({ model.repository.saveCare(reminder.copy(enabled = enabled)) }) }
                SecondaryAction(R.string.edit_action) { care = reminder; editor = "care" }
                val logged = period.careLogs.any { it.reminderId == reminder.id }
                PrimaryAction(if (logged) R.string.completed else R.string.mark_care_done, !state.busy && !logged) { model.action({
                    model.repository.saveCareLog(CareLog(java.util.UUID.nameUUIDFromBytes("care|${reminder.id}|${period.end}".toByteArray()).toString(), reminder.id,
                        period.end.toString(), reminder.instruction, model.repository.clock.millis()))
                }) }
            }
        }
    }
    when {
        editor == "water" -> WaterEditor(model, state, water, onSafety) { editor = null }
        editor == "sleep" -> SleepEditor(model, state, sleep, onSafety) { editor = null }
        editor == "food" -> FoodEditor(model, state, food, onSafety) { editor = null }
        editor == "meal_plan" && food != null -> MealPlanEditor(model,state,food!!,onSafety) { editor = null }
        editor == "workout" || editor == "manual_workout" -> WorkoutEditor(model, state, editor == "manual_workout", onSafety) { editor = null }
        editor == "set" && active != null -> SetEditor(model, state, active.id, onSafety, selectedSet, sets.size) { editor = null }
        editor == "template" -> TemplateEditor(model,state,selectedTemplate,onSafety) { editor = null }
        editor?.startsWith("history:") == true -> period.sessions.firstOrNull { it.id == editor!!.substringAfter(':') }?.let { HistoryEditor(model, state, it, onSafety) { editor = null } }
        editor == "care" -> CareEditor(model,state,care,onSafety) { editor = null }
    }
}

@Composable fun WaterEditor(model: ExpandedViewModel, state: ExpandedUiState, original: WaterLog?, onSafety: () -> Unit, onClose: () -> Unit) {
    val period = state.period ?: return
    val key = "water:${original?.id ?: "new"}"
    var confirmLarge by remember { mutableStateOf(false) }
    DraftEditor(model, state, key, R.string.log_water, mapOf("amount" to (original?.millilitres?.toString() ?: "250"), "day" to (original?.day ?: period.end.toString())), onSafety, onClose) { values, close ->
        TextInput(values["amount"].orEmpty(), R.string.water_amount_ml, { model.field(key, "amount", it) })
        TextInput(values["day"].orEmpty(), R.string.logical_date, { model.field(key, "day", it) })
        ToggleRow(R.string.confirm_large_water, confirmLarge) { confirmLarge = it }
        PrimaryAction(R.string.save_action, !state.busy) { model.action({
            val amount = values["amount"]!!.toInt(); require(amount <= 2000 || confirmLarge)
            val day = LocalDate.parse(values["day"]!!)
            model.repository.saveWater(WaterLog(original?.id ?: model.repository.newId(), day.toString(), amount, original?.at ?: model.repository.clock.millis(),
                original?.zone ?: period.preferences.zoneId, original?.boundary ?: period.preferences.boundaryMinutes))
        }, after = close) }
    }
}

@Composable fun SleepEditor(model: ExpandedViewModel, state: ExpandedUiState, original: SleepLog?, onSafety: () -> Unit, onClose: () -> Unit) {
    val period = state.period ?: return
    val zone = ZoneId.of(original?.zone ?: period.preferences.zoneId)
    val key = "sleep:${original?.id ?: "new"}"
    DraftEditor(model, state, key, R.string.log_sleep, mapOf("bed" to (original?.bedtime?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime().toString() } ?: ""),
        "wake" to (original?.wake?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime().toString() } ?: ""), "rested" to (original?.restedness?.toString() ?: ""),
        "notes" to original?.notes.orEmpty()), onSafety, onClose) { values, close ->
        TextInput(values["bed"].orEmpty(), R.string.sleep_bedtime, { model.field(key, "bed", it) }, supporting = stringResource(R.string.datetime_example))
        TextInput(values["wake"].orEmpty(), R.string.sleep_wake, { model.field(key, "wake", it) }, supporting = stringResource(R.string.datetime_example))
        TextInput(values["rested"].orEmpty(), R.string.restedness_optional, { model.field(key, "rested", it) })
        TextInput(values["notes"].orEmpty(), R.string.note_text, { model.field(key, "notes", it) }, 2)
        PrimaryAction(R.string.save_action, !state.busy) { model.action({
            val bed = LocalDateTime.parse(values["bed"]!!).atZone(zone).toInstant()
            val wake = LocalDateTime.parse(values["wake"]!!).atZone(zone).toInstant()
            val boundary = original?.boundary ?: period.preferences.boundaryMinutes
            val day = com.thanu.steady.domain.LogicalDayPolicy().getLogicalDay(wake, zone, boundary)
            model.repository.saveSleep(SleepLog(original?.id ?: model.repository.newId(), day.toString(), bed.toEpochMilli(), wake.toEpochMilli(),
                values["rested"]?.takeIf(String::isNotBlank)?.toInt(), values["notes"].orEmpty(), zone.id, boundary))
        }, after = close) }
    }
}

@Composable fun FoodEditor(model: ExpandedViewModel, state: ExpandedUiState, original: FoodIdeaRecord?, onSafety: () -> Unit, onClose: () -> Unit) {
    val key = "food:${original?.id ?: "new"}"
    val newId = remember(key) { original?.id ?: model.repository.newId() }
    var context by remember(original?.id) { mutableStateOf<RecipeContext?>(if(original == null) RecipeContext() else null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    LaunchedEffect(original?.id,retry) {
        if(original != null) try { context = withContext(Dispatchers.IO) { model.repository.recipeContext(original.id) }; failed=false }
        catch(cancelled: CancellationException) { throw cancelled }
        catch(_: Exception) { failed=true }
    }
    val initialContext = context
    if(initialContext == null) {
        androidx.compose.ui.window.Dialog(onDismissRequest=onClose,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
            DialogSurface { ExpandedPage {
                SecondaryAction(R.string.safety_action,onClick=onSafety); SecondaryAction(R.string.close_keep_draft,onClick=onClose)
                Text(stringResource(if(failed) R.string.recipe_context_unavailable else R.string.loading_records))
                if(failed) SecondaryAction(R.string.retry) { retry++ }
            } }
        }
        return
    }
    DraftEditor(model, state, key, R.string.food_editor, mapOf("id" to newId,"title" to original?.title.orEmpty(), "ingredients" to original?.ingredients.orEmpty(),
        "steps" to original?.instructions.orEmpty(), "prep" to (original?.prepMinutes?.toString() ?: ""), "budget" to original?.budget.orEmpty(),
        "tags" to original?.tags.orEmpty(), "vegetarian" to (original?.vegetarian?.toString() ?: "true"),
        "mealType" to initialContext.mealType.orEmpty(),"context" to initialContext.context), onSafety, onClose) { values, close ->
        Text(stringResource(R.string.pantry_choices))
        listOf(R.string.pantry_rice,R.string.pantry_lentils,R.string.pantry_chickpeas,R.string.pantry_eggs,
            R.string.pantry_yogurt,R.string.pantry_vegetables,R.string.pantry_fruit).forEach { label ->
            val item = stringResource(label)
            ToggleRow(label,PantryRules.selected(values["ingredients"].orEmpty(),item)) {
                model.field(key,"ingredients",PantryRules.toggle(values["ingredients"].orEmpty(),item,it))
            }
        }
        Text(stringResource(R.string.pantry_custom_notice))
        listOf(5,10,15,25).forEach { minutes ->
            OutlinedButton(onClick={ model.field(key,"prep",minutes.toString()) },modifier=Modifier.fillMaxWidth().heightIn(min=56.dp)) {
                Text(stringResource(R.string.preparation_minutes,minutes))
            }
        }
        ChoiceList(values["mealType"].orEmpty(),mealTypeChoices) { model.field(key,"mealType",it) }
        TextInput(values["context"].orEmpty(),R.string.meal_context,{ model.field(key,"context",it) },2)
        listOf("title" to R.string.food_name, "ingredients" to R.string.food_ingredients, "steps" to R.string.food_instructions,
            "prep" to R.string.preparation_minutes_field, "budget" to R.string.budget_tag, "tags" to R.string.food_tags).forEach { (field, label) ->
            TextInput(values[field].orEmpty(), label, { model.field(key, field, it) }, if (field in setOf("ingredients", "steps")) 3 else 1)
        }
        ToggleRow(R.string.vegetarian_food, values["vegetarian"] == "true") { model.field(key, "vegetarian", it.toString()) }
        PrimaryAction(R.string.save_action, !state.busy) { model.action({
            model.repository.saveRecipe(FoodIdeaRecord(original?.id ?: values["id"] ?: newId, values["title"].orEmpty(), values["ingredients"].orEmpty(),
                values["steps"].orEmpty(), values["prep"]?.takeIf(String::isNotBlank)?.toInt(), values["budget"].orEmpty(), values["tags"].orEmpty(),
                values["vegetarian"] == "true", original?.favorite ?: false, original?.provenance ?: "USER", original?.reviewedDay, model.repository.clock.millis()),
                RecipeContext(values["mealType"]?.takeIf(String::isNotBlank),values["context"].orEmpty()))
        }, after = close) }
    }
}

@Composable fun WorkoutEditor(model: ExpandedViewModel, state: ExpandedUiState, manual: Boolean, onSafety: () -> Unit, onClose: () -> Unit) {
    val period = state.period ?: return
    val key = if (manual) "manual_workout:new" else "workout:new"
    DraftEditor(model, state, key, if (manual) R.string.log_manual_workout else R.string.start_workout,
        mapOf("kind" to "WALKING", "title" to "", "minutes" to "", "notes" to "", "work" to "30", "rest" to "30",
            "rounds" to "4", "warmup" to "0", "cooldown" to "0"), onSafety, onClose) { values, close ->
        ChoiceList(values["kind"] ?: "WALKING", workoutModes) { model.field(key, "kind", it) }
        TextInput(values["title"].orEmpty(), R.string.workout_name, { model.field(key, "title", it) })
        TextInput(values["minutes"].orEmpty(), if (manual) R.string.actual_minutes else R.string.duration_minutes_optional, { model.field(key, "minutes", it) })
        TextInput(values["notes"].orEmpty(), R.string.note_text, { model.field(key, "notes", it) }, 2)
        if(!manual && values["kind"] == "INTERVALS") IntervalFields(model,key,values)
        Text(stringResource(R.string.user_entered_source))
        if (period.active.isNotEmpty()) Text(stringResource(R.string.focus_workout_conflict))
        PrimaryAction(if (manual) R.string.save_action else R.string.timer_start, !state.busy && period.active.isEmpty()) { model.action({
            if (manual) model.manualWorkout(values["kind"] ?: "WALKING", values["title"].orEmpty(), values["minutes"]!!.toDouble(), values["notes"].orEmpty())
            else {
                val program = if(values["kind"] == "INTERVALS") intervalFrom(values) else null
                model.startActivity("WORKOUT", values["kind"] ?: "WALKING", values["title"].orEmpty(),
                    program?.totalSeconds ?: values["minutes"]?.takeIf(String::isNotBlank)?.toLong()?.times(60), 0,
                    notes = values["notes"].orEmpty(), program = program)
            }
        }, after = close) }
    }
}

@Composable fun SetEditor(model: ExpandedViewModel, state: ExpandedUiState, sessionId: String, onSafety: () -> Unit,
    original: ExerciseSet? = null, position: Int = 0, onClose: () -> Unit) {
    val key = "set:$sessionId:${original?.id ?: "new"}"
    DraftEditor(model, state, key, R.string.add_set, mapOf("exercise" to original?.exercise.orEmpty(), "reps" to (original?.reps?.toString() ?: ""),
        "load" to (original?.load?.toString() ?: ""), "unit" to (original?.unit ?: "kg"), "notes" to original?.notes.orEmpty()), onSafety, onClose) { values, close ->
        TextInput(values["exercise"].orEmpty(), R.string.exercise_name, { model.field(key, "exercise", it) })
        TextInput(values["reps"].orEmpty(), R.string.repetitions, { model.field(key, "reps", it) })
        TextInput(values["load"].orEmpty(), R.string.load_optional, { model.field(key, "load", it) })
        ChoiceList(values["unit"] ?: "kg", listOf("kg" to R.string.kilograms, "lb" to R.string.pounds)) { model.field(key, "unit", it) }
        TextInput(values["notes"].orEmpty(),R.string.note_text,{ model.field(key,"notes",it) },2)
        PrimaryAction(R.string.save_action, !state.busy) { model.action({
            model.saveSet(ExerciseSet(original?.id ?: model.repository.newId(), sessionId, values["exercise"].orEmpty(),
                values["reps"]!!.toInt(), values["load"]?.takeIf(String::isNotBlank)?.toDouble(), values["unit"] ?: "kg",
                original?.doneAt ?: model.repository.clock.millis(),values["notes"].orEmpty(),original?.position ?: position))
        }, after = close) }
    }
}

@Composable fun HistoryEditor(model: ExpandedViewModel, state: ExpandedUiState, session: ActivitySession, onSafety: () -> Unit, onClose: () -> Unit) {
    val key = "history:${session.id}"
    DraftEditor(model, state, key, R.string.edit_session, mapOf("seconds" to (session.activeMillis / 1000).toString(), "notes" to session.notes,
        "effort" to (session.effort?.toString() ?: "")), onSafety, onClose) { values, close ->
        TextInput(values["seconds"].orEmpty(), R.string.actual_seconds_field, { model.field(key, "seconds", it) })
        TextInput(values["notes"].orEmpty(), R.string.note_text, { model.field(key, "notes", it) }, 3)
        TextInput(values["effort"].orEmpty(), R.string.effort_optional, { model.field(key, "effort", it) })
        PrimaryAction(R.string.save_action, !state.busy) { model.action({ model.correctHistory(session.id,
            values["seconds"]!!.toLong().times(1000), values["notes"].orEmpty(), values["effort"]?.takeIf(String::isNotBlank)?.toInt()) }, after = close) }
    }
}
