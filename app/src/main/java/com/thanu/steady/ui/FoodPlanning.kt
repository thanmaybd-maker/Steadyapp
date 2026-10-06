package com.thanu.steady.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.data.FoodIdeaRecord
import com.thanu.steady.domain.*
import kotlinx.coroutines.*
import java.time.LocalTime

val mealTypeChoices = listOf("" to R.string.meal_type_optional,"BREAKFAST" to R.string.meal_breakfast,
    "LUNCH" to R.string.meal_lunch,"DINNER" to R.string.meal_dinner,"SNACK" to R.string.meal_snack)

@Composable fun RecipeContextSummary(model: ExpandedViewModel, recipe: FoodIdeaRecord, saving: Boolean) {
    var context by remember(recipe.id) { mutableStateOf<RecipeContext?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    LaunchedEffect(recipe.id,recipe.updated,saving,retry) {
        try { context=withContext(Dispatchers.IO) { model.repository.recipeContext(recipe.id) }; failed=false }
        catch(cancelled: CancellationException) { throw cancelled }
        catch(_: Exception) { failed=true }
    }
    if(failed) { Text(stringResource(R.string.recipe_context_unavailable)); SecondaryAction(R.string.retry) { retry++ } }
    context?.let { value ->
        value.mealType?.let { type -> Text(stringResource(mealTypeChoices.first { it.first == type }.second)) }
        if(value.context.isNotBlank()) Text(value.context)
    }
}

@Composable fun MealPlanEditor(model: ExpandedViewModel,state: ExpandedUiState,recipe: FoodIdeaRecord,onSafety: () -> Unit,onClose: () -> Unit) {
    val period = state.period ?: return
    val key = "meal-plan:${recipe.id}"
    val initialId = remember(recipe.id) { model.repository.newId() }
    DraftEditor(model,state,key,R.string.schedule_meal,mapOf("id" to initialId,"day" to period.end.toString(),
        "time" to "","minutes" to (recipe.prepMinutes?.coerceAtLeast(1)?.toString() ?: "15"),"title" to recipe.title,
        "notes" to (recipe.ingredients + "\n\n" + recipe.instructions)),onSafety,onClose) { form,close ->
        Text(stringResource(R.string.meal_plan_notice))
        TextInput(form["title"].orEmpty(),R.string.task_title,{ model.field(key,"title",it) })
        TextInput(form["day"].orEmpty(),R.string.date_iso,{ model.field(key,"day",it) })
        TextInput(form["time"].orEmpty(),R.string.time_optional,{ model.field(key,"time",it) })
        TextInput(form["minutes"].orEmpty(),R.string.duration_minutes,{ model.field(key,"minutes",it) })
        TextInput(form["notes"].orEmpty(),R.string.note_text,{ model.field(key,"notes",it) },3)
        PrimaryAction(R.string.add_food_plan,!state.busy) {
            model.action({
                val minute = form["time"]?.takeIf(String::isNotBlank)?.let { LocalTime.parse(it).let { time -> time.hour * 60 + time.minute } }
                model.repository.adoptMeal(MealPlanProposal(form.getValue("id"),recipe.id,form.getValue("day"),form.getValue("title"),
                    form["notes"].orEmpty(),minute,form.getValue("minutes").toInt(),period.preferences.zoneId,period.preferences.boundaryMinutes))
            },after=close)
        }
    }
}
