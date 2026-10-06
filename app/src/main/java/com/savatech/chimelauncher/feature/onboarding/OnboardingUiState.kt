package com.savatech.chimelauncher.feature.onboarding

import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.settings.FrictionLevel
import com.savatech.chimelauncher.domain.model.AppCategory

data class GoalDraft(val title: String = "", val task: String = "")

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME_GOALS,
    val isLoading: Boolean = true,
    val onboardingCompleted: Boolean? = null,
    val isDefaultHome: Boolean = false,
    val usageGranted: Boolean = false,
    val notificationGranted: Boolean = false,
    val hasExistingGoals: Boolean = false,
    val existingGoalCount: Int = 0,
    val hasExistingClassifications: Boolean = false,
    val classifiedCount: Int = 0,
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val classifications: Map<String, AppCategory> = emptyMap(),
    val goals: List<GoalDraft> = listOf(GoalDraft()),
    val frictionLevel: FrictionLevel = FrictionLevel.BALANCED,
    val frictionConfigured: Boolean = false,
    val isSaving: Boolean = false,
    val failure: OnboardingFailure? = null,
)

enum class OnboardingFailure {
    LOAD_FAILED,
    GOALS_INVALID,
    SAVE_FAILED,
}
