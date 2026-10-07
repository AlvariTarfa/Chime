package com.savatech.chimelauncher.data.backup

import androidx.room.withTransaction
import com.savatech.chimelauncher.core.di.IoDispatcher
import com.savatech.chimelauncher.data.db.AppDatabase
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.db.entities.CheckIn
import com.savatech.chimelauncher.data.db.entities.DailyPriority
import com.savatech.chimelauncher.data.db.entities.FocusMode
import com.savatech.chimelauncher.data.db.entities.FocusSession
import com.savatech.chimelauncher.data.db.entities.Goal
import com.savatech.chimelauncher.data.db.entities.InterceptEvent
import com.savatech.chimelauncher.data.db.entities.Task
import com.savatech.chimelauncher.data.db.entities.TaskLog
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.domain.backup.BackupCodec
import com.savatech.chimelauncher.domain.backup.BackupImportMode
import com.savatech.chimelauncher.domain.backup.BackupModel
import com.savatech.chimelauncher.domain.backup.ImportReport
import com.savatech.chimelauncher.service.SchedulerRescheduler
import com.savatech.chimelauncher.service.SessionCompletionScheduler
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class BackupRepository @Inject constructor(
    private val database: AppDatabase,
    private val settings: SettingsRepository,
    private val schedulerRescheduler: SchedulerRescheduler,
    private val sessionScheduler: SessionCompletionScheduler,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val backupDao get() = database.backupDao()

    suspend fun export(): String = withContext(ioDispatcher) {
        database.withTransaction {
            val entities = readEntities()
            val backup = BackupModel(
                schemaVersion = BackupCodec.CURRENT_SCHEMA_VERSION,
                exportedAt = LocalDateTime.now(clock).toString(),
                goals = entities.goals.map(Goal::toBackup),
                tasks = entities.tasks.map(Task::toBackup),
                taskLogs = entities.taskLogs.map(TaskLog::toBackup),
                dailyPriorities = entities.dailyPriorities.map(DailyPriority::toBackup),
                appConfigs = entities.appConfigs.map(AppConfig::toBackup),
                focusModes = entities.focusModes.map(FocusMode::toBackup),
                focusSessions = entities.focusSessions.map(FocusSession::toBackup),
                checkIns = entities.checkIns.map(CheckIn::toBackup),
                interceptEvents = entities.interceptEvents.map(InterceptEvent::toBackup),
                settings = settings.exportBackupSettings(),
            )
            BackupCodec.encode(backup)
        }
    }

    suspend fun previewImport(source: String, mode: BackupImportMode): ImportReport =
        withContext(ioDispatcher) {
            val backup = BackupCodec.decode(source)
            settings.validateBackupSettings(backup.settings)
            database.withTransaction {
                val current = readEntities()
                val plan = planImport(backup.toEntityLists(), current, mode)
                plan.report.withSettings(
                    settings = settings.recognizedBackupSettings(backup.settings),
                    currentSettings = settings.exportBackupSettings(),
                    mode = mode,
                )
            }
        }

    suspend fun import(source: String, mode: BackupImportMode): ImportReport =
        withContext(ioDispatcher) {
            val backup = BackupCodec.decode(source)
            settings.validateBackupSettings(backup.settings)
            val report = database.withTransaction {
                val current = readEntities()
                val plan = planImport(backup.toEntityLists(), current, mode)
                if (mode == BackupImportMode.REPLACE) {
                    clearAllRows()
                }
                writePlan(plan, mode)
                settings.importBackupSettings(backup.settings, replace = mode == BackupImportMode.REPLACE)
                plan.report.withSettings(
                    settings = settings.recognizedBackupSettings(backup.settings),
                    currentSettings = settings.exportBackupSettings(),
                    mode = mode,
                )
            }
            schedulerRescheduler.rescheduleAll()
            database.focusSessionDao().getUnfinished().forEach { session ->
                sessionScheduler.schedule(
                    session.id,
                    session.startedAt + session.plannedMinutes * MILLIS_PER_MINUTE,
                )
            }
            report
        }

    private suspend fun readEntities() = BackupEntityLists(
        goals = backupDao.goals(),
        tasks = backupDao.tasks(),
        taskLogs = backupDao.taskLogs(),
        dailyPriorities = backupDao.dailyPriorities(),
        appConfigs = backupDao.appConfigs(),
        focusModes = backupDao.focusModes(),
        focusSessions = backupDao.focusSessions(),
        checkIns = backupDao.checkIns(),
        interceptEvents = backupDao.interceptEvents(),
    )

    private suspend fun clearAllRows() {
        backupDao.clearTaskLogs()
        backupDao.clearTasks()
        backupDao.clearDailyPriorities()
        backupDao.clearAppConfigs()
        backupDao.clearAppGrants()
        backupDao.clearInterceptEvents()
        backupDao.clearFocusSessions()
        backupDao.clearCheckIns()
        backupDao.clearDigestItems()
        backupDao.clearFocusModes()
        backupDao.clearGoals()
    }

    private suspend fun writePlan(plan: BackupImportPlan, mode: BackupImportMode) {
        backupDao.putGoals(plan.insertGoals)
        if (mode == BackupImportMode.MERGE) backupDao.updateGoals(plan.updateGoals)
        backupDao.putTasks(plan.insertTasks)
        if (mode == BackupImportMode.MERGE) backupDao.updateTasks(plan.updateTasks)
        backupDao.putTaskLogs(plan.taskLogs)
        backupDao.putDailyPriorities(plan.dailyPriorities)
        backupDao.putAppConfigs(plan.appConfigs)
        backupDao.putFocusModes(plan.focusModes)
        backupDao.putFocusSessions(plan.focusSessions)
        backupDao.putCheckIns(plan.checkIns)
        backupDao.putInterceptEvents(plan.interceptEvents)
    }

    private fun ImportReport.withSettings(
        settings: Map<String, kotlinx.serialization.json.JsonElement>,
        currentSettings: Map<String, kotlinx.serialization.json.JsonElement>,
        mode: BackupImportMode,
    ): ImportReport {
        val insertedSettings = if (mode == BackupImportMode.REPLACE) {
            settings.size
        } else {
            settings.keys.count { it !in currentSettings }
        }
        val updatedSettings = if (mode == BackupImportMode.MERGE) {
            settings.keys.count { it in currentSettings }
        } else 0
        return copy(inserted = inserted + insertedSettings, updated = updated + updatedSettings)
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = BackupCodec.CURRENT_SCHEMA_VERSION
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
