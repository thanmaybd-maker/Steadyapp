package com.thanu.steady.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.R
import com.thanu.steady.data.*
import com.thanu.steady.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Clock
import java.util.UUID

data class SafetyUiState(val isLoading: Boolean = true, val plan: SafetyPlan = SafetyPlan(),
    val isEditing: Boolean = false, val isSaved: Boolean = false, val busy: Boolean = false, val errorMessage: Int? = null)

class SafetyViewModel(private val repository: PrivateSafetyRepository, private val clock: Clock = Clock.systemUTC()) : ViewModel() {
    private val _uiState = MutableStateFlow(SafetyUiState())
    val uiState = _uiState.asStateFlow()
    private var saved = SafetyPlan()
    private var draftJob: Job? = null
    private var draftVersion = 0
    init { loadPlan() }
    fun loadPlan() {
        if (_uiState.value.busy || _uiState.value.isEditing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true,errorMessage = null) }
            try {
                val (p,contacts) = withContext(Dispatchers.IO) { repository.load() }
                saved = if (p == null) SafetyPlan() else SafetyPlan(p.id,p.warningSigns,p.copingSteps,p.safePeoplePlaces,p.environmentSteps,
                    p.clinicName,p.clinicPhone,p.followUpAt,p.reviewedByUserAt,p.clinicianReviewStatus,p.updatedAt,
                    contacts.map { SupportContact(it.id,it.role,it.displayName,it.phone,it.note,it.sortOrder) })
                val draft = withContext(Dispatchers.IO) { repository.loadDraft() }
                _uiState.update { it.copy(isLoading = false,plan = draft ?: saved,isEditing = draft != null) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(isLoading = false,errorMessage = R.string.private_plan_load_failed) } }
        }
    }
    fun updatePlan(field: String,value: String) {
        if (_uiState.value.busy || value.length > 100_000) return
        _uiState.update { s -> val p = s.plan
            s.copy(isSaved = false,plan = when(field) {
                "warningSigns" -> p.copy(warningSigns=value); "copingSteps" -> p.copy(copingSteps=value)
                "safePeoplePlaces" -> p.copy(safePeoplePlaces=value); "environmentSteps" -> p.copy(environmentSteps=value)
                "clinicName" -> p.copy(clinicName=value); "clinicPhone" -> p.copy(clinicPhone=value)
                "followUpAt" -> p.copy(followUpAt=value.takeIf(String::isNotBlank)); else -> p
            })
        }
        persistDraft()
    }
    fun toggleEdit() {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(isEditing = !it.isEditing,isSaved = false) }
    }
    fun addContact() {
        if (_uiState.value.busy || _uiState.value.plan.contacts.size >= 100) return
        _uiState.update { s -> s.copy(plan=s.plan.copy(contacts=s.plan.contacts + SupportContact(UUID.randomUUID().toString(),"other","","",null,s.plan.contacts.size)),isSaved=false) }
        persistDraft()
    }
    fun contact(id: String,field: String,value: String) {
        if (_uiState.value.busy || value.length > 10_000) return
        _uiState.update { s -> s.copy(isSaved=false,plan=s.plan.copy(contacts=s.plan.contacts.map {
            if(it.id != id) it else when(field) {
                "name" -> it.copy(displayName=value); "phone" -> it.copy(phone=value); "role" -> it.copy(role=value)
                "note" -> it.copy(note=value.takeIf(String::isNotBlank)); else -> it
            }
        })) }
        persistDraft()
    }
    fun removeContact(id: String) {
        if (!_uiState.value.busy) { _uiState.update { s -> s.copy(plan=s.plan.copy(contacts=s.plan.contacts.filterNot { it.id == id }),isSaved=false) }; persistDraft() }
    }
    fun reviewed() { _uiState.update { it.copy(plan=it.plan.copy(reviewedByUserAt=clock.instant()),isSaved=false) }; persistDraft() }
    private fun persistDraft() {
        val version = ++draftVersion
        val plan = _uiState.value.plan
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            delay(400)
            try { withContext(Dispatchers.IO) { repository.saveDraft(plan) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if(version == draftVersion) _uiState.update { it.copy(errorMessage = R.string.private_draft_failed) } }
        }
    }
    fun savePlan() {
        if (_uiState.value.busy) return
        val p = _uiState.value.plan
        _uiState.update { it.copy(busy=true,errorMessage=null) }
        viewModelScope.launch {
            try {
                draftJob?.cancelAndJoin()
                withContext(Dispatchers.IO) {
                    val entity = SafetyPlanEntity(p.id,p.warningSigns,p.copingSteps,p.safePeoplePlaces,p.environmentSteps,p.clinicName,
                        p.clinicPhone,p.followUpAt,p.reviewedByUserAt,p.clinicianReviewStatus,clock.instant())
                    repository.save(entity,p.contacts.map { SupportContactEntity(it.id,p.id,it.role,it.displayName,it.phone,it.note,it.sortOrder) })
                }
                saved=p
                _uiState.update { it.copy(isSaved=true,isEditing=false) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(errorMessage=R.string.private_plan_save_failed) } }
            finally { _uiState.update { it.copy(busy=false) } }
        }
    }
}

