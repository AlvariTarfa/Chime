package com.savatech.chimelauncher.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.apps.AppClassificationStore
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.data.settings.FrictionLevel
import com.savatech.chimelauncher.data.settings.OnboardingSettings
import com.savatech.chimelauncher.data.usage.OnboardingUsageAccess
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.usecase.SetDailyPrioritiesUseCase
import com.savatech.chimelauncher.core.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settings: OnboardingSettings,
    private val appRepository: AppRepository,
    private val classificationStore: AppClassificationStore,
    private val goalRepository: GoalRepository,
    private val setDailyPriorities: SetDailyPrioritiesUseCase,
    private val usageAccess: OnboardingUsageAccess,
    private val defaultHomeStatus: DefaultHomeStatus,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val savedState = OnboardingSavedState(savedStateHandle)
    private val goalSetup = OnboardingGoalSetup(goalRepository, setDailyPriorities, clock)
    private val _uiState = MutableStateFlow(
        OnboardingUiState(
            step = savedState.step(),
            goals = savedState.goalDrafts(),
            searchQuery = savedState.searchQuery(),
            classifications = savedState.classifications(),
            frictionLevel = savedState.frictionLevel() ?: FrictionLevel.BALANCED,
        ),
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.onboardingCompleted.collect { completed ->
                _uiState.update { it.copy(onboardingCompleted = completed) }
            }
        }
        viewModelScope.launch {
            usageAccess.hasPermission.collect { granted ->
                _uiState.update { it.copy(usageGranted = granted) }
            }
        }
        viewModelScope.launch {
            try {
                usageAccess.refresh()
                val activeGoals = goalRepository.observeGoals(GoalStatus.ACTIVE).first()
                val savedConfigs = withContext(ioDispatcher) { classificationStore.getAllConfigs() }
                appRepository.isLoaded.first { it }
                val apps = appRepository.apps.value.distinctBy(AppInfo::packageName)
                val installedPackages = apps.map(AppInfo::packageName)
                val hasSavedCategories = savedConfigs.keys.any(installedPackages::contains)
                val initialCategories = if (hasSavedCategories) {
                    suggestedAppCategories(installedPackages).toMutableMap().apply {
                        savedConfigs.forEach { (packageName, config) ->
                            if (packageName in installedPackages) {
                                this[packageName] = AppCategory.fromStorage(config.category)
                            }
                        }
                    }
                } else {
                    suggestedAppCategories(installedPackages)
                }
                val friction = settings.frictionLevel.first()
                val frictionConfigured = settings.frictionConfigured.first()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasExistingGoals = activeGoals.isNotEmpty(),
                        existingGoalCount = activeGoals.size,
                        hasExistingClassifications = hasSavedCategories,
                        classifiedCount = savedConfigs.keys.count(installedPackages::contains),
                        apps = apps,
                        classifications = if (it.classifications.isEmpty()) {
                            initialCategories
                        } else {
                            it.classifications.filterKeys(installedPackages::contains)
                        },
                        frictionLevel = if (savedState.hasFrictionChoice()) {
                            it.frictionLevel
                        } else {
                            friction
                        },
                        frictionConfigured = frictionConfigured,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, failure = OnboardingFailure.LOAD_FAILED) }
            }
        }
        refreshDefaultHomeStatus()
    }

    fun next() = moveTo(_uiState.value.step.next())
    fun back() = moveTo(_uiState.value.step.previous())
    fun skip() {
        if (_uiState.value.step !in setOf(
                OnboardingStep.WELCOME_GOALS,
                OnboardingStep.WELCOME_FRICTION,
                OnboardingStep.DONE,
            )
        ) next()
    }

    fun refreshDefaultHomeStatus() {
        viewModelScope.launch {
            try {
                val isDefault = defaultHomeStatus.isDefaultHome()
                _uiState.update { it.copy(isDefaultHome = isDefault) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(failure = OnboardingFailure.LOAD_FAILED) }
            }
        }
    }

    fun refreshUsageAccess() = usageAccess.refresh()
    fun usageSettingsIntent() = usageAccess.settingsIntent()

    fun reportFailure() {
        _uiState.update { it.copy(failure = OnboardingFailure.LOAD_FAILED) }
    }

    fun setNotificationGranted(granted: Boolean) =
        _uiState.update { it.copy(notificationGranted = granted) }

    fun updateSearchQuery(query: String) {
        savedState.saveSearchQuery(query)
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setCategory(packageName: String, category: AppCategory) {
        _uiState.update { it.copy(classifications = it.classifications + (packageName to category)) }
        saveClassificationsToHandle(_uiState.value.classifications)
    }

    fun markAllNeutral() {
        _uiState.update {
            it.copy(classifications = it.apps.associate { app ->
                app.packageName to AppCategory.NEUTRAL
            })
        }
        saveClassificationsToHandle(_uiState.value.classifications)
    }

    fun updateGoalTitle(index: Int, title: String) = updateGoal(index) { it.copy(title = title) }
    fun updateGoalTask(index: Int, task: String) = updateGoal(index) { it.copy(task = task) }

    fun addGoal() {
        _uiState.update {
            if (it.goals.size >= MAX_GOALS) it
            else it.copy(goals = it.goals + GoalDraft())
        }
        savedState.saveGoalDrafts(_uiState.value.goals)
    }

    fun removeGoal(index: Int) {
        _uiState.update { it.copy(goals = it.goals.filterIndexed { position, _ -> position != index }) }
        savedState.saveGoalDrafts(_uiState.value.goals)
    }

    fun setFrictionLevel(level: FrictionLevel) {
        savedState.saveFrictionLevel(level)
        _uiState.update { it.copy(frictionLevel = level) }
    }

    fun saveFrictionLevel(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val level = _uiState.value.frictionLevel
                settings.setFrictionLevel(level)
                settings.setBaseDelaySeconds(level.defaultBaseDelaySeconds)
                _uiState.update { it.copy(frictionConfigured = true) }
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(failure = OnboardingFailure.SAVE_FAILED) }
            }
        }
    }

    fun saveClassifications(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, failure = null) }
            try {
                val categories = classificationBatch(
                    _uiState.value.apps.map(AppInfo::packageName),
                    _uiState.value.classifications,
                )
                withContext(ioDispatcher) { classificationStore.setCategories(categories) }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasExistingClassifications = categories.isNotEmpty(),
                        classifiedCount = categories.size,
                    )
                }
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, failure = OnboardingFailure.SAVE_FAILED)
                }
            }
        }
    }

    fun saveGoals(category: String, onSaved: () -> Unit = {}) {
        val drafts = _uiState.value.goals
        if (drafts.isEmpty() || drafts.size > MAX_GOALS ||
            drafts.any { it.title.isBlank() || it.title.length > MAX_TITLE_LENGTH ||
                it.task.isBlank() || it.task.length > MAX_TITLE_LENGTH }
        ) {
            _uiState.update { it.copy(failure = OnboardingFailure.GOALS_INVALID) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, failure = null) }
            try {
                val createdCount = goalSetup.createGoalsAndPrioritize(drafts, category)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        hasExistingGoals = true,
                        existingGoalCount = it.existingGoalCount + createdCount,
                    )
                }
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, failure = OnboardingFailure.SAVE_FAILED) }
            }
        }
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, failure = null) }
            try {
                settings.setOnboardingCompleted(true)
                _uiState.update {
                    it.copy(isSaving = false, onboardingCompleted = true)
                }
                onComplete()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false, failure = OnboardingFailure.SAVE_FAILED) }
            }
        }
    }

    private fun moveTo(step: OnboardingStep?) {
        if (step == null) return
        savedState.saveStep(step)
        _uiState.update { it.copy(step = step, failure = null) }
    }

    private fun updateGoal(index: Int, transform: (GoalDraft) -> GoalDraft) {
        _uiState.update { state ->
            state.copy(goals = state.goals.mapIndexed { position, draft ->
                if (position == index) transform(draft) else draft
            })
        }
        savedState.saveGoalDrafts(_uiState.value.goals)
    }

    private fun saveClassificationsToHandle(categories: Map<String, AppCategory>) {
        savedState.saveClassifications(categories)
    }

    private companion object {
        const val MAX_GOALS = 3
        const val MAX_TITLE_LENGTH = 80
    }
}
