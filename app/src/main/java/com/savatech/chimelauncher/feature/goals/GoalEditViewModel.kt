package com.savatech.chimelauncher.feature.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.service.SchedulerRescheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

data class GoalEditUiState(
    val goalId: String? = null,
    val title: String = "",
    val why: String = "",
    val category: String = "",
    val targetDate: String? = null,
    val unit: String = "",
    val targetValue: String = "",
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val titleError: GoalTitleError? = null,
    val whyError: Boolean = false,
    val measurableTargetError: Boolean = false,
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val failure: GoalEditFailure? = null,
)

enum class GoalTitleError { REQUIRED, TOO_LONG }
enum class GoalEditFailure { NOT_FOUND, SAVE_FAILED, DELETE_FAILED }

@HiltViewModel
class GoalEditViewModel @Inject constructor(
    private val repository: GoalRepository,
    savedStateHandle: SavedStateHandle,
    private val schedulerFacade: SchedulerRescheduler,
) : ViewModel() {
    private val goalId = savedStateHandle.get<String>("goalId").takeUnless { it == NEW_GOAL_ID }
    private val _uiState = MutableStateFlow(GoalEditUiState(goalId = goalId, isLoading = goalId != null))
    val uiState: StateFlow<GoalEditUiState> = _uiState.asStateFlow()

    init {
        if (goalId != null) {
            viewModelScope.launch {
                val goal = repository.getGoal(goalId)
                if (goal == null) {
                    _uiState.update { it.copy(isLoading = false, failure = GoalEditFailure.NOT_FOUND) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = goal.title,
                            why = goal.why.orEmpty(),
                            category = goal.category,
                            targetDate = goal.targetDate?.toString(),
                            unit = goal.unit.orEmpty(),
                            targetValue = goal.targetValue?.toString().orEmpty(),
                            status = goal.status,
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(value: String) = _uiState.update {
        it.copy(title = value.take(MAX_TITLE_LENGTH + 1), titleError = null)
    }

    fun updateWhy(value: String) = _uiState.update {
        it.copy(why = value.take(MAX_WHY_LENGTH + 1), whyError = false)
    }

    fun updateCategory(value: String) = _uiState.update { it.copy(category = value) }
    fun updateTargetDate(value: LocalDate?) =
        _uiState.update { it.copy(targetDate = value?.toString()) }
    fun updateUnit(value: String) = _uiState.update { it.copy(unit = value) }
    fun updateTargetValue(value: String) = _uiState.update {
        it.copy(targetValue = value.filter { char -> char.isDigit() || char == '.' }, measurableTargetError = false)
    }

    fun save(allowedCategories: List<String>) {
        val state = _uiState.value
        val titleError = when {
            state.title.isBlank() -> GoalTitleError.REQUIRED
            state.title.length > MAX_TITLE_LENGTH -> GoalTitleError.TOO_LONG
            else -> null
        }
        val whyError = state.why.length > MAX_WHY_LENGTH
        val hasUnit = state.unit.isNotBlank()
        val hasValue = state.targetValue.isNotBlank()
        val parsedValue = state.targetValue.toDoubleOrNull()
        val targetError = hasUnit != hasValue ||
            (hasValue && (parsedValue == null || !parsedValue.isFinite() || parsedValue <= 0.0))
        if (titleError != null || whyError || targetError || state.category !in allowedCategories) {
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    whyError = whyError,
                    measurableTargetError = targetError,
                )
            }
            return
        }
        viewModelScope.launch {
            val model = GoalModel(
                id = goalId ?: UUID.randomUUID().toString(),
                title = state.title.trim(),
                why = state.why.trim().ifBlank { null },
                category = state.category,
                targetDate = state.targetDate?.let(LocalDate::parse),
                unit = state.unit.trim().ifBlank { null },
                targetValue = parsedValue,
                status = state.status,
                createdAt = state.createdAt,
            )
            try {
                if (goalId == null) repository.createGoal(model)
                else if (!repository.updateGoal(model)) error("Goal update failed")
                schedulerFacade.rescheduleAll()
                _uiState.update { it.copy(saved = true, failure = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(failure = GoalEditFailure.SAVE_FAILED) }
            }
        }
    }

    fun setStatus(status: GoalStatus) {
        val goalId = goalId ?: return
        viewModelScope.launch {
            try {
                val existing = repository.getGoal(goalId)
                if (existing == null || !repository.updateGoal(existing.copy(status = status))) {
                    _uiState.update { it.copy(failure = GoalEditFailure.SAVE_FAILED) }
                } else {
                    schedulerFacade.rescheduleAll()
                    _uiState.update { it.copy(status = status, saved = status == GoalStatus.ARCHIVED) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(failure = GoalEditFailure.SAVE_FAILED) }
            }
        }
    }

    fun showDeleteConfirmation() = _uiState.update { it.copy(showDeleteConfirmation = true) }
    fun dismissDeleteConfirmation() = _uiState.update { it.copy(showDeleteConfirmation = false) }

    fun deleteGoal() {
        val goalId = goalId ?: return
        viewModelScope.launch {
            try {
                if (repository.deleteGoal(goalId)) {
                    schedulerFacade.rescheduleAll()
                    _uiState.update { it.copy(saved = true, showDeleteConfirmation = false) }
                } else {
                    _uiState.update { it.copy(failure = GoalEditFailure.DELETE_FAILED) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(failure = GoalEditFailure.DELETE_FAILED) }
            }
        }
    }

    private companion object {
        const val NEW_GOAL_ID = "new"
        const val MAX_TITLE_LENGTH = 80
        const val MAX_WHY_LENGTH = 300
    }
}
