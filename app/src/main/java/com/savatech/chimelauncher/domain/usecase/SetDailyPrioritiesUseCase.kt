package com.savatech.chimelauncher.domain.usecase

import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.GoalStatus
import java.time.LocalDate
import javax.inject.Inject

sealed interface SetDailyPrioritiesResult {
    data object Success : SetDailyPrioritiesResult
    data object TooManyPriorities : SetDailyPrioritiesResult
    data class DuplicateGoal(val goalId: String) : SetDailyPrioritiesResult
    data class GoalNotFound(val goalId: String) : SetDailyPrioritiesResult
    data class GoalNotActive(val goalId: String) : SetDailyPrioritiesResult
}

class SetDailyPrioritiesUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    suspend operator fun invoke(
        date: LocalDate,
        goalIds: List<String>,
    ): SetDailyPrioritiesResult {
        val duplicateId = goalIds.groupingBy { it }.eachCount()
            .entries.firstOrNull { it.value > 1 }?.key
        if (duplicateId != null) return SetDailyPrioritiesResult.DuplicateGoal(duplicateId)
        if (goalIds.size > MAX_DAILY_PRIORITIES) return SetDailyPrioritiesResult.TooManyPriorities

        for (goalId in goalIds) {
            val goal = goalRepository.getGoal(goalId)
                ?: return SetDailyPrioritiesResult.GoalNotFound(goalId)
            if (goal.status != GoalStatus.ACTIVE) {
                return SetDailyPrioritiesResult.GoalNotActive(goalId)
            }
        }

        goalRepository.setDailyPriorities(date, goalIds)
        return SetDailyPrioritiesResult.Success
    }

    private companion object {
        const val MAX_DAILY_PRIORITIES = 3
    }
}
