package com.savatech.chimelauncher.feature.home

import app.cash.turbine.test
import com.savatech.chimelauncher.data.apps.AppConfigResult
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.IconCache
import com.savatech.chimelauncher.core.launch.LaunchResult
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.domain.usecase.FakeGoalRepository
import com.savatech.chimelauncher.domain.usecase.ObserveTodayOverviewUseCase
import com.savatech.chimelauncher.domain.usecase.ToggleTaskCompletionUseCase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val scheduler = TestCoroutineScheduler()

    @get:Rule
    val mainDispatcherRule = HomeMainDispatcherRule(scheduler)

    @Test
    fun emptyStateHasNoGoalsOrPriorities() = runTest(mainDispatcherRule.dispatcher) {
        val repository = FakeGoalRepository()
        val viewModel = createViewModel(repository, testClock())

        viewModel.uiState.test {
            runCurrent()
            assertFalse(viewModel.uiState.value.hasGoals)
            assertTrue(viewModel.uiState.value.priorityGoals.isEmpty())
            assertEquals(null, viewModel.uiState.value.nextTask)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun showsThreePrioritiesInSavedOrderAndCompletingTaskUpdatesProgress() =
        runTest(mainDispatcherRule.dispatcher) {
            val clock = testClock()
            val today = LocalDate.now(clock)
            val repository = FakeGoalRepository()
            repeat(3) { index ->
                val goal = goal("goal-$index")
                repository.createGoal(goal)
                repository.addTask(task("task-$index", goal.id, clock))
            }
            repository.setDailyPriorities(today, listOf("goal-2", "goal-0", "goal-1"))
            val viewModel = createViewModel(repository, clock)

            viewModel.uiState.test {
                runCurrent()
                val initial = viewModel.uiState.value
                assertEquals(listOf("goal-2", "goal-0", "goal-1"), initial.priorityGoals.map {
                    it.overview.goal.id
                })
                assertEquals("task-2", initial.nextTask?.task?.task?.id)

                viewModel.setTaskCompleted("task-2", true)
                runCurrent()

                val updated = viewModel.uiState.value
                assertEquals(1f, updated.priorityGoals.first().overview.dailyProgress)
                assertTrue(updated.priorityGoals.first().unfinishedTasks.isEmpty())
                assertEquals("task-0", updated.nextTask?.task?.task?.id)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun midnightRolloverLoadsPrioritiesForTheNewDate() = runTest(mainDispatcherRule.dispatcher) {
        val zone = ZoneId.of("UTC")
        val clock = MutableClock(
            ZonedDateTime.of(2026, 10, 5, 23, 59, 30, 0, zone).toInstant(),
            zone,
        )
        val repository = FakeGoalRepository()
        repository.createGoal(goal("goal"))
        repository.setDailyPriorities(LocalDate.now(clock), listOf("goal"))
        val viewModel = createViewModel(repository, clock)

        viewModel.uiState.test {
            runCurrent()
            assertEquals(listOf("goal"), viewModel.uiState.value.priorityGoals.map {
                it.overview.goal.id
            })

            clock.advanceSeconds(30)
            advanceTimeBy(30_000)
            runCurrent()

            assertEquals(LocalDate.of(2026, 10, 6), viewModel.uiState.value.currentTime.toLocalDate())
            assertTrue(viewModel.uiState.value.priorityGoals.isEmpty())
            assertTrue(viewModel.uiState.value.hasGoals)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(repository: FakeGoalRepository, clock: Clock) = HomeViewModel(
        appsSource = FakeHomeAppsSource(),
        goalRepository = repository,
        observeTodayOverview = ObserveTodayOverviewUseCase(repository),
        toggleTaskCompletion = ToggleTaskCompletionUseCase(repository),
        clock = clock,
    )

    private fun goal(id: String) = GoalModel(
        id = id,
        title = id,
        why = null,
        category = "Personal",
        targetDate = null,
        unit = null,
        targetValue = null,
        status = GoalStatus.ACTIVE,
        createdAt = 0L,
    )

    private fun task(id: String, goalId: String, clock: Clock) = TaskModel(
        id = id,
        goalId = goalId,
        title = id,
        recurrence = Recurrence.DAILY,
        daysMask = 0,
        reminderTime = null,
        createdAt = clock.millis(),
    )

    private fun testClock(): MutableClock = MutableClock(
        Instant.parse("2026-10-05T12:00:00Z"),
        ZoneId.of("UTC"),
    )
}

private class FakeHomeAppsSource : HomeAppsSource {
    override val state = MutableStateFlow(HomeAppsState())
    override val iconCache: IconCache? = null

    override suspend fun requestLaunch(app: AppInfo): LaunchResult = LaunchResult.Started
    override suspend fun setPinned(packageName: String, pinned: Boolean) = AppConfigResult.Updated
    override suspend fun hide(packageName: String) = Unit
    override suspend fun setCategory(packageName: String, category: AppCategory) = Unit
    override suspend fun setDailyLimit(packageName: String, minutes: Int?) = Unit
    override suspend fun dailyLimitWarnings(): List<String> = emptyList()
}

private class MutableClock(
    private var currentInstant: Instant,
    private val zone: ZoneId,
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(currentInstant, zone)
    override fun instant(): Instant = currentInstant

    fun advanceSeconds(seconds: Long) {
        currentInstant = currentInstant.plusSeconds(seconds)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeMainDispatcherRule(
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
