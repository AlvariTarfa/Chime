package com.savatech.chimelauncher.data.backup

import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.db.entities.CheckIn
import com.savatech.chimelauncher.data.db.entities.DailyPriority
import com.savatech.chimelauncher.data.db.entities.FocusMode
import com.savatech.chimelauncher.data.db.entities.FocusSession
import com.savatech.chimelauncher.data.db.entities.Goal
import com.savatech.chimelauncher.data.db.entities.InterceptEvent
import com.savatech.chimelauncher.data.db.entities.Task
import com.savatech.chimelauncher.data.db.entities.TaskLog
import com.savatech.chimelauncher.domain.backup.BackupAppConfig
import com.savatech.chimelauncher.domain.backup.BackupCheckIn
import com.savatech.chimelauncher.domain.backup.BackupDailyPriority
import com.savatech.chimelauncher.domain.backup.BackupFocusMode
import com.savatech.chimelauncher.domain.backup.BackupFocusSession
import com.savatech.chimelauncher.domain.backup.BackupGoal
import com.savatech.chimelauncher.domain.backup.BackupInterceptEvent
import com.savatech.chimelauncher.domain.backup.BackupModel
import com.savatech.chimelauncher.domain.backup.BackupTask
import com.savatech.chimelauncher.domain.backup.BackupTaskLog

internal fun Goal.toBackup() = BackupGoal(
    id, title, why, category, targetDate, unit, targetValue, status, createdAt,
)

internal fun BackupGoal.toEntity() = Goal(
    id, title, why, category, targetDate, unit, targetValue, status, createdAt,
)

internal fun Task.toBackup() = BackupTask(
    id, goalId, title, recurrence, daysMask, reminderTime, createdAt,
)

internal fun BackupTask.toEntity() = Task(
    id, goalId, title, recurrence, daysMask, reminderTime, createdAt,
)

internal fun TaskLog.toBackup() = BackupTaskLog(id, taskId, date, value, completed)
internal fun BackupTaskLog.toEntity() = TaskLog(id, taskId, date, value, completed)
internal fun DailyPriority.toBackup() = BackupDailyPriority(date, goalId, position)
internal fun BackupDailyPriority.toEntity() = DailyPriority(date, goalId, position)

internal fun AppConfig.toBackup() = BackupAppConfig(
    packageName, category, pinned, pinOrder, hidden, dailyLimitMin, linkedGoalId,
)

internal fun BackupAppConfig.toEntity() = AppConfig(
    packageName, category, pinned, pinOrder, hidden, dailyLimitMin, linkedGoalId,
)

internal fun FocusMode.toBackup() = BackupFocusMode(
    id, name, allowedPackages, scheduleJson, suppressNotifications, isBuiltIn,
)

internal fun BackupFocusMode.toEntity() = FocusMode(
    id, name, allowedPackages, scheduleJson, suppressNotifications, isBuiltIn,
)

internal fun FocusSession.toBackup() = BackupFocusSession(
    id, goalId, taskId, startedAt, plannedMinutes, endedAt, completed,
)

internal fun BackupFocusSession.toEntity() = FocusSession(
    id, goalId, taskId, startedAt, plannedMinutes, endedAt, completed,
)

internal fun CheckIn.toBackup() =
    BackupCheckIn(id, type, date, mood, notes, payloadJson)

internal fun BackupCheckIn.toEntity() =
    CheckIn(id, type, date, mood, notes, payloadJson)

internal fun InterceptEvent.toBackup() = BackupInterceptEvent(
    id, packageName, timestamp, outcome, grantedMinutes, reason,
)

internal fun BackupInterceptEvent.toEntity() = InterceptEvent(
    id, packageName, timestamp, outcome, grantedMinutes, reason,
)

internal fun BackupModel.toEntityLists() = BackupEntityLists(
    goals.map(BackupGoal::toEntity),
    tasks.map(BackupTask::toEntity),
    taskLogs.map(BackupTaskLog::toEntity),
    dailyPriorities.map(BackupDailyPriority::toEntity),
    appConfigs.map(BackupAppConfig::toEntity),
    focusModes.map(BackupFocusMode::toEntity),
    focusSessions.map(BackupFocusSession::toEntity),
    checkIns.map(BackupCheckIn::toEntity),
    interceptEvents.map(BackupInterceptEvent::toEntity),
)

internal data class BackupEntityLists(
    val goals: List<Goal>,
    val tasks: List<Task>,
    val taskLogs: List<TaskLog>,
    val dailyPriorities: List<DailyPriority>,
    val appConfigs: List<AppConfig>,
    val focusModes: List<FocusMode>,
    val focusSessions: List<FocusSession>,
    val checkIns: List<CheckIn>,
    val interceptEvents: List<InterceptEvent>,
)
