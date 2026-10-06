package com.thanu.steady.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.domain.PersonalizationRules

@Composable fun DashboardSettingsEditor(model: ExpandedViewModel,state: ExpandedUiState,onSafety: () -> Unit,onClose: () -> Unit) {
    val profile = state.period?.profile ?: return
    val rings = profile.ringMetrics.split(',')
    val key = "summary_preferences"
    DraftEditor(model,state,key,R.string.summary_preferences,mapOf("one" to rings[0],"two" to rings[1],"three" to rings[2],
        "quantities" to profile.waterQuickMl,"unit" to profile.waterUnit,"review" to profile.reviewCards),onSafety,onClose) { values,close ->
        listOf("one" to R.string.first_summary,"two" to R.string.second_summary,"three" to R.string.third_summary).forEach { (field,label) ->
            Text(stringResource(label))
            ChoiceList(values[field].orEmpty(),PersonalizationRules.metrics.map { it to metricLabel(it) }) { model.field(key,field,it) }
        }
        Text(stringResource(R.string.summary_distinct_notice))
        TextInput(values["quantities"].orEmpty(),R.string.water_quick_quantities,{ model.field(key,"quantities",it) },supporting = stringResource(R.string.water_quantities_example))
        ChoiceList(values["unit"].orEmpty(),listOf("ML" to R.string.millilitres_unit,"FLOZ" to R.string.fluid_ounces_unit)) { model.field(key,"unit",it) }
        Text(stringResource(R.string.visible_review_cards))
        val cards = values["review"].orEmpty().split(',').filter(String::isNotBlank).toSet()
        PersonalizationRules.reviewCards.forEach { card ->
            ToggleRow(metricLabel(card),card in cards) { enabled -> model.field(key,"review",(if(enabled) cards+card else cards-card).joinToString(",")) }
        }
        PrimaryAction(R.string.save_action,!state.busy) { model.action({ model.repository.saveProfile(profile.copy(
            ringMetrics = listOf(values["one"],values["two"],values["three"]).joinToString(","),
            waterQuickMl = values["quantities"].orEmpty(),waterUnit = values["unit"].orEmpty(),reviewCards = values["review"].orEmpty())) },after = close) }
    }
}
