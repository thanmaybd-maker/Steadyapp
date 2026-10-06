package com.thanu.steady.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.data.PeriodSnapshot

/** A record-backed replacement for the old sample macro/energy values. */
@Composable fun CognitiveNutritionCard(period: PeriodSnapshot) {
    SectionCard(R.string.recorded_meals_title) {
        Text(stringResource(R.string.recorded_meals_count, period.meals.size),
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        period.meals.forEach { meal ->
            Text(meal.title, color = MaterialTheme.colorScheme.onSurface)
            if (meal.notes.isNotBlank()) Text(meal.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(stringResource(R.string.nutrition_optional_notice), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
