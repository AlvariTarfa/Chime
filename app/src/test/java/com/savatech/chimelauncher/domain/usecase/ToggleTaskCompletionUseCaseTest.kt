package com.savatech.chimelauncher.domain.usecase

import com.savatech.chimelauncher.domain.model.TaskLogModel
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ToggleTaskCompletionUseCaseTest {
    @Test
    fun repeatedDesiredStateUpsertsSingleLogAndPreservesValue() = runBlocking {
        val repository = FakeGoalRepository()
        val date = LocalDate.of(2024, 6, 1)
        repository.logs["task" to date] = TaskLogModel(
            id = 12,
            taskId = "task",
            date = date,
            value = 4.5,
            completed = false,
        )
        val useCase = ToggleTaskCompletionUseCase(repository)

        useCase("task", date, completed = true)
        useCase("task", date, completed = true)

        assertEquals(1, repository.logs.size)
        assertEquals(12L, repository.logs.getValue("task" to date).id)
        assertEquals(4.5, repository.logs.getValue("task" to date).value)
        assertEquals(true, repository.logs.getValue("task" to date).completed)
    }
}
