package com.thanu.steady.ui

import androidx.compose.runtime.Composable
import com.thanu.steady.R
import com.thanu.steady.data.CareReminder
import java.time.LocalTime
import java.util.Locale

@Composable fun CareEditor(model: ExpandedViewModel,state: ExpandedUiState,original: CareReminder?,onSafety: () -> Unit,onClose: () -> Unit) {
    val key = "care:${original?.id ?: "new"}"
    DraftEditor(model,state,key,R.string.add_care,mapOf("instruction" to original?.instruction.orEmpty(),"time" to (original?.minute?.let {
        String.format(Locale.ROOT,"%02d:%02d",it/60,it%60) } ?: "09:00"),"weekdays" to (original?.weekdays ?: 127).toString(),
        "enabled" to (original?.enabled ?: true).toString()),onSafety,onClose) { values,close ->
        TextInput(values["instruction"].orEmpty(),R.string.care_instruction,{ model.field(key,"instruction",it) },3)
        TextInput(values["time"].orEmpty(),R.string.reminder_time,{ model.field(key,"time",it) })
        val weekdays = values["weekdays"]?.toIntOrNull() ?: 127
        listOf(R.string.monday,R.string.tuesday,R.string.wednesday,R.string.thursday,R.string.friday,R.string.saturday,R.string.sunday).forEachIndexed { index,label ->
            ToggleRow(label,weekdays and (1 shl index) != 0) { enabled -> model.field(key,"weekdays",(if(enabled) weekdays or (1 shl index) else weekdays and (1 shl index).inv()).toString()) }
        }
        ToggleRow(R.string.reminder_enabled,values["enabled"] == "true") { model.field(key,"enabled",it.toString()) }
        PrimaryAction(R.string.save_action,!state.busy) { model.action({
            val time = LocalTime.parse(values.getValue("time"))
            model.repository.saveCare(CareReminder(original?.id ?: model.repository.newId(),values["instruction"].orEmpty(),weekdays,
                time.hour*60+time.minute,values["enabled"] == "true",model.repository.clock.millis()))
        },after = close) }
    }
}
