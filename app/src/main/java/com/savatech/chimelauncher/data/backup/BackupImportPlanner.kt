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
import com.savatech.chimelauncher.domain.backup.BackupImportMode
import com.savatech.chimelauncher.domain.backup.BackupImportIssue
import com.savatech.chimelauncher.domain.backup.BackupIssueReason
import com.savatech.chimelauncher.domain.backup.ImportReport
import com.savatech.chimelauncher.domain.backup.BackupRecordType

internal data class BackupImportPlan(
    val insertGoals: List<Goal>,
    val updateGoals: List<Goal>,
    val insertTasks: List<Task>,
    val updateTasks: List<Task>,
    val taskLogs: List<TaskLog>,
    val dailyPriorities: List<DailyPriority>,
    val appConfigs: List<AppConfig>,
    val focusModes: List<FocusMode>,
    val focusSessions: List<FocusSession>,
    val checkIns: List<CheckIn>,
    val interceptEvents: List<InterceptEvent>,
    val report: ImportReport,
)

internal fun planImport(
    incoming: BackupEntityLists,
    current: BackupEntityLists,
    mode: BackupImportMode,
): BackupImportPlan {
    var skipped = 0
    val errors = mutableListOf<BackupImportIssue>()

    fun invalid(kind: BackupRecordType, id: String, reason: BackupIssueReason) {
        skipped++
        errors += BackupImportIssue(kind, id, reason)
    }

    val baseline = if (mode == BackupImportMode.MERGE) current else BackupEntityLists(
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
        emptyList(), emptyList(), emptyList(), emptyList(),
    )

    val goals = resolveRows(
        incoming.goals.filter { row ->
            if (row.id.isBlank()) {
                invalid(BackupRecordType.GOAL, "(blank)", BackupIssueReason.EMPTY_ID)
                false
            } else true
        },
        baseline.goals,
        recordType = BackupRecordType.GOAL,
        identity = Goal::id,
        matches = { old, new -> old.id == new.id },
        timestamp = Goal::createdAt,
    ).also { skipped += it.skipped; errors += it.errors }
    val effectiveGoals = (baseline.goals + goals.inserts + goals.updates).associateBy(Goal::id)

    val tasks = resolveRows(
        incoming.tasks.filter { row ->
            if (row.id.isBlank()) {
                invalid(BackupRecordType.TASK, "(blank)", BackupIssueReason.EMPTY_ID)
                false
            } else if (row.goalId !in effectiveGoals) {
                invalid(BackupRecordType.TASK, row.id, BackupIssueReason.MISSING_GOAL)
                false
            } else true
        },
        baseline.tasks,
        recordType = BackupRecordType.TASK,
        identity = Task::id,
        matches = { old, new -> old.id == new.id },
        timestamp = Task::createdAt,
    ).also { skipped += it.skipped; errors += it.errors }
    val effectiveTasks = (baseline.tasks + tasks.inserts + tasks.updates).associateBy(Task::id)

    val taskLogs = resolveRows(
        incoming.taskLogs.filter { row ->
            if (row.taskId !in effectiveTasks) {
                invalid(
                    BackupRecordType.TASK_LOG,
                    "${row.taskId}/${row.date}",
                    BackupIssueReason.MISSING_TASK,
                )
                false
            } else if (
                baseline.taskLogs.count {
                    it.id == row.id || (it.taskId == row.taskId && it.date == row.date)
                } > 1
            ) {
                invalid(
                    BackupRecordType.TASK_LOG,
                    "${row.taskId}/${row.date}",
                    BackupIssueReason.DUPLICATE_ROW,
                )
                false
            } else true
        },
        baseline.taskLogs,
        recordType = BackupRecordType.TASK_LOG,
        identity = { "${it.taskId}\u0000${it.date}" },
        identifier = { "${it.taskId}/${it.date}" },
        matches = { old, new ->
            old.id == new.id || (old.taskId == new.taskId && old.date == new.date)
        },
        timestamp = { null },
        preserveId = { old, new ->
            if (old.taskId == new.taskId && old.date == new.date) new.copy(id = old.id) else new
        },
    ).also { skipped += it.skipped; errors += it.errors }

    val priorities = resolveRows(
        incoming.dailyPriorities.filter { row ->
            if (row.goalId !in effectiveGoals) {
                invalid(
                    BackupRecordType.DAILY_PRIORITY,
                    "${row.date}/${row.goalId}",
                    BackupIssueReason.MISSING_GOAL,
                )
                false
            } else true
        },
        baseline.dailyPriorities,
        recordType = BackupRecordType.DAILY_PRIORITY,
        identity = { "${it.date}\u0000${it.goalId}" },
        identifier = { "${it.date}/${it.goalId}" },
        matches = { old, new -> old.date == new.date && old.goalId == new.goalId },
        timestamp = { null },
    ).also { skipped += it.skipped; errors += it.errors }

    val configs = resolveRows(
        incoming.appConfigs.filter { row ->
            if (row.packageName.isBlank()) {
                invalid(
                    BackupRecordType.APP_CONFIG,
                    "(blank)",
                    BackupIssueReason.EMPTY_PACKAGE_NAME,
                )
                false
            } else if (row.linkedGoalId?.let { it !in effectiveGoals } == true) {
                invalid(BackupRecordType.APP_CONFIG, row.packageName, BackupIssueReason.MISSING_GOAL)
                false
            } else true
        },
        baseline.appConfigs,
        recordType = BackupRecordType.APP_CONFIG,
        identity = AppConfig::packageName,
        matches = { old, new -> old.packageName == new.packageName },
        timestamp = { null },
    ).also { skipped += it.skipped; errors += it.errors }

    val modes = resolveRows(
        incoming.focusModes.filter { row ->
            if (row.id.isBlank()) {
                invalid(BackupRecordType.FOCUS_MODE, "(blank)", BackupIssueReason.EMPTY_ID)
                false
            } else true
        },
        baseline.focusModes,
        recordType = BackupRecordType.FOCUS_MODE,
        identity = FocusMode::id,
        matches = { old, new -> old.id == new.id },
        timestamp = { null },
    ).also { skipped += it.skipped; errors += it.errors }

    val sessions = resolveRows(
        incoming.focusSessions.filter { row ->
            val task = row.taskId?.let(effectiveTasks::get)
            val valid = row.goalId?.let { it in effectiveGoals } != false &&
                (row.taskId == null || (task != null && row.goalId == task.goalId))
            if (!valid) {
                invalid(
                    BackupRecordType.FOCUS_SESSION,
                    row.id.toString(),
                    BackupIssueReason.INVALID_LINK,
                )
                false
            } else true
        },
        baseline.focusSessions,
        recordType = BackupRecordType.FOCUS_SESSION,
        identity = FocusSession::id,
        matches = { old, new -> old.id == new.id },
        timestamp = FocusSession::startedAt,
    ).also { skipped += it.skipped; errors += it.errors }

    val checkIns = resolveRows(
        incoming.checkIns,
        baseline.checkIns,
        recordType = BackupRecordType.CHECK_IN,
        identity = CheckIn::id,
        matches = { old, new -> old.id == new.id },
        timestamp = { it.date.replace("-", "").toLongOrNull() },
    ).also { skipped += it.skipped; errors += it.errors }

    val events = resolveRows(
        incoming.interceptEvents.filter { row ->
            if (row.packageName.isBlank()) {
                invalid(
                    BackupRecordType.INTERCEPT_EVENT,
                    row.id.toString(),
                    BackupIssueReason.EMPTY_PACKAGE_NAME,
                )
                false
            } else true
        },
        baseline.interceptEvents,
        recordType = BackupRecordType.INTERCEPT_EVENT,
        identity = InterceptEvent::id,
        matches = { old, new -> old.id == new.id },
        timestamp = InterceptEvent::timestamp,
    ).also { skipped += it.skipped; errors += it.errors }

    val updateCount = goals.updates.size + tasks.updates.size +
        taskLogs.updates.size + priorities.updates.size + configs.updates.size +
        modes.updates.size + sessions.updates.size + checkIns.updates.size + events.updates.size
    val insertCount = goals.inserts.size + tasks.inserts.size +
        taskLogs.inserts.size + priorities.inserts.size + configs.inserts.size +
        modes.inserts.size + sessions.inserts.size + checkIns.inserts.size + events.inserts.size

    return BackupImportPlan(
        goals.inserts,
        goals.updates,
        tasks.inserts,
        tasks.updates,
        taskLogs.inserts + taskLogs.updates,
        priorities.inserts + priorities.updates,
        configs.inserts + configs.updates,
        modes.inserts + modes.updates,
        sessions.inserts + sessions.updates,
        checkIns.inserts + checkIns.updates,
        events.inserts + events.updates,
        ImportReport(insertCount, updateCount, skipped, errors),
    )
}

