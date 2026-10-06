package com.savatech.chimelauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.core.launch.AppLauncher
import com.savatech.chimelauncher.core.launch.LaunchResult
import com.savatech.chimelauncher.data.apps.AppConfigRepository
import com.savatech.chimelauncher.data.apps.AppConfigResult
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.IconCache
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.data.settings.DrawerMode
import com.savatech.chimelauncher.data.settings.IconShape
import com.savatech.chimelauncher.data.settings.SwipeAppTarget
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.usecase.GoalOverview
import com.savatech.chimelauncher.domain.usecase.ObserveTodayOverviewUseCase
import com.savatech.chimelauncher.domain.usecase.ScheduledTaskOverview
import com.savatech.chimelauncher.domain.usecase.ToggleTaskCompletionUseCase
import com.savatech.chimelauncher.domain.usecase.ObserveVisibleAppsUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeTaskItem(
    val task: ScheduledTaskOverview,
    val goalId: String,
    val goalTitle: String,
)

data class HomePriorityGoal(
    val overview: GoalOverview,
    val unfinishedTasks: List<HomeTaskItem>,
)

data class HomeUiState(
    val currentTime: ZonedDateTime,
    val priorityGoals: List<HomePriorityGoal> = emptyList(),
    val hasGoals: Boolean = false,
    val nextTask: HomeTaskItem? = null,
    val pinnedApps: List<AppInfo> = emptyList(),
    val configs: Map<String, AppConfig> = emptyMap(),
    val drawerMode: DrawerMode = DrawerMode.TEXT,
    val iconShape: IconShape = IconShape.CIRCLE,
    val iconPackPackage: String? = null,
    val leftSwipeApp: AppInfo? = null,
    val rightSwipeApp: AppInfo? = null,
    val activeFocusModeName: String? = null,
    val taskUpdateFailed: Boolean = false,
)

data class HomeAppsState(
    val pinnedApps: List<AppInfo> = emptyList(),
    val configs: Map<String, AppConfig> = emptyMap(),
    val drawerMode: DrawerMode = DrawerMode.TEXT,
    val iconShape: IconShape = IconShape.CIRCLE,
    val iconPackPackage: String? = null,
    val leftSwipeApp: AppInfo? = null,
    val rightSwipeApp: AppInfo? = null,
)

private data class HomeAppPreferences(
    val drawerMode: DrawerMode,
    val iconShape: IconShape,
    val iconPackPackage: String?,
    val leftSwipeTarget: SwipeAppTarget?,
    val rightSwipeTarget: SwipeAppTarget?,
)

interface HomeAppsSource {
    val state: Flow<HomeAppsState>
    val iconCache: IconCache?
    suspend fun requestLaunch(app: AppInfo): LaunchResult
    suspend fun setPinned(packageName: String, pinned: Boolean): AppConfigResult
    suspend fun hide(packageName: String)
    suspend fun setCategory(packageName: String, category: com.savatech.chimelauncher.domain.model.AppCategory)
    suspend fun setDailyLimit(packageName: String, minutes: Int?)
    suspend fun dailyLimitWarnings(): List<String>
}

