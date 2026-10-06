package com.savatech.chimelauncher.feature.onboarding

import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.domain.usecase.SetDailyPrioritiesResult
import com.savatech.chimelauncher.domain.usecase.SetDailyPrioritiesUseCase
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

internal class OnboardingGoalSetup(
    private val goalRepository: GoalRepository,
    private val setDailyPriorities: SetDailyPrioritiesUseCase,
    private val clock: Clock,
) {
    suspend fun createGoalsAndPrioritize(drafts: List<GoalDraft>, category: String): Int {
        val today = LocalDate.now(clock)
        val goalIds = drafts.map { draft ->
            val goalId = UUID.randomUUID().toString()
            goalRepository.createGoal(
                GoalModel(
                    id = goalId,
                    title = draft.title.trim(),
                    why = null,
                    category = category,
                    targetDate = null,
                    unit = null,
                    targetValue = null,
                    status = GoalStatus.ACTIVE,
                    createdAt = clock.millis(),
                ),
            )
            goalRepository.addTask(
                TaskModel(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    title = draft.task.trim(),
                    recurrence = Recurrence.DAILY,
                    daysMask = 0,
                    reminderTime = null,
                    createdAt = clock.millis(),
                ),
            )
            goalId
        }
        if (setDailyPriorities(today, goalIds) != SetDailyPrioritiesResult.Success) {
            error("Could not set today's goal priorities.")
        }
        return goalIds.size
    }
}
