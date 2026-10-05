package com.savatech.chimelauncher.domain.usecase

import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SetDailyPrioritiesUseCaseTest {
    private val date = LocalDate.of(2024, 6, 1)

    @Test
    fun fourthPriorityIsRejected() = runBlocking {
        val repository = FakeGoalRepository()
        listOf("one", "two", "three", "four").forEach { repository.goals[it] = goal(it) }

        val result = SetDailyPrioritiesUseCase(repository)(
            date,
            listOf("one", "two", "three", "four"),
        )

        assertEquals(SetDailyPrioritiesResult.TooManyPriorities, result)
        assertEquals(emptyList<String>(), repository.priorityGoalIds)
    }

    @Test
    fun pausedGoalIsRejected() = runBlocking {
        val repository = FakeGoalRepository()
        repository.goals["paused"] = goal("paused", GoalStatus.PAUSED)

        val result = SetDailyPrioritiesUseCase(repository)(date, listOf("paused"))

        assertEquals(SetDailyPrioritiesResult.GoalNotActive("paused"), result)
        assertEquals(emptyList<String>(), repository.priorityGoalIds)
    }

    @Test
    fun duplicateGoalIsRejected() = runBlocking {
        val repository = FakeGoalRepository()
        repository.goals["active"] = goal("active")

        val result = SetDailyPrioritiesUseCase(repository)(date, listOf("active", "active"))

        assertEquals(SetDailyPrioritiesResult.DuplicateGoal("active"), result)
        assertEquals(emptyList<String>(), repository.priorityGoalIds)
    }

    @Test
    fun activeUniquePrioritiesArePersistedInOrder() = runBlocking {
        val repository = FakeGoalRepository()
        repository.goals["first"] = goal("first")
        repository.goals["second"] = goal("second")

        val result = SetDailyPrioritiesUseCase(repository)(date, listOf("second", "first"))

        assertEquals(SetDailyPrioritiesResult.Success, result)
        assertEquals(date, repository.priorityDate)
        assertEquals(listOf("second", "first"), repository.priorityGoalIds)
    }

    private fun goal(id: String, status: GoalStatus = GoalStatus.ACTIVE) = GoalModel(
        id = id,
        title = id,
        why = null,
        category = "PERSONAL",
        targetDate = null,
        unit = null,
        targetValue = null,
        status = status,
        createdAt = 0,
    )
}
