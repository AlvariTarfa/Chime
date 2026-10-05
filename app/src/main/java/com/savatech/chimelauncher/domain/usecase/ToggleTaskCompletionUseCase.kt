package com.savatech.chimelauncher.domain.usecase

import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.TaskLogModel
import java.time.LocalDate
import javax.inject.Inject

class ToggleTaskCompletionUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
) {
    suspend operator fun invoke(taskId: String, date: LocalDate, completed: Boolean) {
        val existingLog = goalRepository.getTaskLog(taskId, date)
        goalRepository.upsertTaskLog(
            TaskLogModel(
                id = existingLog?.id ?: 0,
                taskId = taskId,
                date = date,
                value = existingLog?.value,
                completed = completed,
            ),
        )
    }
}
