package com.savatech.chimelauncher.feature.goals

import androidx.lifecycle.SavedStateHandle
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.domain.usecase.FakeGoalRepository
import com.savatech.chimelauncher.domain.usecase.SetDailyPrioritiesResult
import com.savatech.chimelauncher.domain.usecase.SetDailyPrioritiesUseCase
import com.savatech.chimelauncher.domain.usecase.ToggleTaskCompletionUseCase
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class GoalViewModelsTest {
    private val scheduler = TestCoroutineScheduler()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(scheduler)

    @Test
    fun goalCreationShowsValidationAndPersistsValidGoal() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeGoalRepository()
        val viewModel = GoalEditViewModel(
            repository,
            SavedStateHandle(mapOf("goalId" to "new")),
        )
        val categories = listOf("Health", "Learning", "Career", "Finance", "Relationships", "Personal", "Other")

        viewModel.uiState.test {
            awaitItem()
            viewModel.updateCategory("Other")
            awaitItem()
            viewModel.save(categories)
            assertEquals(GoalTitleError.REQUIRED, awaitItem().titleError)
            assertTrue(repository.goals.isEmpty())

            viewModel.updateTitle("Practice daily")
            awaitItem()
            viewModel.save(categories)
            advanceUntilIdle()
            assertTrue(awaitItem().saved)
            assertEquals("Practice daily", repository.goals.values.single().title)
        }
    }

    @Test
    fun checkingTodayTaskUpdatesDetailStateAndProgress() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeGoalRepository()
        val today = LocalDate.now()
        val goal = goal("health", GoalStatus.ACTIVE)
        val task = TaskModel(
            id = "walk",
            goalId = goal.id,
            title = "Walk",
            recurrence = Recurrence.DAILY,
            daysMask = 0,
            reminderTime = null,
            createdAt = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        repository.createGoal(goal)
        repository.addTask(task)
        val viewModel = GoalDetailViewModel(
            repository,
            ToggleTaskCompletionUseCase(repository),
            SavedStateHandle(mapOf("goalId" to goal.id)),
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().isLoading)
            advanceUntilIdle()
            val initial = awaitItem()
            assertFalse(initial.tasks.single().completedToday)
            assertEquals(0f, initial.progress)

            viewModel.toggleToday(task.id, true)
            advanceUntilIdle()
            val updated = awaitItem()
            assertTrue(updated.tasks.single().completedToday)
            assertEquals(1f, updated.progress)
        }
    }

    @Test
    fun priorityUseCaseErrorsAreExposedInUiState() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeGoalRepository()
        repository.createGoal(goal("paused", GoalStatus.PAUSED))
        val viewModel = PriorityViewModel(repository, SetDailyPrioritiesUseCase(repository))

        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()
            viewModel.toggle("paused", true)
            awaitItem()
            viewModel.save()
            advanceUntilIdle()
            assertEquals(SetDailyPrioritiesResult.GoalNotActive("paused"), awaitItem().error)
        }
    }

    @Test
    fun statusFilterShowsGoalsInSelectedCategory() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeGoalRepository()
        repository.createGoal(goal("active", GoalStatus.ACTIVE))
        repository.createGoal(goal("paused", GoalStatus.PAUSED))
        val viewModel = GoalsListViewModel(repository)

        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()
            val active = awaitItem()
            assertEquals(listOf("active"), active.goals.map { it.goal.id })
            viewModel.selectStatus(GoalStatus.PAUSED)
            advanceUntilIdle()
            val paused = awaitItem()
            assertEquals(GoalStatus.PAUSED, paused.selectedStatus)
            assertEquals(listOf("paused"), paused.goals.map { it.goal.id })
        }
    }

    private fun goal(id: String, status: GoalStatus) = GoalModel(
        id = id,
        title = id,
        why = null,
        category = "Health",
        targetDate = null,
        unit = null,
        targetValue = null,
        status = status,
        createdAt = System.currentTimeMillis(),
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    scheduler: TestCoroutineScheduler,
) : TestWatcher() {
    val dispatcher = StandardTestDispatcher(scheduler)

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
