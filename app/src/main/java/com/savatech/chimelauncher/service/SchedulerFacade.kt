package com.savatech.chimelauncher.service

import com.savatech.chimelauncher.core.di.IoDispatcher
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.domain.checkin.nextDailyOccurrence
import com.savatech.chimelauncher.domain.checkin.nextWeeklyOccurrence
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.service.work.CheckInWorker
import com.savatech.chimelauncher.service.work.NudgeWorker
import com.savatech.chimelauncher.service.work.TaskReminderWorker
import com.savatech.chimelauncher.service.work.WorkRequestScheduler
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class SchedulerFacade @Inject constructor(
    private val settings: SettingsRepository,
    private val goals: GoalRepository,
    private val scheduler: WorkRequestScheduler,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SchedulerRescheduler {
    override suspend fun rescheduleAll() = withContext(ioDispatcher) {
        val now = LocalDateTime.now(clock)
        scheduleCheckIn(
            CheckInWorker.MORNING,
            settings.morningCheckInEnabled.first(),
            settings.morningCheckInTime.first(),
            now,
        )
        scheduleCheckIn(
            CheckInWorker.EVENING,
            settings.eveningCheckInEnabled.first(),
            settings.eveningCheckInTime.first(),
            now,
        )
        scheduleCheckIn(
            CheckInWorker.WEEKLY,
            settings.weeklyCheckInEnabled.first(),
            settings.weeklyCheckInTime.first(),
            now,
            settings.weeklyCheckInDay.first(),
        )
        scheduleNextNudge()
        scheduler.cancelTaskReminders()
        val activeGoalIds = goals.observeGoals(GoalStatus.ACTIVE).first().map { it.id }
        activeGoalIds.flatMap { goals.observeTasks(it).first() }
            .filter { it.reminderTime != null }
            .forEach { task ->
                val occurrence = nextDailyOccurrence(now, LocalTime.parse(task.reminderTime))
                scheduler.enqueueUnique(
                    taskReminderWorkName(task.id),
                    TaskReminderWorker.WORKER_NAME,
                    occurrenceClockDelay(now, occurrence),
                    mapOf(TaskReminderWorker.KEY_TASK_ID to task.id),
                )
            }
    }

    suspend fun scheduleNextCheckIn(type: String) = withContext(ioDispatcher) {
        val now = LocalDateTime.now(clock)
        val weekly = type == CheckInWorker.WEEKLY
        val enabled = when (type) {
            CheckInWorker.MORNING -> settings.morningCheckInEnabled.first()
            CheckInWorker.EVENING -> settings.eveningCheckInEnabled.first()
            CheckInWorker.WEEKLY -> settings.weeklyCheckInEnabled.first()
            else -> error("Unknown check-in type: $type")
        }
        val time = when (type) {
            CheckInWorker.MORNING -> settings.morningCheckInTime.first()
            CheckInWorker.EVENING -> settings.eveningCheckInTime.first()
            else -> settings.weeklyCheckInTime.first()
        }
        scheduleCheckIn(type, enabled, time, now, if (weekly) settings.weeklyCheckInDay.first() else null)
    }

    suspend fun scheduleNextTaskReminder(taskId: String) = withContext(ioDispatcher) {
        val task = goals.getTask(taskId)
        val goal = task?.let { goals.getGoal(it.goalId) }
        val time = task?.reminderTime?.let(LocalTime::parse)
        val name = taskReminderWorkName(taskId)
        if (task == null || goal?.status != GoalStatus.ACTIVE || time == null) {
            scheduler.cancelUnique(name)
        } else {
            val now = LocalDateTime.now(clock)
            val occurrence = nextDailyOccurrence(now, time)
            scheduler.enqueueUnique(
                name,
                TaskReminderWorker.WORKER_NAME,
                occurrenceClockDelay(now, occurrence),
                mapOf(TaskReminderWorker.KEY_TASK_ID to taskId),
            )
        }
    }

    suspend fun scheduleNextNudge() = withContext(ioDispatcher) {
        if (!settings.nudgesEnabled.first()) {
            scheduler.cancelUnique(NUDGE_WORK_NAME)
        } else {
            val now = LocalDateTime.now(clock)
            val occurrence = nextDailyOccurrence(now, LocalTime.of(14, 0))
            scheduler.enqueueUnique(
                NUDGE_WORK_NAME,
                NudgeWorker.WORKER_NAME,
                occurrenceClockDelay(now, occurrence),
                emptyMap(),
            )
        }
    }

    private fun scheduleCheckIn(
        type: String,
        enabled: Boolean,
        timeValue: String,
        now: LocalDateTime,
        dayValue: String? = null,
    ) {
        val name = checkInWorkName(type)
        if (!enabled) {
            scheduler.cancelUnique(name)
            return
        }
        val time = LocalTime.parse(timeValue)
        val occurrence = if (dayValue == null) {
            nextDailyOccurrence(now, time)
        } else {
            nextWeeklyOccurrence(now, DayOfWeek.valueOf(dayValue), time)
        }
        scheduler.enqueueUnique(
            name,
            CheckInWorker.WORKER_NAME,
            occurrenceClockDelay(now, occurrence),
            mapOf(CheckInWorker.KEY_TYPE to type),
        )
    }

    private fun occurrenceClockDelay(now: LocalDateTime, occurrence: LocalDateTime): Long =
        java.time.Duration.between(
            now.atZone(java.time.ZoneId.systemDefault()).toInstant(),
            occurrence.atZone(java.time.ZoneId.systemDefault()).toInstant(),
        ).toMillis().coerceAtLeast(0)

    companion object {
        const val NUDGE_WORK_NAME = "daily-nudge"
        fun checkInWorkName(type: String) = "check-in-$type"
        fun taskReminderWorkName(taskId: String) = "task-reminder-$taskId"
    }
}
