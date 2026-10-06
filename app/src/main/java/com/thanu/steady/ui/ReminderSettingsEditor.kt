package com.thanu.steady.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import java.time.LocalTime
import java.util.Locale

@Composable fun ReminderSettingsEditor(model: ExpandedViewModel,state: ExpandedUiState,onSafety: () -> Unit,onClose: () -> Unit) {
    val p = state.period ?: return
    val key = "reminder_settings"
    fun time(value: Int) = String.format(Locale.ROOT,"%02d:%02d",value/60,value%60)
    DraftEditor(model,state,key,R.string.reminder_settings,mapOf("start" to time(p.profile.quietStart),"end" to time(p.profile.quietEnd),
        "budget" to p.profile.alertBudget.toString(),"sound" to (p.preferences.cueFlags and 1 != 0).toString(),
        "vibration" to (p.preferences.cueFlags and 2 != 0).toString(),"pause" to p.preferences.pauseEnabled.toString()),onSafety,onClose) { values,close ->
        TextInput(values["start"].orEmpty(),R.string.quiet_start,{ model.field(key,"start",it) })
        TextInput(values["end"].orEmpty(),R.string.quiet_end,{ model.field(key,"end",it) })
        TextInput(values["budget"].orEmpty(),R.string.alert_budget,{ model.field(key,"budget",it) })
        ToggleRow(R.string.cue_sound,values["sound"] == "true") { model.field(key,"sound",it.toString()) }
        ToggleRow(R.string.cue_vibration,values["vibration"] == "true") { model.field(key,"vibration",it.toString()) }
        ToggleRow(R.string.next_days_paused,values["pause"] == "true") { model.field(key,"pause",it.toString()) }
        Text(stringResource(R.string.next_days_policy)); Text(stringResource(R.string.reminder_delivery_limit))
        PrimaryAction(R.string.save_action,!state.busy) { model.action({
            val start = LocalTime.parse(values.getValue("start")); val end = LocalTime.parse(values.getValue("end"))
            val flags = (if(values["sound"] == "true") 1 else 0) or (if(values["vibration"] == "true") 2 else 0)
            model.repository.reminderSettings(p.profile.copy(quietStart = start.hour*60+start.minute,quietEnd = end.hour*60+end.minute,
                alertBudget = values.getValue("budget").toInt()),flags,values["pause"] == "true")
        },after = close) }
    }
}
