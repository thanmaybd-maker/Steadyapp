package com.thanu.steady.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.R
import com.thanu.steady.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Clock
import java.util.UUID

data class SafetyNotesState(val loading: Boolean=true,val notes: List<PrivateSafetyNote> = emptyList(),
    val draft: PrivateSafetyNote?=null,val editorOpen: Boolean=false,val busy: Boolean=false,
    val error: Int?=null,val draftStatus: Int=R.string.saved)

class SafetyNotesViewModel(private val repository: PrivateSafetyRepository,private val clock: Clock): ViewModel() {
    private val _state=MutableStateFlow(SafetyNotesState())
    val state=_state.asStateFlow()
    private var draftJob: Job?=null
    private var version=0
    init { load() }
    fun load() {
        if(_state.value.busy || _state.value.draft != null) return
        val request=++version
        _state.update { it.copy(loading=true,error=null) }
        viewModelScope.launch {
            try {
                val result=withContext(Dispatchers.IO) { repository.notes() to repository.noteDraft() }
                if(request == version) _state.update { it.copy(loading=false,notes=result.first,draft=result.second) }
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(_: Exception) { if(request == version) _state.update { it.copy(loading=false,error=R.string.private_notes_load_failed) } }
        }
    }
    fun edit(note: PrivateSafetyNote?=null) {
        val current=_state.value
        if(current.busy || current.loading || current.error == R.string.private_notes_load_failed) return
        val draft=current.draft ?: note ?: PrivateSafetyNote(UUID.randomUUID().toString(),created=clock.millis(),updated=clock.millis())
        _state.update { it.copy(draft=draft,editorOpen=true,error=null) }
        persist(draft)
    }
    fun field(key: String,value: String) {
        val old=_state.value.draft ?: return
        if(_state.value.busy) return
        val fresh=when(key) {
            "title" -> if(value.length <= 500) old.copy(title=value) else return
            "content" -> if(value.length <= 100_000) old.copy(content=value) else return
            "category" -> old.copy(category=value)
            "pinned" -> old.copy(pinned=value == "true")
            else -> return
        }.copy(updated=maxOf(clock.millis(),old.created))
        _state.update { it.copy(draft=fresh,error=null) }; persist(fresh)
    }
    private fun persist(note: PrivateSafetyNote) {
        val request=++version
        draftJob?.cancel(); _state.update { it.copy(draftStatus=R.string.private_note_pending_save) }
        draftJob=viewModelScope.launch {
            delay(250)
            try { withContext(Dispatchers.IO) { repository.saveNoteDraft(note) }
                if(request == version) _state.update { it.copy(draftStatus=R.string.saved) }
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(_: Exception) { if(request == version) _state.update { it.copy(error=R.string.private_draft_failed,draftStatus=R.string.private_draft_failed) } }
        }
    }
    fun closeEditor() { _state.update { it.copy(editorOpen=false) } }
    fun retryDraft() { _state.value.draft?.let(::persist) }
    fun save() { val note=_state.value.draft ?: return; work(clearDraft=true) { repository.saveNoteDraft(note); repository.saveNote(note) } }
    fun discard() = work(clearDraft=true) { repository.discardNoteDraft() }
    fun pin(note: PrivateSafetyNote) { if(_state.value.draft == null) work { repository.pinNote(note.id,note.revision,clock.millis()) } }
    fun delete(note: PrivateSafetyNote) { if(_state.value.draft == null) work { repository.deleteNote(note.id,note.revision) } }
    private fun work(clearDraft: Boolean=false,action: suspend () -> Unit) {
        if(_state.value.busy) return
        _state.update { it.copy(busy=true,error=null) }
        viewModelScope.launch {
            try {
                draftJob?.cancelAndJoin()
                withContext(Dispatchers.IO) { action() }
                _state.update { it.copy(draft=if(clearDraft) null else it.draft,editorOpen=if(clearDraft) false else it.editorOpen,draftStatus=R.string.saved) }
                try { val notes=withContext(Dispatchers.IO) { repository.notes() }; _state.update { it.copy(notes=notes) } }
                catch(cancelled: CancellationException) { throw cancelled }
                catch(_: Exception) { _state.update { it.copy(error=R.string.private_notes_load_failed) } }
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(_: Exception) { _state.update { it.copy(error=R.string.private_note_save_failed) } }
            finally { _state.update { it.copy(busy=false) } }
        }
    }
}
