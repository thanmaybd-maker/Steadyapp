package com.thanu.steady.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.thanu.steady.R

/** Public optional guidance. No personal state, timer, forced breath hold or database. */
@Composable fun SensoryGrounding(onClose: () -> Unit,country: String) {
    var step by rememberSaveable { mutableStateOf(0) }
    var help by rememberSaveable { mutableStateOf(false) }
    val steps=listOf(R.string.grounding_see,R.string.grounding_touch,R.string.grounding_hear,R.string.grounding_smell,R.string.grounding_taste)
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        DialogSurface { ExpandedPage {
            SecondaryAction(R.string.close_action,onClick=onClose)
            SecondaryAction(R.string.public_help) { help=!help }
            Text(stringResource(R.string.grounding_optional))
            if(help) PublicSafetyPanel(country,offerGrounding=false)
            SectionCard(R.string.grounding_title) {
                Text(stringResource(R.string.grounding_step,step+1,steps.size))
                Text(stringResource(steps[step]))
                if(step > 0) SecondaryAction(R.string.grounding_previous) { step-- }
                PrimaryAction(if(step == steps.lastIndex) R.string.grounding_finish else R.string.grounding_next) {
                    if(step == steps.lastIndex) onClose() else step++
                }
                SecondaryAction(R.string.grounding_skip) { if(step == steps.lastIndex) onClose() else step++ }
            }
        } }
    }
}
