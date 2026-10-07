package com.savatech.chimelauncher.domain.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BackupModel(
    val schemaVersion: Int,
    val exportedAt: String,
    val goals: List<BackupGoal> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
    val taskLogs: List<BackupTaskLog> = emptyList(),
    val dailyPriorities: List<BackupDailyPriority> = emptyList(),
    val appConfigs: List<BackupAppConfig> = emptyList(),
    val focusModes: List<BackupFocusMode> = emptyList(),
    val focusSessions: List<BackupFocusSession> = emptyList(),
    val checkIns: List<BackupCheckIn> = emptyList(),
    val interceptEvents: List<BackupInterceptEvent> = emptyList(),
    val settings: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class BackupGoal(
    val id: String,
    val title: String,
    val why: String? = null,
    val category: String,
    val targetDate: String? = null,
    val unit: String? = null,
    val targetValue: Double? = null,
    val status: String,
    val createdAt: Long,
)

@Serializable
data class BackupTask(
    val id: String,
    val goalId: String,
    val title: String,
    val recurrence: String,
    val daysMask: Int,
    val reminderTime: String? = null,
    val createdAt: Long,
)

@Serializable
data class BackupTaskLog(
    val id: Long = 0,
    val taskId: String,
    val date: String,
    val value: Double? = null,
    val completed: Boolean,
)

@Serializable
data class BackupDailyPriority(
    val date: String,
    val goalId: String,
    val position: Int,
)

@Serializable
data class BackupAppConfig(
    val packageName: String,
    val category: String,
    val pinned: Boolean,
    val pinOrder: Int,
    val hidden: Boolean,
    val dailyLimitMin: Int? = null,
    val linkedGoalId: String? = null,
)

@Serializable
data class BackupFocusMode(
    val id: String,
    val name: String,
    val allowedPackages: String,
    val scheduleJson: String? = null,
    val suppressNotifications: Boolean,
    val isBuiltIn: Boolean,
)

@Serializable
data class BackupFocusSession(
    val id: Long = 0,
    val goalId: String? = null,
    val taskId: String? = null,
    val startedAt: Long,
    val plannedMinutes: Int,
    val endedAt: Long? = null,
    val completed: Boolean,
)

@Serializable
data class BackupCheckIn(
    val id: Long = 0,
    val type: String,
    val date: String,
    val mood: Int? = null,
    val notes: String? = null,
    val payloadJson: String? = null,
)

@Serializable
data class BackupInterceptEvent(
    val id: Long = 0,
    val packageName: String,
    val timestamp: Long,
    val outcome: String,
    val grantedMinutes: Int? = null,
    val reason: String? = null,
)

enum class BackupImportMode {
    MERGE,
    REPLACE,
}

data class ImportReport(
    val inserted: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val errors: List<BackupImportIssue> = emptyList(),
)

data class BackupImportIssue(
    val recordType: BackupRecordType,
    val identifier: String,
    val reason: BackupIssueReason,
)

enum class BackupRecordType {
    GOAL,
    TASK,
    TASK_LOG,
    DAILY_PRIORITY,
    APP_CONFIG,
    FOCUS_MODE,
    FOCUS_SESSION,
    CHECK_IN,
    INTERCEPT_EVENT,
}

enum class BackupIssueReason {
    EMPTY_ID,
    EMPTY_PACKAGE_NAME,
    MISSING_GOAL,
    MISSING_TASK,
    INVALID_LINK,
    DUPLICATE_ROW,
}

sealed class BackupError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data class NewerSchemaVersion(val found: Int, val supported: Int) :
        BackupError("Backup schema version $found is newer than supported version $supported.")

    data class InvalidSchemaVersion(val found: Int) :
        BackupError("Backup schema version $found is not supported.")

    class MalformedJson(cause: Throwable) :
        BackupError("The selected file is not a valid Chime backup.", cause)

    data class InvalidSettings(val key: String) :
        BackupError("Backup contains an invalid value for setting \"$key\".")
}
