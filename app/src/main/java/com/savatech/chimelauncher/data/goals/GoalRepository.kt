package com.savatech.chimelauncher.data.goals

import com.savatech.chimelauncher.data.db.dao.DailyPriorityDao
import com.savatech.chimelauncher.data.db.dao.GoalDao
import com.savatech.chimelauncher.data.db.dao.TaskDao
import com.savatech.chimelauncher.data.db.dao.TaskLogDao
import com.savatech.chimelauncher.domain.model.DailyPriorityModel
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.TaskLogModel
import com.savatech.chimelauncher.domain.model.TaskModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GoalRepository {
    suspend fun createGoal(goal: GoalModel)
    suspend fun updateGoal(goal: GoalModel): Boolean
    suspend fun deleteGoal(goalId: String): Boolean
    fun observeGoals(status: GoalStatus): Flow<List<GoalModel>>
    suspend fun getGoal(goalId: String): GoalModel?

    suspend fun addTask(task: TaskModel)
    suspend fun updateTask(task: TaskModel): Boolean
    suspend fun deleteTask(taskId: String): Boolean
    fun observeTasks(goalId: String): Flow<List<TaskModel>>

    suspend fun upsertTaskLog(log: TaskLogModel)
    suspend fun getTaskLog(taskId: String, date: LocalDate): TaskLogModel?
    fun observeTaskLogs(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskLogModel>>

    suspend fun setDailyPriorities(date: LocalDate, goalIds: List<String>)
    fun observeDailyPriorities(date: LocalDate): Flow<List<DailyPriorityModel>>
    suspend fun clearDailyPriorities(date: LocalDate)
}

@Singleton
class RoomGoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val taskDao: TaskDao,
    private val taskLogDao: TaskLogDao,
    private val dailyPriorityDao: DailyPriorityDao,
) : GoalRepository {
    override suspend fun createGoal(goal: GoalModel) {
        goalDao.insert(goal.toEntity())
    }

    override suspend fun updateGoal(goal: GoalModel): Boolean =
        goalDao.update(goal.toEntity()) > 0

    override suspend fun deleteGoal(goalId: String): Boolean {
        val goal = goalDao.getById(goalId) ?: return false
        return goalDao.delete(goal) > 0
    }

    override fun observeGoals(status: GoalStatus): Flow<List<GoalModel>> =
        goalDao.observeByStatus(status.name).map { goals -> goals.map { it.toModel() } }

    override suspend fun getGoal(goalId: String): GoalModel? =
        goalDao.getById(goalId)?.toModel()

    override suspend fun addTask(task: TaskModel) {
        taskDao.insert(task.toEntity())
    }

    override suspend fun updateTask(task: TaskModel): Boolean =
        taskDao.update(task.toEntity()) > 0

    override suspend fun deleteTask(taskId: String): Boolean {
        val task = taskDao.getById(taskId) ?: return false
        return taskDao.delete(task) > 0
    }

    override fun observeTasks(goalId: String): Flow<List<TaskModel>> =
        taskDao.observeByGoal(goalId).map { tasks -> tasks.map { it.toModel() } }

    override suspend fun upsertTaskLog(log: TaskLogModel) {
        taskLogDao.upsert(log.toEntity())
    }

    override suspend fun getTaskLog(taskId: String, date: LocalDate): TaskLogModel? =
        taskLogDao.getForTaskAndDate(taskId, date.toString())?.toModel()

    override fun observeTaskLogs(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<TaskLogModel>> =
        taskLogDao.observeBetweenDates(startDate.toString(), endDate.toString())
            .map { logs -> logs.map { it.toModel() } }

    override suspend fun setDailyPriorities(date: LocalDate, goalIds: List<String>) {
        val priorities = goalIds.mapIndexed { index, goalId ->
            DailyPriorityModel(date = date, goalId = goalId, position = index).toEntity()
        }
        dailyPriorityDao.replaceForDate(date.toString(), priorities)
    }

    override fun observeDailyPriorities(date: LocalDate): Flow<List<DailyPriorityModel>> =
        dailyPriorityDao.observeForDate(date.toString())
            .map { priorities -> priorities.map { it.toModel() } }

    override suspend fun clearDailyPriorities(date: LocalDate) {
        dailyPriorityDao.deleteForDate(date.toString())
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GoalRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindGoalRepository(repository: RoomGoalRepository): GoalRepository
}
