package com.thanu.steady.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.data.SteadyDatabase
import com.thanu.steady.data.WeeklyReviewEntity
import com.thanu.steady.domain.DailyPlan
import com.thanu.steady.domain.WeeklyReview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class ReviewUiState(
    val isLoading: Boolean = true,
    val weekEnd: LocalDate = LocalDate.now(),
    val review: WeeklyReview = WeeklyReview(weekEnd = LocalDate.now()),
    val plansInWindow: List<DailyPlan> = emptyList(),
    val isSaved: Boolean = false,
    val showThirtyDay: Boolean = false
)

class ReviewViewModel(private val database: SteadyDatabase) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadWeek(LocalDate.now())
    }

    fun loadWeek(endDate: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, weekEnd = endDate) }
            val start = endDate.minusDays(6)
            
            val plans = database.reviewDao().getPlansInWindow(start, endDate).map { it.toDomain() }
            val reviewEntity = database.reviewDao().getReview(endDate)
            
            val review = if (reviewEntity != null) {
                WeeklyReview(
                    weekEnd = reviewEntity.weekEnd,
                    indicatorSleep = reviewEntity.indicatorSleep,
                    indicatorLearning = reviewEntity.indicatorLearning,
                    indicatorBuilding = reviewEntity.indicatorBuilding,
                    indicatorHealth = reviewEntity.indicatorHealth,
                    indicatorConnection = reviewEntity.indicatorConnection,
                    helped = reviewEntity.helped,
                    tooDemanding = reviewEntity.tooDemanding,
                    changedEvidence = reviewEntity.changedEvidence,
                    adjustment = reviewEntity.adjustment,
                    updatedAt = reviewEntity.updatedAt
                )
            } else {
                WeeklyReview(weekEnd = endDate)
            }

            _uiState.update { 
                it.copy(isLoading = false, review = review, plansInWindow = plans) 
            }
        }
    }

    fun updateReviewField(field: String, value: String) {
        _uiState.update { state ->
            val r = state.review
            val updated = when(field) {
                "sleep" -> r.copy(indicatorSleep = value)
                "learning" -> r.copy(indicatorLearning = value)
                "building" -> r.copy(indicatorBuilding = value)
                "health" -> r.copy(indicatorHealth = value)
                "connection" -> r.copy(indicatorConnection = value)
                "helped" -> r.copy(helped = value)
                "tooDemanding" -> r.copy(tooDemanding = value)
                "changedEvidence" -> r.copy(changedEvidence = value)
                "adjustment" -> r.copy(adjustment = value)
                else -> r
            }
            state.copy(review = updated, isSaved = false)
        }
    }

    fun toggleThirtyDay() {
        _uiState.update { it.copy(showThirtyDay = !it.showThirtyDay) }
    }

    fun saveReview() {
        viewModelScope.launch {
            val r = _uiState.value.review
            val entity = WeeklyReviewEntity(
                weekEnd = r.weekEnd,
                indicatorSleep = r.indicatorSleep,
                indicatorLearning = r.indicatorLearning,
                indicatorBuilding = r.indicatorBuilding,
                indicatorHealth = r.indicatorHealth,
                indicatorConnection = r.indicatorConnection,
                helped = r.helped,
                tooDemanding = r.tooDemanding,
                changedEvidence = r.changedEvidence,
                adjustment = r.adjustment,
                updatedAt = Instant.now()
            )
            database.reviewDao().insertReview(entity)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