private data class ResolvedRows<T>(
    val inserts: List<T>,
    val updates: List<T>,
    val skipped: Int,
    val errors: List<BackupImportIssue>,
)

private fun <T, K> resolveRows(
    rows: List<T>,
    existing: List<T>,
    recordType: BackupRecordType,
    identity: (T) -> K,
    identifier: (T) -> String = { identity(it).toString() },
    matches: (T, T) -> Boolean,
    timestamp: (T) -> Long?,
    preserveId: (T, T) -> T = { _, incoming -> incoming },
): ResolvedRows<T> {
    val seen = mutableSetOf<K>()
    val inserts = mutableListOf<T>()
    val updates = mutableListOf<T>()
    val accepted = mutableListOf<T>()
    val errors = mutableListOf<BackupImportIssue>()
    var skipped = 0
    rows.forEach { incoming ->
        if (!seen.add(identity(incoming)) || accepted.any { matches(it, incoming) }) {
            skipped++
            errors += BackupImportIssue(recordType, identifier(incoming), BackupIssueReason.DUPLICATE_ROW)
            return@forEach
        }
        val old = existing.firstOrNull { matches(it, incoming) }
        if (old == null) {
            inserts += incoming
            accepted += incoming
        } else {
            val oldTime = timestamp(old)
            val newTime = timestamp(incoming)
            // MERGE keeps a strictly newer stored timestamp; ties and untimestamped rows prefer import.
            if (oldTime != null && newTime != null && oldTime > newTime) {
                skipped++
            } else {
                val updated = preserveId(old, incoming)
                updates += updated
                accepted += updated
            }
        }
    }
    return ResolvedRows(inserts, updates, skipped, errors)
}
