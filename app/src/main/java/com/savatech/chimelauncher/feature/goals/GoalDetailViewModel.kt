package com.savatech.chimelauncher.feature.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskLogModel
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.domain.progress.dailyProgress
import com.savatech.chimelauncher.domain.streak.TaskSpec
import com.savatech.chimelauncher.domain.streak.StreakResult
import com.savatech.chimelauncher.domain.streak.compute
import com.savatech.chimelauncher.domain.usecase.ToggleTaskCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val task: TaskModel,
    val completedToday: Boolean,
    val todayValue: String,
    val streak: StreakResult,
)

data class GoalDetailUiState(
    val goal: GoalModel? = null,
    val tasks: List<TaskDetailUiState> = emptyList(),
    val progress: Float? = null,
    val recentLogs: List<TaskLogModel> = emptyList(),
    val isLoading: Boolean = true,
    val showDeleteGoalConfirmation: Boolean = false,
    val deletingTaskId: String? = null,
    val failure: GoalDetailFailure? = null,
)

enum class GoalDetailFailure { NOT_FOUND, UPDATE_FAILED, DELETE_FAILED }

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    private val repository: GoalRepository,
    private val toggleTaskCompletion: ToggleTaskCompletionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val goalId: String = checkNotNull(savedStateHandle["goalId"])
    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()
    private val today = LocalDate.now()
    private var goalDeleted = false

    init {
        viewModelScope.launch {
            val goal = repository.getGoal(goalId)
            if (goal == null) {
                _uiState.update { it.copy(isLoading = false, failure = GoalDetailFailure.NOT_FOUND) }
                return@launch
            }
            combine(
                repository.observeTasks(goalId),
                repository.observeTaskLogs(goal.createdOn(), today),
            ) { tasks, logs ->
                val byTask = logs.groupBy(TaskLogModel::taskId)
                val tasksUi = tasks.map { task ->
                    val taskLogs = byTask[task.id].orEmpty()
                    TaskDetailUiState(
                        task = task,
                        completedToday = taskLogs.any { it.date == today && it.completed },
                        todayValue = taskLogs.firstOrNull { it.date == today }?.value?.toString().orEmpty(),
                        streak = compute(
                            TaskSpec(task.recurrence, task.daysMask, task.createdOn()),
                            taskLogs.filter(TaskLogModel::completed).map(TaskLogModel::date).toSet(),
                            today,
                        ),
                    )
                }
                val scheduled = tasksUi.filter { it.task.isScheduledOn(today) }
                val completedIds = tasksUi.filter(TaskDetailUiState::completedToday)
                    .map { it.task.id }.toSet()
                GoalDetailUiState(
                    goal = goal,
                    tasks = tasksUi,
                    progress = dailyProgress(
                        scheduled.size,
                        scheduled.count { it.task.id in completedIds },
                    ),
                    recentLogs = logs.sortedByDescending(TaskLogModel::date).take(10),
                    isLoading = false,
                )
            }.collect { state ->
                if (!goalDeleted) _uiState.value = state
            }
        }
    }

    fun toggleToday(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            try {
                toggleTaskCompletion(taskId, today, completed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(failure = GoalDetailFailure.UPDATE_FAILED) }
            }
        }
    }

    fun saveTask(
        taskId: String?,
        title: String,
        recurrence: Recurrence,
        daysMask: Int,
        reminderTime: String?,
        todayValue: String,
    ) {
        val goal = _uiState.value.goal ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            val existing = taskId?.let { id -> _uiState.value.tasks.firstOrNull { it.task.id == id }?.task }
            val task = TaskModel(
                id = taskId ?: UUID.randomUUID().toString(),
                goalId = goal.id,
                title = title.trim(),
                recurrence = recurrence,
                daysMask = if (recurrence == Recurrence.DAILY) 0 else daysMask,
                reminderTime = reminderTime,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            try {
                if (existing == null) repository.addTask(task)
                else if (!repository.updateTask(task)) {
                    _uiState.update { it.copy(failure = GoalDetailFailure.UPDATE_FAILED) }
                    return@launch
                }
                todayValue.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }?.let { value ->
                    val existingLog = repository.getTaskLog(task.id, today)
                    repository.upsertTaskLog(
                        TaskLogModel(
                            id = existingLog?.id ?: 0,
                            taskId = task.id,
                            date = today,
                            value = value,
                            completed = existingLog?.completed ?: false,
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(failure = GoalDetailFailure.UPDATE_FAILED) }
            }
        }
    }

    fun requestDeleteTask(taskId: String) = _uiState.update { it.copy(deletingTaskId = taskId) }
    fun dismissDeleteTask() = _uiState.update { it.copy(deletingTaskId = null) }
    fun deleteTask() {
        val taskId = _uiState.value.deletingTaskId ?: return
        viewModelScope.launch {
            try {
                if (!repository.deleteTask(taskId)) _uiState.update {
                    it.copy(failure = GoalDetailFailure.DELETE_FAILED, deletingTaskId = null)
                } else _uiState.update { it.copy(deletingTaskId = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(failure = GoalDetailFailure.DELETE_FAILED, deletingTaskId = null)
                }
            }
        }
    }

    fun updateStatus(status: GoalStatus) {
        viewModelScope.launch {
            try {
                val goal = repository.getGoal(goalId)
                if (goal == null || !repository.updateGoal(goal.copy(status = status))) {
                    _uiState.update { it.copy(failure = GoalDetailFailure.UPDATE_FAILED) }
                } else {
                    _uiState.update { it.copy(goal = goal.copy(status = status)) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(failure = GoalDetailFailure.UPDATE_FAILED) }
            }
        }
    }

    fun showDeleteGoalConfirmation() =
        _uiState.update { it.copy(showDeleteGoalConfirmation = true) }
    fun dismissDeleteGoalConfirmation() =
        _uiState.update { it.copy(showDeleteGoalConfirmation = false) }
    fun deleteGoal() {
        viewModelScope.launch {
            try {
                if (repository.deleteGoal(goalId)) {
                    goalDeleted = true
                    _uiState.update { it.copy(goal = null, showDeleteGoalConfirmation = false) }
                } else {
                    _uiState.update { it.copy(failure = GoalDetailFailure.DELETE_FAILED) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(failure = GoalDetailFailure.DELETE_FAILED) }
            }
        }
    }
}
