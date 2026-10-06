package com.thanu.steady.ui

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.IntervalProgram
import kotlin.math.roundToInt

fun intervalFrom(values: Map<String,String>) = IntervalProgram(values.getValue("work").toInt(),values.getValue("rest").toInt(),
    values.getValue("rounds").toInt(),values.getValue("warmup").toInt(),values.getValue("cooldown").toInt()).also(IntervalProgram::validate)

@Composable fun IntervalFields(model: ExpandedViewModel,key: String,values: Map<String,String>) {
    listOf("work" to R.string.work_seconds,"rest" to R.string.rest_seconds,"rounds" to R.string.interval_rounds,
        "warmup" to R.string.warmup_seconds,"cooldown" to R.string.cooldown_seconds).forEach { (field,label) ->
        TextInput(values[field].orEmpty(),label,{ model.field(key,field,it) })
    }
    Text(stringResource(R.string.interval_last_rest))
}
@Composable fun TemplateEditor(model: ExpandedViewModel,state: ExpandedUiState,original: WorkoutTemplate?,onSafety: () -> Unit,onClose: () -> Unit) {
    val key = "template:${original?.id ?: "new"}"
    DraftEditor(model,state,key,R.string.workout_template,mapOf("title" to original?.title.orEmpty(),"mode" to (original?.mode ?: "STRENGTH"),
        "exercises" to original?.exercises.orEmpty(),"work" to (original?.workSeconds ?: 30).toString(),"rest" to (original?.restSeconds ?: 30).toString(),
        "rounds" to (original?.rounds ?: 1).toString(),"warmup" to (original?.warmupSeconds ?: 0).toString(),"cooldown" to (original?.cooldownSeconds ?: 0).toString()),onSafety,onClose) { values,close ->
        TextInput(values["title"].orEmpty(),R.string.workout_name,{ model.field(key,"title",it) })
        ChoiceList(values["mode"] ?: "STRENGTH",listOf("STRENGTH" to R.string.strength_mode,"INTERVALS" to R.string.intervals_mode)) { model.field(key,"mode",it) }
        TextInput(values["exercises"].orEmpty(),R.string.template_exercises,{ model.field(key,"exercises",it) },4)
        IntervalFields(model,key,values)
        PrimaryAction(R.string.save_action,!state.busy) { model.action({
            val p = intervalFrom(values)
            model.saveTemplate(WorkoutTemplate(original?.id ?: model.repository.newId(),values["title"].orEmpty(),values["mode"] ?: "STRENGTH",
                values["exercises"].orEmpty(),p.workSeconds,p.restSeconds,p.rounds,p.warmupSeconds,p.cooldownSeconds))
        },after = close) }
    }
}
@Composable fun ActiveEffort(model: ExpandedViewModel,state: ExpandedUiState,session: ActivitySession) {
    var value by remember(session.id,session.effort) { mutableStateOf((session.effort ?: 1).toFloat()) }
    var chosen by remember(session.id,session.effort) { mutableStateOf(session.effort != null) }
    val label = stringResource(R.string.effort_slider)
    Text(if(chosen) stringResource(R.string.effort_value,value.roundToInt()) else stringResource(R.string.effort_unset))
    Slider(value,{ value = it; chosen = true },enabled = !state.busy,valueRange = 1f..10f,steps = 8,
        onValueChangeFinished = { if(chosen) model.effort(session.id,value.roundToInt()) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { contentDescription = label })
    SecondaryAction(R.string.clear_effort,!state.busy) { chosen = false; model.effort(session.id,null) }
}
