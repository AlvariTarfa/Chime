package com.savatech.chimelauncher.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.checkin.CheckInRepository
import com.savatech.chimelauncher.data.db.entities.CheckIn
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.CheckInType
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskLogModel
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.domain.streak.TaskSpec
import com.savatech.chimelauncher.domain.streak.compute
import com.savatech.chimelauncher.service.SchedulerRescheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class WeeklyGoalReview(
    val goal: GoalModel,
    val completionRate: Float,
    val currentStreak: Int,
)

data class CheckInUiState(
    val goals: List<GoalModel> = emptyList(),
    val selectedGoalIds: List<String> = emptyList(),
    val completedTasks: List<Pair<TaskModel, TaskLogModel>> = emptyList(),
    val weeklyReview: List<WeeklyGoalReview> = emptyList(),
    val energy: Int = 3,
    val intention: String = "",
    val blockedBy: String = "",
    val win: String = "",
    val historySearch: String = "",
    val isLoading: Boolean = true,
    val saved: Boolean = false,
    val error: Boolean = false,
)

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val checkInRepository: CheckInRepository,
    private val schedulerFacade: SchedulerRescheduler,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()
    val history: StateFlow<List<CheckIn>> = checkInRepository.observeHistory().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    private val date = LocalDate.now(clock)

    init {
        viewModelScope.launch {
            try {
                val goals = GoalStatus.entries.flatMap { goalRepository.observeGoals(it).first() }
                val activeGoals = goals.filter { it.status == GoalStatus.ACTIVE }
                val priorities = goalRepository.observeDailyPriorities(date).first()
                    .sortedBy { it.position }.map { it.goalId }
                val todayLogs = goalRepository.observeTaskLogs(date, date).first()
                    .associateBy(TaskLogModel::taskId)
                val completedTasks = goals.flatMap { goal ->
                    goalRepository.observeTasks(goal.id).first()
                        .filter { task -> todayLogs[task.id]?.completed == true }
                        .map { task -> task to todayLogs.getValue(task.id) }
                }
                val review = buildWeeklyReview(goals, date)
                _uiState.update {
                    it.copy(
                        goals = goals,
                        selectedGoalIds = priorities.filter { id -> activeGoals.any { goal -> goal.id == id } },
                        completedTasks = completedTasks,
                        weeklyReview = review,
                        isLoading = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = true) }
            }
        }
    }

    fun togglePriority(goalId: String, selected: Boolean) {
        _uiState.update { state ->
            val ids = state.selectedGoalIds.toMutableList()
            if (selected && goalId !in ids && ids.size < MAX_PRIORITIES) ids += goalId
            if (!selected) ids.remove(goalId)
            state.copy(selectedGoalIds = ids)
        }
    }

    fun setEnergy(value: Int) {
        if (value in 1..5) _uiState.update { it.copy(energy = value) }
    }
    fun setIntention(value: String) = _uiState.update { it.copy(intention = value) }
    fun setBlockedBy(value: String) = _uiState.update { it.copy(blockedBy = value) }
    fun setWin(value: String) = _uiState.update { it.copy(win = value) }
    fun setHistorySearch(value: String) = _uiState.update { it.copy(historySearch = value) }

    fun saveMorning() = save {
        val state = _uiState.value
        goalRepository.setDailyPriorities(date, state.selectedGoalIds)
        val payload = MorningCheckInPayload(
            state.selectedGoalIds,
            state.energy,
            state.intention.trim().ifBlank { null },
        )
        CheckIn(
            type = CheckInType.MORNING.name,
            date = date.toString(),
            mood = state.energy,
            notes = payload.intention,
            payloadJson = Json.encodeToString(payload),
        )
    }

    fun saveEvening() = save {
        val state = _uiState.value
        goalRepository.setDailyPriorities(date.plusDays(1), state.selectedGoalIds)
        val payload = EveningCheckInPayload(
            state.completedTasks.map { it.first.id },
            state.blockedBy.trim().ifBlank { null },
            state.win.trim().ifBlank { null },
            state.selectedGoalIds,
        )
        CheckIn(
            type = CheckInType.EVENING.name,
            date = date.toString(),
            mood = null,
            notes = listOfNotNull(payload.blockedBy, payload.win).joinToString("\n").ifBlank { null },
            payloadJson = Json.encodeToString(payload),
        )
    }

    fun saveWeeklyReview() = save {
        val rates = _uiState.value.weeklyReview.associate { it.goal.id to it.completionRate.toDouble() }
        val streaks = _uiState.value.weeklyReview.associate { it.goal.id to it.currentStreak }
        val payload = WeeklyReviewPayload(rates, streaks)
        CheckIn(
            type = CheckInType.WEEKLY.name,
            date = date.toString(),
            mood = null,
            notes = null,
            payloadJson = Json.encodeToString(payload),
        )
    }

    fun changeGoalStatus(goal: GoalModel, status: GoalStatus) {
        viewModelScope.launch {
            try {
                if (!goalRepository.updateGoal(goal.copy(status = status))) error("Goal update failed")
                schedulerFacade.rescheduleAll()
                _uiState.update { current ->
                    current.copy(goals = current.goals.map { if (it.id == goal.id) it.copy(status = status) else it })
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(error = true) }
            }
        }
    }

    private fun save(createRecord: suspend () -> CheckIn) {
        viewModelScope.launch {
            try {
                checkInRepository.save(createRecord())
                _uiState.update { it.copy(saved = true, error = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(error = true) }
            }
        }
    }

    private suspend fun buildWeeklyReview(goals: List<GoalModel>, today: LocalDate): List<WeeklyGoalReview> {
        val start = today.minusDays(6)
        val logs = goalRepository.observeTaskLogs(start, today).first()
        return goals.map { goal ->
            val tasks = goalRepository.observeTasks(goal.id).first()
            var scheduledCount = 0
            var completedCount = 0
            val streaks = mutableListOf<Int>()
            tasks.forEach { task ->
                val completions = logs.filter { it.taskId == task.id && it.completed }.map { it.date }.toSet()
                streaks += currentStreak(task, completions, today)
                for (dayOffset in 0..6) {
                    val day = start.plusDays(dayOffset.toLong())
                    if (task.isScheduledOn(day)) {
                        scheduledCount++
                        if (day in completions) completedCount++
                    }
                }
            }
            WeeklyGoalReview(
                goal = goal,
                completionRate = if (scheduledCount == 0) 0f else completedCount.toFloat() / scheduledCount,
                currentStreak = streaks.maxOrNull() ?: 0,
            )
        }
    }

    private fun currentStreak(task: TaskModel, completed: Set<LocalDate>, today: LocalDate): Int {
        val createdOn = task.createdAtDate()
        if (createdOn.isAfter(today)) return 0
        return compute(TaskSpec(task.recurrence, task.daysMask, createdOn), completed, today).currentStreak
    }

    private fun TaskModel.createdAtDate(): LocalDate =
        java.time.Instant.ofEpochMilli(createdAt).atZone(clock.zone).toLocalDate()

    private fun TaskModel.isScheduledOn(day: LocalDate): Boolean =
        recurrence == Recurrence.DAILY || daysMask and (1 shl (day.dayOfWeek.value - 1)) != 0

    private companion object {
        const val MAX_PRIORITIES = 3
    }
}

@Serializable
private data class MorningCheckInPayload(
    val priorityGoalIds: List<String>,
    val energy: Int,
    val intention: String?,
)

@Serializable
private data class EveningCheckInPayload(
    val completedTaskIds: List<String>,
    val blockedBy: String?,
    val win: String?,
    val carryOverGoalIds: List<String>,
)

@Serializable
private data class WeeklyReviewPayload(
    val completionRates: Map<String, Double>,
    val currentStreaks: Map<String, Int>,
)
