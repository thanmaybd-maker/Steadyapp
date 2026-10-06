package com.thanu.steady.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.data.HabitVersion

@Composable fun RoutineAnchorEditor(model: ExpandedViewModel,state: ExpandedUiState,key: String,onSafety: () -> Unit,onClose: () -> Unit) {
    val parts = key.split(':'); val title = stringResource(parts[1].toInt())
    val unit = stringResource(R.string.minutes_unit)
    val groups = listOf("WAKING" to R.string.after_waking,"COLLEGE" to R.string.after_college,"SLEEP" to R.string.before_sleep)
    DraftEditor(model,state,key,R.string.routine_anchors,mapOf("title" to title,"target" to parts[2],"group" to "WAKING","essential" to "false"),onSafety,onClose) { values,close ->
        TextInput(values["title"].orEmpty(),R.string.habit_title,{ model.field(key,"title",it) })
        TextInput(values["target"].orEmpty(),R.string.duration_minutes,{ model.field(key,"target",it) })
        ChoiceList(values["group"] ?: "WAKING",groups) { model.field(key,"group",it) }
        ToggleRow(R.string.essential_task,values["essential"] == "true") { model.field(key,"essential",it.toString()) }
        val group = stringResource(groups.firstOrNull { it.first == values["group"] }?.second ?: R.string.after_waking)
        PrimaryAction(R.string.save_action,!state.busy) { model.action({
            val day = model.repository.logicalDay()
            model.repository.saveHabit(HabitVersion(model.repository.newId(),model.repository.newId(),day.toString(),"$group · ${values["title"].orEmpty()}",
                "DURATION",unit,values.getValue("target").toDouble(),anchorDay = day.toString(),essential = values["essential"] == "true",created = model.repository.clock.millis()))
        },after = close) }
    }
}
