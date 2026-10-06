package com.thanu.steady.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import com.thanu.steady.R
import com.thanu.steady.platform.DialerAdapter

@Composable fun SafetyScreen(viewModel: SafetyViewModel,country: String = "IN") {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var failedDial by remember { mutableStateOf(false) }
    var failedSms by remember { mutableStateOf(false) }
    var masked by remember { mutableStateOf(true) }
    val notesModel: SafetyNotesViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key="private_extra_notes",factory=viewModel.notesFactory())
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event -> if(event == androidx.lifecycle.Lifecycle.Event.ON_STOP) { masked=true; notesModel.closeEditor() } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    ExpandedPage {
        PublicSafetyPanel(country)
        SectionCard(R.string.open_private_plan) {
            Text(stringResource(R.string.private_safety_device_only))
            ToggleRow(R.string.mask_private_content,masked) { masked=it; if(it) notesModel.closeEditor() }
            state.errorMessage?.let { Text(stringResource(it),color=MaterialTheme.colorScheme.error) }
            if (state.isLoading) CircularProgressIndicator()
            else if(masked) Text(stringResource(R.string.private_content_masked))
            else {
                if (state.errorMessage == R.string.private_plan_load_failed) SecondaryAction(R.string.retry,onClick=viewModel::loadPlan)
                else {
                    SecondaryAction(if(state.isEditing) R.string.close_keep_draft else R.string.edit_action,!state.busy,onClick=viewModel::toggleEdit)
                    if (state.isEditing) {
                        listOf("warningSigns" to (R.string.warning_signs to state.plan.warningSigns),
                            "copingSteps" to (R.string.coping_steps to state.plan.copingSteps),
                            "safePeoplePlaces" to (R.string.safe_people_places to state.plan.safePeoplePlaces),
                            "environmentSteps" to (R.string.environment_steps to state.plan.environmentSteps),
                            "clinicName" to (R.string.clinic_name to state.plan.clinicName),
                            "clinicPhone" to (R.string.clinic_phone to state.plan.clinicPhone),
                            "followUpAt" to (R.string.follow_up_optional to state.plan.followUpAt.orEmpty())).forEach { (key,pair) ->
                            TextInput(pair.second,pair.first,{ viewModel.updatePlan(key,it) },if(key.endsWith("Steps") || key == "warningSigns") 3 else 1)
                        }
                    } else {
                        listOf(R.string.warning_signs to state.plan.warningSigns,R.string.coping_steps to state.plan.copingSteps,
                            R.string.safe_people_places to state.plan.safePeoplePlaces,R.string.environment_steps to state.plan.environmentSteps,
                            R.string.clinic_name to state.plan.clinicName,R.string.clinic_phone to state.plan.clinicPhone).forEach { (label,value) ->
                            Text(stringResource(label),style=MaterialTheme.typography.titleMedium)
                            Text(value.ifBlank { stringResource(R.string.none_recorded) })
                        }
                        if (state.plan.clinicPhone.isNotBlank()) SecondaryAction(R.string.open_clinic_dialer) { failedDial=!DialerAdapter(context).openDialer(state.plan.clinicPhone) }
                    }
                    Text(stringResource(R.string.private_contacts),style=MaterialTheme.typography.titleLarge)
                    if(state.isEditing) SecondaryAction(R.string.add_private_contact,!state.busy,onClick=viewModel::addContact)
                    state.plan.contacts.forEach { contact ->
                        if(state.isEditing) {
                            TextInput(contact.displayName,R.string.contact_name,{ viewModel.contact(contact.id,"name",it) })
                            TextInput(contact.phone,R.string.contact_phone,{ viewModel.contact(contact.id,"phone",it) })
                            ChoiceList(contact.role,listOf("first" to R.string.contact_first,"backup" to R.string.contact_backup,"clinic" to R.string.contact_clinic,"other" to R.string.contact_other)) { viewModel.contact(contact.id,"role",it) }
                            TextInput(contact.note.orEmpty(),R.string.note_text,{ viewModel.contact(contact.id,"note",it) })
                            SecondaryAction(R.string.delete_action,!state.busy) { viewModel.removeContact(contact.id) }
                        } else {
                            Text(contact.displayName); Text(contact.phone); contact.note?.let { Text(it) }
                            SecondaryAction(R.string.open_contact_dialer) { failedDial=!DialerAdapter(context).openDialer(contact.phone) }
                            SecondaryAction(R.string.open_contact_sms) { failedSms=!DialerAdapter(context).openSmsComposer(contact.phone) }
                        }
                    }
                    if (failedDial) Text(stringResource(R.string.dialer_unavailable),color=MaterialTheme.colorScheme.error)
                    if (failedSms) Text(stringResource(R.string.sms_composer_unavailable),color=MaterialTheme.colorScheme.error)
                    if(state.isEditing) {
                        SecondaryAction(R.string.mark_reviewed,!state.busy,onClick=viewModel::reviewed)
                        PrimaryAction(R.string.save_action,!state.busy,onClick=viewModel::savePlan)
                    }
                    if(state.busy) Text(stringResource(R.string.saving))
                    if(state.isSaved) Text(stringResource(R.string.saved))
                }
            }
        }
        SafetyNotesPanel(notesModel,masked,country)
    }
}

