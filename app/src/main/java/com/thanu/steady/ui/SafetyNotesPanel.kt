package com.thanu.steady.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.thanu.steady.R

private val safetyNoteCategories=listOf("CONTACT" to R.string.private_note_contact,"MEDICAL" to R.string.private_note_medical,
    "ROUTE" to R.string.private_note_route,"CAMPUS" to R.string.private_note_campus,"GROUNDING" to R.string.private_note_grounding,"OTHER" to R.string.private_note_other)

@Composable fun SafetyNotesPanel(model: SafetyNotesViewModel,masked: Boolean,country: String) {
    val state by model.state.collectAsState()
    var help by remember { mutableStateOf(false) }
    SectionCard(R.string.private_notes_title) {
        state.error?.let { Text(stringResource(it),color=MaterialTheme.colorScheme.error) }
        if(state.loading) Text(stringResource(R.string.loading_records))
        if(state.error == R.string.private_notes_load_failed) SecondaryAction(R.string.retry,onClick=model::load)
        if(masked) Text(stringResource(R.string.private_content_masked))
        else {
            Text(stringResource(R.string.private_note_exclusion))
            PrimaryAction(if(state.draft == null) R.string.private_note_add else R.string.private_note_resume,!state.busy && !state.loading,onClick={ model.edit() })
            if(state.draft != null) { Text(stringResource(R.string.private_note_pending_draft)); SecondaryAction(R.string.private_note_discard,!state.busy,onClick=model::discard) }
            if(state.notes.isEmpty()) Text(stringResource(R.string.private_notes_empty))
            state.notes.forEach { note ->
                Text(note.title.ifBlank { stringResource(R.string.private_note_untitled) },style=MaterialTheme.typography.titleMedium)
                Text(stringResource(safetyNoteCategories.first { it.first == note.category }.second))
                Text(note.content)
                SecondaryAction(if(note.pinned) R.string.private_note_unpin else R.string.private_note_pin,!state.busy && state.draft == null) { model.pin(note) }
                SecondaryAction(R.string.edit_action,!state.busy && state.draft == null) { model.edit(note) }
                SecondaryAction(R.string.delete_action,!state.busy && state.draft == null) { model.delete(note) }
            }
        }
    }
    if(!masked && state.editorOpen) state.draft?.let { note ->
        Dialog(onDismissRequest=model::closeEditor,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false,securePolicy=SecureFlagPolicy.SecureOn)) {
            DialogSurface { ExpandedPage {
                SecondaryAction(R.string.safety_action) { help=true }
                SecondaryAction(R.string.close_keep_draft,onClick=model::closeEditor)
                Text(stringResource(R.string.private_note_editor),style=MaterialTheme.typography.headlineSmall)
                state.error?.let { Text(stringResource(it),color=MaterialTheme.colorScheme.error) }
                Text(stringResource(state.draftStatus))
                if(state.draftStatus == R.string.private_draft_failed) SecondaryAction(R.string.retry,onClick=model::retryDraft)
                TextInput(note.title,R.string.private_note_title,{ model.field("title",it) })
                TextInput(note.content,R.string.private_note_content,{ model.field("content",it) },4)
                ChoiceList(note.category,safetyNoteCategories) { model.field("category",it) }
                ToggleRow(R.string.private_note_pin,note.pinned) { model.field("pinned",it.toString()) }
                PrimaryAction(R.string.save_action,!state.busy,onClick=model::save)
            } }
        }
    }
    if(help) Dialog(onDismissRequest={ help=false },properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false,securePolicy=SecureFlagPolicy.SecureOn)) {
        DialogSurface { ExpandedPage { SecondaryAction(R.string.close_action) { help=false }; PublicSafetyPanel(country) } }
    }
}
