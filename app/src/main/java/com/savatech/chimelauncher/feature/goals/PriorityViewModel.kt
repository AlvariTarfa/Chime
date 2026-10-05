package com.savatech.chimelauncher.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.DailyPriorityModel
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.usecase.SetDailyPrioritiesResult
import com.savatech.chimelauncher.domain.usecase.SetDailyPrioritiesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PriorityUiState(
    val activeGoals: List<GoalModel> = emptyList(),
    val selectedGoalIds: List<String> = emptyList(),
    val error: SetDailyPrioritiesResult? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class PriorityViewModel @Inject constructor(
    repository: GoalRepository,
    private val setDailyPriorities: SetDailyPrioritiesUseCase,
) : ViewModel() {
    private val today = LocalDate.now()
    private val localSelection = MutableStateFlow<List<String>?>(null)
    private val result = MutableStateFlow<SetDailyPrioritiesResult?>(null)
    private val saved = MutableStateFlow(false)

    val uiState: StateFlow<PriorityUiState> = combine(
        repository.observeGoals(GoalStatus.ACTIVE),
        repository.observeDailyPriorities(today),
        localSelection,
        result,
        saved,
    ) { goals, priorities, selection, error, wasSaved ->
        PriorityUiState(
            activeGoals = goals,
            selectedGoalIds = selection ?: priorities.sortedBy(DailyPriorityModel::position)
                .map(DailyPriorityModel::goalId)
                .filter { id -> goals.any { it.id == id } },
            error = error,
            saved = wasSaved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PriorityUiState())

    fun toggle(goalId: String, selected: Boolean) {
        val current = uiState.value.selectedGoalIds
        if (selected && current.size >= MAX_PRIORITIES) {
            result.value = SetDailyPrioritiesResult.TooManyPriorities
            return
        }
        localSelection.value = if (selected) current + goalId else current.filterNot { it == goalId }
        result.value = null
        saved.value = false
    }

    fun move(goalId: String, offset: Int) {
        val current = uiState.value.selectedGoalIds.toMutableList()
        val index = current.indexOf(goalId)
        val target = index + offset
        if (index < 0 || target !in current.indices) return
        val item = current.removeAt(index)
        current.add(target, item)
        localSelection.value = current
    }

    fun save() {
        viewModelScope.launch {
            val outcome = setDailyPriorities(today, uiState.value.selectedGoalIds)
            result.value = when (outcome) {
                SetDailyPrioritiesResult.Success -> null
                else -> outcome
            }
            saved.value = outcome == SetDailyPrioritiesResult.Success
        }
    }

    fun dismissError() {
        result.value = null
    }

    private companion object {
        const val MAX_PRIORITIES = 3
    }
}