@Singleton
class DefaultHomeAppsSource @Inject constructor(
    private val appLauncher: AppLauncher,
    appConfigRepository: AppConfigRepository,
    visibleApps: ObserveVisibleAppsUseCase,
    settingsRepository: SettingsRepository,
    override val iconCache: IconCache,
) : HomeAppsSource {
    private val preferences = combine(
        settingsRepository.drawerMode,
        settingsRepository.iconShape,
        settingsRepository.iconPackPackage,
        settingsRepository.leftSwipeTarget,
        settingsRepository.rightSwipeTarget,
    ) { drawerMode, shape, pack, leftTarget, rightTarget ->
        HomeAppPreferences(drawerMode, shape, pack, leftTarget, rightTarget)
    }

    override val state: Flow<HomeAppsState> = combine(
        visibleApps(),
        appConfigRepository.observeAll(),
        preferences,
    ) { apps, configs, preference ->
        val appsByPackage = apps.associateBy(AppInfo::packageName)
        val pinnedApps = configs.values
            .filter(AppConfig::pinned)
            .sortedBy(AppConfig::pinOrder)
            .mapNotNull { appsByPackage[it.packageName] }
            .take(4)
        fun appFor(target: SwipeAppTarget?): AppInfo? = target?.let {
            apps.firstOrNull { app ->
                app.packageName == it.packageName &&
                    app.className == it.className &&
                    app.userSerial == it.userSerial
            }
        }
        HomeAppsState(
            pinnedApps = pinnedApps,
            configs = configs,
            drawerMode = preference.drawerMode,
            iconShape = preference.iconShape,
            iconPackPackage = preference.iconPackPackage,
            leftSwipeApp = appFor(preference.leftSwipeTarget),
            rightSwipeApp = appFor(preference.rightSwipeTarget),
        )
    }

    private val configRepository = appConfigRepository

    override suspend fun requestLaunch(app: AppInfo): LaunchResult = appLauncher.requestLaunch(app)
    override suspend fun setPinned(packageName: String, pinned: Boolean): AppConfigResult =
        configRepository.setPinned(packageName, pinned)
    override suspend fun hide(packageName: String) = configRepository.setHidden(packageName, true)
    override suspend fun setCategory(
        packageName: String,
        category: com.savatech.chimelauncher.domain.model.AppCategory,
    ) = configRepository.setCategory(packageName, category)
    override suspend fun setDailyLimit(packageName: String, minutes: Int?) =
        configRepository.setDailyLimit(packageName, minutes)
    override suspend fun dailyLimitWarnings(): List<String> = appLauncher.dailyLimitWarnings()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeAppsModule {
    @Binds
    @Singleton
    abstract fun bindHomeAppsSource(source: DefaultHomeAppsSource): HomeAppsSource
}

@Module
@InstallIn(SingletonComponent::class)
object HomeClockModule {
    @Provides
    @Singleton
    fun provideHomeClock(): Clock = Clock.systemDefaultZone()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appsSource: HomeAppsSource,
    private val goalRepository: GoalRepository,
    observeTodayOverview: ObserveTodayOverviewUseCase,
    private val toggleTaskCompletion: ToggleTaskCompletionUseCase,
    private val clock: Clock,
) : ViewModel() {
    private val currentMoment = minuteTicker(clock)
    private val allGoalFlows = GoalStatus.entries.map(goalRepository::observeGoals)
    private val anyGoals: Flow<Boolean> = combine(allGoalFlows) { goals ->
        goals.any { it.isNotEmpty() }
    }
    private val taskUpdateFailed = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dailyGoalState = currentMoment
        .map { it.toLocalDate() }
        .distinctUntilChanged()
        .flatMapLatest { date ->
            combine(
                goalRepository.observeDailyPriorities(date),
                observeTodayOverview(date),
                anyGoals,
            ) { priorities, overviews, hasGoals ->
                val overviewById = overviews.associateBy { it.goal.id }
                val orderedGoals = priorities.sortedBy { it.position }.mapNotNull { priority ->
                    overviewById[priority.goalId]?.let { overview ->
                        val unfinishedTasks = overview.scheduledTasks
                            .filterNot(ScheduledTaskOverview::completed)
                            .map { task -> HomeTaskItem(task, overview.goal.id, overview.goal.title) }
                        HomePriorityGoal(overview, unfinishedTasks)
                    }
                }
                DailyHomeState(orderedGoals, hasGoals)
            }
        }

    val iconCache: IconCache? get() = appsSource.iconCache

    val uiState: StateFlow<HomeUiState> = combine(
        currentMoment,
        dailyGoalState,
        appsSource.state,
        taskUpdateFailed,
    ) { moment, daily, apps, updateFailed ->
        val nextTask = daily.priorityGoals.firstNotNullOfOrNull { it.unfinishedTasks.firstOrNull() }
        HomeUiState(
            currentTime = moment,
            priorityGoals = daily.priorityGoals,
            hasGoals = daily.hasGoals,
            nextTask = nextTask,
            pinnedApps = apps.pinnedApps,
            configs = apps.configs,
            drawerMode = apps.drawerMode,
            iconShape = apps.iconShape,
            iconPackPackage = apps.iconPackPackage,
            leftSwipeApp = apps.leftSwipeApp,
            rightSwipeApp = apps.rightSwipeApp,
            taskUpdateFailed = updateFailed,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(currentTime = ZonedDateTime.now(clock)),
    )

    suspend fun requestLaunch(app: AppInfo): LaunchResult = appsSource.requestLaunch(app)
    suspend fun setPinned(packageName: String, pinned: Boolean): AppConfigResult =
        appsSource.setPinned(packageName, pinned)
    suspend fun hide(packageName: String) = appsSource.hide(packageName)
    suspend fun setCategory(
        packageName: String,
        category: com.savatech.chimelauncher.domain.model.AppCategory,
    ) = appsSource.setCategory(packageName, category)
    suspend fun setDailyLimit(packageName: String, minutes: Int?) =
        appsSource.setDailyLimit(packageName, minutes)
    suspend fun dailyLimitWarnings(): List<String> = appsSource.dailyLimitWarnings()

    fun setTaskCompleted(taskId: String, completed: Boolean) {
        val date = uiState.value.currentTime.toLocalDate()
        viewModelScope.launch {
            taskUpdateFailed.value = false
            try {
                toggleTaskCompletion(taskId, date, completed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                taskUpdateFailed.value = true
            }
        }
    }

    fun dismissTaskUpdateFailure() {
        taskUpdateFailed.update { false }
    }
}

private data class DailyHomeState(
    val priorityGoals: List<HomePriorityGoal>,
    val hasGoals: Boolean,
)

internal fun minuteTicker(clock: Clock): Flow<ZonedDateTime> = flow {
    while (true) {
        emit(ZonedDateTime.now(clock))
        val elapsedInMinute = Math.floorMod(clock.millis(), MILLIS_PER_MINUTE)
        delay(MILLIS_PER_MINUTE - elapsedInMinute)
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
