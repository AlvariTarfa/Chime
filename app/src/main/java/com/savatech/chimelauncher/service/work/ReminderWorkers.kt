package com.savatech.chimelauncher.service.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.EntryPointAccessors
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.savatech.chimelauncher.MainActivity
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.domain.checkin.isWithinQuietHours
import com.savatech.chimelauncher.domain.checkin.shouldPostNudge
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskModel
import com.savatech.chimelauncher.service.SchedulerFacade
import com.savatech.chimelauncher.service.notify.NotificationChannels
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderWorkerEntryPoint {
    fun goalRepository(): GoalRepository
    fun settingsRepository(): SettingsRepository
    fun schedulerFacade(): SchedulerFacade
    fun clock(): Clock
}

class CheckInWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val type = inputData.getString(KEY_TYPE) ?: return Result.failure()
        if (type !in setOf(MORNING, EVENING, WEEKLY)) return Result.failure()
        val deps = dependencies()
        try {
            val settings = deps.settingsRepository()
            val enabled = when (type) {
                MORNING -> settings.morningCheckInEnabled.first()
                EVENING -> settings.eveningCheckInEnabled.first()
                else -> settings.weeklyCheckInEnabled.first()
            }
            if (enabled && canPost() && !quietNow(settings, deps.clock())) {
                val title = when (type) {
                    MORNING -> R.string.morning_check_in_title
                    EVENING -> R.string.evening_check_in_title
                    else -> R.string.weekly_review_title
                }
                post(
                    context = applicationContext,
                    channel = NotificationChannels.CHECK_INS,
                    title = applicationContext.getString(title),
                    text = applicationContext.getString(R.string.check_in_notification_text),
                    route = "checkin/$type",
                    notificationId = type.hashCode(),
                    checkInType = type,
                )
            }
        } finally {
            deps.schedulerFacade().scheduleNextCheckIn(type)
        }
        return Result.success()
    }

    companion object {
        const val WORKER_NAME = "check-in"
        const val KEY_TYPE = "check_in_type"
        const val MORNING = "MORNING"
        const val EVENING = "EVENING"
        const val WEEKLY = "WEEKLY"
    }
}

class TaskReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val deps = dependencies()
        try {
            val task = deps.goalRepository().getTask(taskId) ?: return Result.success()
            val goal = deps.goalRepository().getGoal(task.goalId) ?: return Result.success()
            val now = LocalTime.now(deps.clock())
            val settings = deps.settingsRepository()
            if (
                goal.status == GoalStatus.ACTIVE &&
                task.isScheduledOn(LocalDate.now(deps.clock())) &&
                deps.goalRepository().getTaskLog(taskId, LocalDate.now(deps.clock()))?.completed != true &&
                canPost() &&
                !isWithinQuietHours(
                    now,
                    settings.quietHoursStart.first()?.let(LocalTime::parse),
                    settings.quietHoursEnd.first()?.let(LocalTime::parse),
                )
            ) {
                post(
                    context = applicationContext,
                    channel = NotificationChannels.REMINDERS,
                    title = applicationContext.getString(R.string.task_reminder_title),
                    text = task.title,
                    route = "task/${goal.id}/${task.id}",
                    notificationId = taskId.hashCode(),
                    taskId = task.id,
                )
            }
        } finally {
            deps.schedulerFacade().scheduleNextTaskReminder(taskId)
        }
        return Result.success()
    }

    companion object {
        const val WORKER_NAME = "task-reminder"
        const val KEY_TASK_ID = "task_id"
    }
}

class NudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val deps = dependencies()
        try {
            val settings = deps.settingsRepository()
            val enabled = settings.nudgesEnabled.first()
            val now = LocalTime.now(deps.clock())
            if (!enabled || !canPost() || isQuietNow(settings, now)) return Result.success()
            val date = LocalDate.now(deps.clock())
            val priorities = deps.goalRepository().observeDailyPriorities(date).first()
            var topGoal: GoalModel? = null
            for (priority in priorities) {
                val candidate = deps.goalRepository().getGoal(priority.goalId)
                if (candidate?.status == GoalStatus.ACTIVE) {
                    topGoal = candidate
                    break
                }
            }
            val goal = topGoal ?: return Result.success()
            val taskIds = deps.goalRepository().observeTasks(goal.id).first()
            val completed = taskIds.count { task ->
                deps.goalRepository().getTaskLog(task.id, date)?.completed == true
            }
            if (shouldPostNudge(enabled, true, completed)) {
                post(
                    context = applicationContext,
                    channel = NotificationChannels.NUDGES,
                    title = applicationContext.getString(R.string.daily_nudge_title),
                    text = applicationContext.getString(R.string.daily_nudge_text, goal.title),
                    route = "goals/detail/${goal.id}",
                    notificationId = date.toString().hashCode(),
                    goalId = goal.id,
                )
            }
        } finally {
            deps.schedulerFacade().scheduleNextNudge()
        }
        return Result.success()
    }

    companion object {
        const val WORKER_NAME = "daily-nudge"
    }
}

private fun CoroutineWorker.dependencies(): ReminderWorkerEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, ReminderWorkerEntryPoint::class.java)

private fun CoroutineWorker.canPost(): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private suspend fun CoroutineWorker.quietNow(
    settings: SettingsRepository,
    clock: Clock,
): Boolean = isQuietNow(settings, LocalTime.now(clock))

private fun TaskModel.isScheduledOn(date: LocalDate): Boolean =
    recurrence == Recurrence.DAILY || daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0

private suspend fun isQuietNow(settings: SettingsRepository, now: LocalTime): Boolean =
    isWithinQuietHours(
        now,
        settings.quietHoursStart.first()?.let(LocalTime::parse),
        settings.quietHoursEnd.first()?.let(LocalTime::parse),
    )

private fun post(
    context: Context,
    channel: String,
    title: String,
    text: String,
    route: String,
    notificationId: Int,
    checkInType: String? = null,
    taskId: String? = null,
    goalId: String? = null,
) {
    if (
        Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    NotificationChannels.create(context)
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(MainActivity.EXTRA_NOTIFICATION_ROUTE, route)
        checkInType?.let { putExtra(MainActivity.EXTRA_CHECK_IN_TYPE, it) }
        taskId?.let { putExtra(MainActivity.EXTRA_TASK_ID, it) }
        goalId?.let { putExtra(MainActivity.EXTRA_GOAL_ID, it) }
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        notificationId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = NotificationCompat.Builder(context, channel)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(text)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
    NotificationManagerCompat.from(context).notify(notificationId, notification)
}
