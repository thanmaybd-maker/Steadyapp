package com.thanu.steady.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.data.SteadyDatabase
import com.thanu.steady.data.SafetyPlanEntity
import com.thanu.steady.data.SupportContactEntity
import com.thanu.steady.domain.SafetyPlan
import com.thanu.steady.domain.SupportContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class SafetyUiState(
    val isLoading: Boolean = true,
    val plan: SafetyPlan = SafetyPlan(),
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class SafetyViewModel(private val repository: com.thanu.steady.data.PrivateSafetyRepository,
    private val clock: java.time.Clock = java.time.Clock.systemUTC()) : ViewModel() {
    private val _uiState = MutableStateFlow(SafetyUiState())
    val uiState: StateFlow<SafetyUiState> = _uiState.asStateFlow()

    init {
        loadPlan()
    }

    private fun loadPlan() {
        viewModelScope.launch {
            try {
                val (planEntity, storedContacts) = repository.load()
                val contacts = storedContacts.map {
                    SupportContact(it.id, it.role, it.displayName, it.phone, it.note, it.sortOrder)
                }

                if (planEntity != null) {
                    val plan = SafetyPlan(
                        id = planEntity.id,
                        warningSigns = planEntity.warningSigns,
                        copingSteps = planEntity.copingSteps,
                        safePeoplePlaces = planEntity.safePeoplePlaces,
                        environmentSteps = planEntity.environmentSteps,
                        clinicName = planEntity.clinicName,
                        clinicPhone = planEntity.clinicPhone,
                        followUpAt = planEntity.followUpAt,
                        reviewedByUserAt = planEntity.reviewedByUserAt,
                        clinicianReviewStatus = planEntity.clinicianReviewStatus,
                        updatedAt = planEntity.updatedAt,
                        contacts = contacts
                    )
                    _uiState.update { it.copy(isLoading = false, plan = plan) }
                } else {
                    _uiState.update { it.copy(isLoading = false, plan = SafetyPlan()) }
                }
            } catch (e: Exception) {
                // Return an empty plan on failure rather than crashing or blocking public numbers
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to open encrypted plan") }
            }
        }
    }

    fun updatePlan(field: String, value: String) {
        _uiState.update { state ->
            val p = state.plan
            val updated = when (field) {
                "warningSigns" -> p.copy(warningSigns = value)
                "copingSteps" -> p.copy(copingSteps = value)
                "safePeoplePlaces" -> p.copy(safePeoplePlaces = value)
                "environmentSteps" -> p.copy(environmentSteps = value)
                "clinicName" -> p.copy(clinicName = value)
                "clinicPhone" -> p.copy(clinicPhone = value)
                else -> p
            }
            state.copy(plan = updated, isSaved = false)
        }
    }

    fun toggleEdit() {
        _uiState.update { it.copy(isEditing = !it.isEditing) }
    }

    fun savePlan() {
        viewModelScope.launch {
            try {
                val p = _uiState.value.plan
                val entity = SafetyPlanEntity(
                    id = p.id,
                    warningSigns = p.warningSigns,
                    copingSteps = p.copingSteps,
                    safePeoplePlaces = p.safePeoplePlaces,
                    environmentSteps = p.environmentSteps,
                    clinicName = p.clinicName,
                    clinicPhone = p.clinicPhone,
                    followUpAt = p.followUpAt,
                    reviewedByUserAt = p.reviewedByUserAt,
                    clinicianReviewStatus = p.clinicianReviewStatus,
                    updatedAt = clock.instant()
                )
                val contacts = p.contacts.map {
                    SupportContactEntity(it.id, p.id, it.role, it.displayName, it.phone, it.note, it.sortOrder)
                }
                repository.save(entity, contacts)
                _uiState.update { it.copy(isSaved = true, isEditing = false, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "The private plan could not be saved. Your draft is preserved.") }
            }
        }
    }
}
