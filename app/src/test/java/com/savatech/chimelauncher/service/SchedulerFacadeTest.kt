package com.savatech.chimelauncher.service

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.domain.usecase.FakeGoalRepository
import com.savatech.chimelauncher.service.work.CheckInWorker
import com.savatech.chimelauncher.service.work.NudgeWorker
import com.savatech.chimelauncher.service.work.TaskReminderWorker
import com.savatech.chimelauncher.service.work.WorkRequestScheduler
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulerFacadeTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun schedulesEnabledCheckInsNudgesAndActiveTaskRemindersAndCancelsDisabledWork() = runTest {
        val settings = settingsRepository(temporaryFolder.newFile("scheduler.preferences_pb"), backgroundScope)
        settings.setMorningCheckInEnabled(true)
        settings.setWeeklyCheckInEnabled(true)
        settings.setNudgesEnabled(true)
        val goals = FakeGoalRepository()
        val goal = GoalModel(
            id = "goal",
            title = "Read",
            why = null,
            category = "Learning",
            targetDate = null,
            unit = null,
            targetValue = null,
            status = GoalStatus.ACTIVE,
            createdAt = 0L,
        )
        goals.goals[goal.id] = goal
        goals.addTask(
            TaskModel(
                id = "task",
                goalId = goal.id,
                title = "Read a page",
                recurrence = Recurrence.DAILY,
                daysMask = 0,
                reminderTime = "08:15",
                createdAt = 0L,
            ),
        )
        val fakeWorkManager = FakeWorkRequestScheduler()
        val zone = ZoneId.systemDefault()
        val clock = Clock.fixed(
            LocalDateTime.parse("2026-04-06T07:00").atZone(zone).toInstant(),
            zone,
        )
        val facade = SchedulerFacade(settings, goals, fakeWorkManager, clock, Dispatchers.Unconfined)

        facade.rescheduleAll()

        assertEquals(CheckInWorker.WORKER_NAME, fakeWorkManager.scheduled["check-in-MORNING"]?.workerName)
        assertEquals(CheckInWorker.WORKER_NAME, fakeWorkManager.scheduled["check-in-WEEKLY"]?.workerName)
        assertEquals(NudgeWorker.WORKER_NAME, fakeWorkManager.scheduled[SchedulerFacade.NUDGE_WORK_NAME]?.workerName)
        assertEquals(
            TaskReminderWorker.WORKER_NAME,
            fakeWorkManager.scheduled[SchedulerFacade.taskReminderWorkName("task")]?.workerName,
        )
        assertTrue(fakeWorkManager.cancelled.contains("check-in-EVENING"))
        assertEquals(30 * 60 * 1000L, fakeWorkManager.scheduled["check-in-MORNING"]?.delayMillis)
        assertTrue(fakeWorkManager.cancelledTaskReminders)

        settings.setMorningCheckInEnabled(false)
        facade.rescheduleAll()
        assertFalse("Disabled daily work must be cancelled.", "check-in-MORNING" in fakeWorkManager.scheduled)
        assertTrue(fakeWorkManager.cancelled.contains("check-in-MORNING"))
    }

    private fun settingsRepository(file: File, scope: CoroutineScope): SettingsRepository =
        SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
        )

    private class FakeWorkRequestScheduler : WorkRequestScheduler {
        data class Scheduled(
            val workerName: String,
            val delayMillis: Long,
            val input: Map<String, String>,
        )

        val scheduled = mutableMapOf<String, Scheduled>()
        val cancelled = mutableSetOf<String>()
        var cancelledTaskReminders = false

        override fun enqueueUnique(
            name: String,
            workerName: String,
            delayMillis: Long,
            input: Map<String, String>,
        ) {
            scheduled[name] = Scheduled(workerName, delayMillis, input)
            cancelled.remove(name)
        }

        override fun cancelUnique(name: String) {
            scheduled.remove(name)
            cancelled += name
        }

        override fun cancelTaskReminders() {
            cancelledTaskReminders = true
            scheduled.keys.removeAll { it.startsWith("task-reminder-") }
        }
    }
}
