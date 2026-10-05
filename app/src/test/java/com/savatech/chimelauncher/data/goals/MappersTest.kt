package com.savatech.chimelauncher.data.goals

import com.savatech.chimelauncher.data.db.entities.DailyPriority
import com.savatech.chimelauncher.data.db.entities.Goal
import com.savatech.chimelauncher.data.db.entities.Task
import com.savatech.chimelauncher.data.db.entities.TaskLog
import com.savatech.chimelauncher.domain.model.DailyPriorityModel
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskLogModel
import com.savatech.chimelauncher.domain.model.TaskModel
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {
    @Test
    fun goalMappingRoundTripsIsoDateAndStatus() {
        val model = GoalModel(
            id = "goal",
            title = "Goal",
            why = "Why",
            category = "PERSONAL",
            targetDate = LocalDate.of(2025, 1, 2),
            unit = "pages",
            targetValue = 10.0,
            status = GoalStatus.PAUSED,
            createdAt = 12,
        )

        assertEquals(model, model.toEntity().toModel())
    }

    @Test
    fun taskMappingRoundTripsRecurrenceAndSchedule() {
        val model = TaskModel(
            id = "task",
            goalId = "goal",
            title = "Task",
            recurrence = Recurrence.DAYS_OF_WEEK,
            daysMask = 21,
            reminderTime = "09:30",
            createdAt = 34,
        )

        assertEquals(model, model.toEntity().toModel())
    }

    @Test
    fun taskLogMappingRoundTripsIsoDate() {
        val model = TaskLogModel(
            id = 3,
            taskId = "task",
            date = LocalDate.of(2025, 1, 2),
            value = 5.0,
            completed = true,
        )

        assertEquals(model, model.toEntity().toModel())
    }

    @Test
    fun dailyPriorityMappingRoundTripsPosition() {
        val model = DailyPriorityModel(
            date = LocalDate.of(2025, 1, 2),
            goalId = "goal",
            position = 2,
        )

        assertEquals(model, model.toEntity().toModel())
    }

    @Test
    fun mappersUseExpectedEntityStorageShapes() {
        assertEquals("2025-01-02", Goal(
            id = "goal",
            title = "Goal",
            why = null,
            category = "PERSONAL",
            targetDate = LocalDate.of(2025, 1, 2).toString(),
            unit = null,
            targetValue = null,
            status = "ACTIVE",
            createdAt = 1,
        ).toModel().toEntity().targetDate)
        assertEquals("DAILY", Task(
            id = "task",
            goalId = "goal",
            title = "Task",
            recurrence = "DAILY",
            daysMask = 0,
            reminderTime = null,
            createdAt = 1,
        ).toModel().toEntity().recurrence)
        assertEquals("2025-01-02", TaskLog(
            id = 1,
            taskId = "task",
            date = "2025-01-02",
            value = null,
            completed = false,
        ).toModel().toEntity().date)
        assertEquals(1, DailyPriority(
            date = "2025-01-02",
            goalId = "goal",
            position = 1,
        ).toModel().toEntity().position)
    }
}
