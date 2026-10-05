package com.savatech.chimelauncher.domain.focus

import com.savatech.chimelauncher.data.db.entities.TaskLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusSessionRulesTest {
    @Test
    fun remainingTimeIsDerivedFromTheStartAndPlannedDuration() {
        assertEquals(1_500_000L, remainingSessionMillis(1_000L, 25, 1_000L))
        assertEquals(30_000L, remainingSessionMillis(1_000L, 25, 1_471_000L))
        assertEquals(0L, remainingSessionMillis(1_000L, 25, 1_501_000L))
    }

    @Test
    fun linkedTaskIsMarkedCompleteAndMinutesAreAddedToItsExistingValue() {
        val result = creditSessionToTaskLog(
            TaskLog(id = 9, taskId = "task", date = "2026-10-05", value = 10.0, completed = false),
            "task",
            "2026-10-05",
            "minutes",
            25,
        )
        assertEquals(TaskLog(9, "task", "2026-10-05", 35.0, true), result)
    }

    @Test
    fun hoursAreCreditedAsFractionalHoursAndNonTimeUnitsKeepTheirValue() {
        assertEquals(
            0.5,
            creditSessionToTaskLog(null, "task", "2026-10-05", "hours", 30)?.value!!,
            0.0,
        )
        assertEquals(
            4.0,
            creditSessionToTaskLog(
                TaskLog(3, "task", "2026-10-05", 4.0, false),
                "task",
                "2026-10-05",
                "pages",
                30,
            )?.value!!,
            0.0,
        )
    }

    @Test
    fun unlinkedSessionDoesNotCreateAGoalLog() {
        assertNull(creditSessionToTaskLog(null, null, "2026-10-05", "minutes", 25))
    }
}
