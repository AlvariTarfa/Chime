package com.savatech.chimelauncher.domain.usecase

import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskLogModel
import com.savatech.chimelauncher.domain.model.TaskModel
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveTodayOverviewUseCaseTest {
    @Test
    fun overviewIncludesScheduledTasksAndTheirCompletionState() = runBlocking {
        val repository = FakeGoalRepository()
        val date = LocalDate.of(2024, 4, 1)
        repository.goals["goal"] = goal("goal")
        repository.tasksByGoal["goal"] = listOf(
            task("daily", Recurrence.DAILY, 0),
            task("monday", Recurrence.DAYS_OF_WEEK, 1),
            task("tuesday", Recurrence.DAYS_OF_WEEK, 1 shl 1),
        )
        repository.logs["daily" to date] = TaskLogModel(
            taskId = "daily",
            date = date,
            value = null,
            completed = true,
        )

        val overview = ObserveTodayOverviewUseCase(repository)(date).first().single()

        assertEquals(listOf("daily", "monday"), overview.scheduledTasks.map { it.task.id })
        assertTrue(overview.scheduledTasks.first().completed)
        assertFalse(overview.scheduledTasks.last().completed)
        assertEquals(0.5f, overview.dailyProgress)
    }

    private fun goal(id: String) = GoalModel(
        id = id,
        title = id,
        why = null,
        category = "PERSONAL",
        targetDate = null,
        unit = null,
        targetValue = null,
        status = GoalStatus.ACTIVE,
        createdAt = 0,
    )

    private fun task(id: String, recurrence: Recurrence, daysMask: Int) = TaskModel(
        id = id,
        goalId = "goal",
        title = id,
        recurrence = recurrence,
        daysMask = daysMask,
        reminderTime = null,
        createdAt = 0,
    )
}
