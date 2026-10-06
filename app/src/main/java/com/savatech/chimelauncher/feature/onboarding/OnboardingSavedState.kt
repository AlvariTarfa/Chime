package com.savatech.chimelauncher.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import com.savatech.chimelauncher.data.settings.FrictionLevel
import com.savatech.chimelauncher.domain.model.AppCategory

internal class OnboardingSavedState(private val handle: SavedStateHandle) {
    fun step(): OnboardingStep =
        handle.get<String>(KEY_STEP)
            ?.let { saved -> OnboardingStep.entries.firstOrNull { it.name == saved } }
            ?: OnboardingStep.WELCOME_GOALS

    fun searchQuery(): String = handle.get<String>(KEY_SEARCH).orEmpty()
    fun frictionLevel(): FrictionLevel? =
        handle.get<String>(KEY_FRICTION)?.let(FrictionLevel::fromStorage)
    fun hasFrictionChoice(): Boolean = handle.contains(KEY_FRICTION)

    fun goalDrafts(): List<GoalDraft> {
        val titles = handle.get<ArrayList<String>>(KEY_GOAL_TITLES).orEmpty()
        val tasks = handle.get<ArrayList<String>>(KEY_GOAL_TASKS).orEmpty()
        return if (titles.isEmpty()) listOf(GoalDraft()) else titles.take(MAX_GOALS).mapIndexed { index, title ->
            GoalDraft(title, tasks.getOrNull(index).orEmpty())
        }
    }

    fun classifications(): Map<String, AppCategory> {
        val packages = handle.get<ArrayList<String>>(KEY_CLASSIFICATION_PACKAGES).orEmpty()
        val values = handle.get<ArrayList<String>>(KEY_CLASSIFICATION_VALUES).orEmpty()
        return packages.mapIndexedNotNull { index, packageName ->
            values.getOrNull(index)?.let { packageName to AppCategory.fromStorage(it) }
        }.toMap()
    }

    fun saveStep(step: OnboardingStep) {
        handle[KEY_STEP] = step.name
    }

    fun saveSearchQuery(query: String) {
        handle[KEY_SEARCH] = query
    }

    fun saveFrictionLevel(level: FrictionLevel) {
        handle[KEY_FRICTION] = level.name
    }

    fun saveGoalDrafts(drafts: List<GoalDraft>) {
        handle[KEY_GOAL_TITLES] = ArrayList(drafts.map(GoalDraft::title))
        handle[KEY_GOAL_TASKS] = ArrayList(drafts.map(GoalDraft::task))
    }

    fun saveClassifications(categories: Map<String, AppCategory>) {
        handle[KEY_CLASSIFICATION_PACKAGES] = ArrayList(categories.keys)
        handle[KEY_CLASSIFICATION_VALUES] = ArrayList(categories.values.map(AppCategory::name))
    }

    private companion object {
        const val KEY_STEP = "onboarding_step"
        const val KEY_SEARCH = "onboarding_search"
        const val KEY_FRICTION = "onboarding_friction"
        const val KEY_GOAL_TITLES = "onboarding_goal_titles"
        const val KEY_GOAL_TASKS = "onboarding_goal_tasks"
        const val KEY_CLASSIFICATION_PACKAGES = "onboarding_classification_packages"
        const val KEY_CLASSIFICATION_VALUES = "onboarding_classification_values"
        const val MAX_GOALS = 3
    }
}
