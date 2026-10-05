package com.savatech.chimelauncher.domain.usecase

import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.progress.dailyProgress
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

data class ScheduledTaskOverview(
    val task: TaskModel,
    val completed: Boolean,
)

data class GoalOverview(
    val goal: GoalModel,
    val scheduledTasks: List<ScheduledTaskOverview>,
    val dailyProgress: Float?,
)

class ObserveTodayOverviewUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(date: LocalDate): Flow<List<GoalOverview>> =
        goalRepository.observeGoals(GoalStatus.ACTIVE).flatMapLatest { goals ->
            if (goals.isEmpty()) {
                flowOf(emptyList())
            } else {
                val perGoalFlows = goals.map { goal -> observeGoal(goal, date) }
                combine(perGoalFlows) { overviews -> overviews.toList() }
            }
        }

    private fun observeGoal(goal: GoalModel, date: LocalDate): Flow<GoalOverview> =
        combine(
            goalRepository.observeTasks(goal.id),
            goalRepository.observeTaskLogs(date, date),
        ) { tasks, logs ->
            val completedTaskIds = logs.asSequence()
                .filter { it.completed }
                .map { it.taskId }
                .toSet()
            val scheduled = tasks
                .filter { it.isScheduledOn(date) }
                .map { task -> ScheduledTaskOverview(task, task.id in completedTaskIds) }
            GoalOverview(
                goal = goal,
                scheduledTasks = scheduled,
                dailyProgress = dailyProgress(
                    scheduledTaskCount = scheduled.size,
                    completedCount = scheduled.count { it.completed },
                ),
            )
        }
}

private fun TaskModel.isScheduledOn(date: LocalDate): Boolean =
    recurrence == Recurrence.DAILY ||
        daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0
