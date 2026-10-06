package com.savatech.chimelauncher.domain.usecase

import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.DailyPriorityModel
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.TaskLogModel
import com.savatech.chimelauncher.domain.model.TaskModel
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeGoalRepository : GoalRepository {
    val goals = mutableMapOf<String, GoalModel>()
    val tasksByGoal = mutableMapOf<String, List<TaskModel>>()
    val logs = mutableMapOf<Pair<String, LocalDate>, TaskLogModel>()
    var priorityDate: LocalDate? = null
    var priorityGoalIds: List<String> = emptyList()
    private val goalState = MutableStateFlow<List<GoalModel>>(emptyList())
    private val taskStates = mutableMapOf<String, MutableStateFlow<List<TaskModel>>>()
    private val logState = MutableStateFlow<List<TaskLogModel>>(emptyList())
    private val priorityState = MutableStateFlow<List<DailyPriorityModel>>(emptyList())

    override suspend fun createGoal(goal: GoalModel) {
        goals[goal.id] = goal
        goalState.value = goals.values.toList()
    }

    override suspend fun updateGoal(goal: GoalModel): Boolean {
        if (goal.id !in goals) return false
        goals[goal.id] = goal
        goalState.value = goals.values.toList()
        return true
    }

    override suspend fun deleteGoal(goalId: String): Boolean {
        val removed = goals.remove(goalId) != null
        if (removed) goalState.value = goals.values.toList()
        return removed
    }

    override fun observeGoals(status: GoalStatus): Flow<List<GoalModel>> =
        goalState.map { all ->
            (all + goals.values).distinctBy { it.id }.filter { it.status == status }
        }

    override suspend fun getGoal(goalId: String): GoalModel? = goals[goalId]

    override suspend fun addTask(task: TaskModel) {
        tasksByGoal[task.goalId] = tasksByGoal[task.goalId].orEmpty() + task
        taskStates.getOrPut(task.goalId) { MutableStateFlow(emptyList()) }.value =
            tasksByGoal.getValue(task.goalId)
    }

    override suspend fun updateTask(task: TaskModel): Boolean {
        val tasks = tasksByGoal[task.goalId].orEmpty()
        if (tasks.none { it.id == task.id }) return false
        tasksByGoal[task.goalId] = tasks.map { if (it.id == task.id) task else it }
        taskStates.getOrPut(task.goalId) { MutableStateFlow(emptyList()) }.value =
            tasksByGoal.getValue(task.goalId)
        return true
    }

    override suspend fun deleteTask(taskId: String): Boolean {
        val goalId = tasksByGoal.entries.firstOrNull { entry -> entry.value.any { it.id == taskId } }
            ?.key ?: return false
        tasksByGoal[goalId] = tasksByGoal.getValue(goalId).filterNot { it.id == taskId }
        taskStates.getOrPut(goalId) { MutableStateFlow(emptyList()) }.value = tasksByGoal.getValue(goalId)
        return true
    }

    override suspend fun getTask(taskId: String): TaskModel? =
        tasksByGoal.values.flatten().firstOrNull { it.id == taskId }

    override fun observeTasks(goalId: String): Flow<List<TaskModel>> =
        taskStates.getOrPut(goalId) { MutableStateFlow(tasksByGoal[goalId].orEmpty()) }

    override suspend fun upsertTaskLog(log: TaskLogModel) {
        logs[log.taskId to log.date] = log
        logState.value = logs.values.toList()
    }

    override suspend fun getTaskLog(taskId: String, date: LocalDate): TaskLogModel? =
        logs[taskId to date]

    override fun observeTaskLogs(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<TaskLogModel>> =
        logState.map { items ->
            (items + logs.values).distinctBy { it.taskId to it.date }
                .filter { it.date in startDate..endDate }
        }

    override suspend fun setDailyPriorities(date: LocalDate, goalIds: List<String>) {
        priorityDate = date
        priorityGoalIds = goalIds
        priorityState.value = goalIds.mapIndexed { index, id ->
            DailyPriorityModel(date, id, index)
        }
    }

    override fun observeDailyPriorities(date: LocalDate): Flow<List<DailyPriorityModel>> =
        priorityState.map { items -> items.filter { it.date == date } }

    override suspend fun clearDailyPriorities(date: LocalDate) {
        priorityDate = date
        priorityGoalIds = emptyList()
        priorityState.value = emptyList()
    }
}
