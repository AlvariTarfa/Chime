package com.savatech.chimelauncher.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.domain.progress.dailyProgress
import com.savatech.chimelauncher.domain.streak.TaskSpec
import com.savatech.chimelauncher.domain.streak.compute
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class GoalCardUiState(
    val goal: GoalModel,
    val dailyProgress: Float?,
    val scheduledTaskCount: Int,
    val currentStreak: Int,
)

data class GoalsListUiState(
    val selectedStatus: GoalStatus = GoalStatus.ACTIVE,
    val goals: List<GoalCardUiState> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class GoalsListViewModel @Inject constructor(
    private val repository: GoalRepository,
) : ViewModel() {
    private val selectedStatus = MutableStateFlow(GoalStatus.ACTIVE)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GoalsListUiState> = selectedStatus.flatMapLatest { status ->
        repository.observeGoals(status).flatMapLatest { goals ->
            if (goals.isEmpty()) {
                flowOf(GoalsListUiState(status, emptyList(), isLoading = false))
            } else {
                combine(goals.map(::observeCard)) { cards ->
                    GoalsListUiState(status, cards.toList(), isLoading = false)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsListUiState())

    fun selectStatus(status: GoalStatus) {
        selectedStatus.value = status
    }

    private fun observeCard(goal: GoalModel): Flow<GoalCardUiState> {
        val today = LocalDate.now()
        return combine(
            repository.observeTasks(goal.id),
            repository.observeTaskLogs(goal.createdOn(), today),
        ) { tasks, logs ->
            val todayLogs = logs.filter { it.date == today && it.completed }
                .map { it.taskId }
                .toSet()
            val scheduled = tasks.filter { it.isScheduledOn(today) }
            val currentStreak = tasks.maxOfOrNull { task ->
                val taskLogs = logs.filter { it.taskId == task.id && it.completed }
                compute(
                    TaskSpec(task.recurrence, task.daysMask, task.createdOn()),
                    taskLogs.map { it.date }.toSet(),
                    today,
                ).currentStreak
            } ?: 0
            GoalCardUiState(
                goal = goal,
                dailyProgress = dailyProgress(
                    scheduled.size,
                    scheduled.count { it.id in todayLogs },
                ),
                scheduledTaskCount = scheduled.size,
                currentStreak = currentStreak,
            )
        }
    }
}

internal fun GoalModel.createdOn(): LocalDate =
    Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

internal fun TaskModel.createdOn(): LocalDate =
    Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()

internal fun TaskModel.isScheduledOn(date: LocalDate): Boolean =
    recurrence == Recurrence.DAILY ||
        daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0
