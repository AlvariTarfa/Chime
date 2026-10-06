package com.savatech.chimelauncher.feature.onboarding

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import com.savatech.chimelauncher.data.apps.AppClassificationStore
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.data.settings.FrictionLevel
import com.savatech.chimelauncher.data.settings.OnboardingSettings
import com.savatech.chimelauncher.data.usage.OnboardingUsageAccess
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.domain.model.DailyPriorityModel
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.TaskLogModel
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.domain.usecase.SetDailyPrioritiesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun nextBackAndSkipAdvanceThroughTheExpectedSteps() = runTest(dispatcher) {
        val savedState = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle = savedState)

        assertEquals(OnboardingStep.WELCOME_GOALS, viewModel.uiState.value.step)
        viewModel.next()
        assertEquals(OnboardingStep.WELCOME_FRICTION, viewModel.uiState.value.step)
        assertEquals(OnboardingStep.WELCOME_FRICTION.name, savedState.get<String>("onboarding_step"))
        viewModel.back()
        assertEquals(OnboardingStep.WELCOME_GOALS, viewModel.uiState.value.step)
        viewModel.next()
        viewModel.next()
        assertEquals(OnboardingStep.DEFAULT_LAUNCHER, viewModel.uiState.value.step)
        viewModel.skip()
        assertEquals(OnboardingStep.USAGE_ACCESS, viewModel.uiState.value.step)
        viewModel.skip()
        assertEquals(OnboardingStep.NOTIFICATIONS, viewModel.uiState.value.step)
        viewModel.skip()
        assertEquals(OnboardingStep.FIRST_GOALS, viewModel.uiState.value.step)
        viewModel.skip()
        assertEquals(OnboardingStep.CLASSIFY_APPS, viewModel.uiState.value.step)
        viewModel.skip()
        assertEquals(OnboardingStep.FRICTION, viewModel.uiState.value.step)
        viewModel.skip()
        assertEquals(OnboardingStep.DONE, viewModel.uiState.value.step)
    }

    @Test
    fun doneStepStoresCompletionAndInvokesNavigationCallback() = runTest(dispatcher) {
        val preferences = FakeOnboardingSettings()
        val viewModel = createViewModel(
            settings = preferences,
            savedStateHandle = SavedStateHandle(mapOf("onboarding_step" to OnboardingStep.DONE.name)),
        )
        var navigated = false

        viewModel.completeOnboarding { navigated = true }
        advanceUntilIdle()

        assertTrue(preferences.completed.value)
        assertEquals(true, viewModel.uiState.value.onboardingCompleted)
        assertTrue(navigated)
    }

    private fun createViewModel(
        settings: FakeOnboardingSettings = FakeOnboardingSettings(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = OnboardingViewModel(
        savedStateHandle = savedStateHandle,
        settings = settings,
        appRepository = FakeAppRepository(),
        classificationStore = FakeClassificationStore(),
        goalRepository = EmptyGoalRepository(),
        setDailyPriorities = SetDailyPrioritiesUseCase(EmptyGoalRepository()),
        usageAccess = FakeUsageAccess(),
        defaultHomeStatus = object : DefaultHomeStatus {
            override suspend fun isDefaultHome() = false
        },
        clock = Clock.fixed(
            LocalDate.of(2026, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC,
        ),
        ioDispatcher = dispatcher,
    )
}

private class FakeOnboardingSettings : OnboardingSettings {
    val completed = MutableStateFlow(false)
    override val onboardingCompleted: Flow<Boolean> = completed
    override val frictionLevel: Flow<FrictionLevel> = flowOf(FrictionLevel.BALANCED)
    override val frictionConfigured: Flow<Boolean> = flowOf(false)
    override suspend fun setOnboardingCompleted(value: Boolean) {
        completed.value = value
    }
    override suspend fun setFrictionLevel(value: FrictionLevel) = Unit
    override suspend fun setBaseDelaySeconds(value: Int) = Unit
}

private class FakeAppRepository : AppRepository {
    override val apps: StateFlow<List<AppInfo>> =
        MutableStateFlow<List<AppInfo>>(emptyList()).asStateFlow()
    override val isLoaded: StateFlow<Boolean> = MutableStateFlow<Boolean>(true).asStateFlow()
    override suspend fun launch(app: AppInfo): Result<Unit> = Result.success(Unit)
}

private class FakeClassificationStore : AppClassificationStore {
    override suspend fun getAllConfigs(): Map<String, AppConfig> = emptyMap()
    override suspend fun setCategories(categories: Map<String, AppCategory>) = Unit
}

private class FakeUsageAccess : OnboardingUsageAccess {
    override val hasPermission: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override fun refresh() = Unit
    override fun settingsIntent() = Intent("usage")
}

private class EmptyGoalRepository : GoalRepository {
    override suspend fun createGoal(goal: GoalModel) = Unit
    override suspend fun updateGoal(goal: GoalModel) = false
    override suspend fun deleteGoal(goalId: String) = false
    override fun observeGoals(status: GoalStatus): Flow<List<GoalModel>> = flowOf(emptyList())
    override suspend fun getGoal(goalId: String): GoalModel? = null
    override suspend fun addTask(task: TaskModel) = Unit
    override suspend fun updateTask(task: TaskModel) = false
    override suspend fun deleteTask(taskId: String) = false
    override suspend fun getTask(taskId: String): TaskModel? = null
    override fun observeTasks(goalId: String): Flow<List<TaskModel>> = flowOf(emptyList())
    override suspend fun upsertTaskLog(log: TaskLogModel) = Unit
    override suspend fun getTaskLog(taskId: String, date: LocalDate): TaskLogModel? = null
    override fun observeTaskLogs(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<TaskLogModel>> = flowOf(emptyList())
    override suspend fun setDailyPriorities(date: LocalDate, goalIds: List<String>) = Unit
    override fun observeDailyPriorities(date: LocalDate): Flow<List<DailyPriorityModel>> =
        flowOf(emptyList())
    override suspend fun clearDailyPriorities(date: LocalDate) = Unit
}
