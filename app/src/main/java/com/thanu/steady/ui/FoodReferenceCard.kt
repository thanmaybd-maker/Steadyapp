package com.thanu.steady.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.data.FoodIdeaRecord

/** Original assembly ideas, not personal fixtures or externally copied recipes. */
@Composable fun FoodReferenceCard(model: ExpandedViewModel,state: ExpandedUiState) {
    SectionCard(R.string.food_reference_title) {
        Text(stringResource(R.string.food_reference_reviewed))
        Text(stringResource(R.string.food_reference_calcium))
        Text(stringResource(R.string.food_reference_iron))
        Text(stringResource(R.string.food_reference_vitamin_c))
        Text(stringResource(R.string.food_reference_sources))
        Text(stringResource(R.string.food_reference_urls))
        listOf(Triple(R.string.food_example_title,R.string.food_example_ingredients,R.string.food_example_steps),
            Triple(R.string.food_fruit_title,R.string.food_fruit_ingredients,R.string.food_fruit_steps),
            Triple(R.string.food_toast_title,R.string.food_toast_ingredients,R.string.food_toast_steps),
            Triple(R.string.food_leftovers_title,R.string.food_leftovers_ingredients,R.string.food_leftovers_steps)).forEach { (title,ingredients,instructions) ->
            val name = stringResource(title); val ingredientText = stringResource(ingredients); val steps = stringResource(instructions)
            val budget = stringResource(R.string.food_example_budget)
            Text(name); Text(ingredientText)
            SecondaryAction(R.string.add_editable_food_example,!state.busy) { model.action({
                model.repository.saveFood(FoodIdeaRecord(model.repository.newId(),name,ingredientText,steps,budget = budget,
                    provenance = "STEADY_ORIGINAL_IDEA",reviewedDay = "2026-10-06",updated = model.repository.clock.millis()))
            }) }
        }
    }
}
