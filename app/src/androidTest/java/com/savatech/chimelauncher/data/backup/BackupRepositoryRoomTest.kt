package com.savatech.chimelauncher.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import com.savatech.chimelauncher.data.settings.DrawerMode
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.domain.backup.BackupImportMode
import com.savatech.chimelauncher.service.SchedulerRescheduler
import com.savatech.chimelauncher.service.SessionCompletionScheduler
import java.io.File
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRepositoryRoomTest {
    @Test
    fun exportThenReplaceIntoEmptyRoomDatabaseRestoresBackupRowsAndSettings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sourceDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val targetDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val sourceSettings = settingsRepository(context.cacheDir, "source")
        val targetSettings = settingsRepository(context.cacheDir, "target")
        val sourceRepository = backupRepository(sourceDatabase, sourceSettings, context)
        val targetRepository = backupRepository(targetDatabase, targetSettings, context)
        try {
            val goal = Goal("goal", "Goal", null, "PRODUCTIVE", null, null, null, "ACTIVE", 10L)
            val task = Task("task", goal.id, "Task", "DAILY", 127, null, 11L)
            sourceDatabase.goalDao().insert(goal)
            sourceDatabase.taskDao().insert(task)
            sourceDatabase.taskLogDao().insert(TaskLog(1L, task.id, "2026-10-07", 2.0, true))
            sourceDatabase.dailyPriorityDao().insert(DailyPriority("2026-10-07", goal.id, 0))
            sourceDatabase.appConfigDao().insert(
                AppConfig("sample.app", "DISTRACTING", true, 0, false, 20, goal.id),
            )
            sourceDatabase.focusModeDao().insert(
                FocusMode("mode", "Mode", "sample.app", null, true, false),
            )
            sourceDatabase.focusSessionDao().insert(
                FocusSession(1L, goal.id, task.id, 100L, 25, 200L, true),
            )
            sourceDatabase.checkInDao().insert(
                CheckIn(1L, "EVENING", "2026-10-07", 4, "note", null),
            )
            sourceDatabase.interceptEventDao().insert(
                InterceptEvent(1L, "sample.app", 300L, "OPENED", 10, "reason"),
            )
            sourceSettings.setDrawerMode(DrawerMode.GRID)

            val exported = sourceRepository.export()
            targetRepository.import(exported, BackupImportMode.REPLACE)

            assertEquals(sourceDatabase.backupDao().goals(), targetDatabase.backupDao().goals())
            assertEquals(sourceDatabase.backupDao().tasks(), targetDatabase.backupDao().tasks())
            assertEquals(sourceDatabase.backupDao().taskLogs(), targetDatabase.backupDao().taskLogs())
            assertEquals(
                sourceDatabase.backupDao().dailyPriorities(),
                targetDatabase.backupDao().dailyPriorities(),
            )
            assertEquals(sourceDatabase.backupDao().appConfigs(), targetDatabase.backupDao().appConfigs())
            assertEquals(sourceDatabase.backupDao().focusModes(), targetDatabase.backupDao().focusModes())
            assertEquals(
                sourceDatabase.backupDao().focusSessions(),
                targetDatabase.backupDao().focusSessions(),
            )
            assertEquals(sourceDatabase.backupDao().checkIns(), targetDatabase.backupDao().checkIns())
            assertEquals(
                sourceDatabase.backupDao().interceptEvents(),
                targetDatabase.backupDao().interceptEvents(),
            )
            assertEquals(DrawerMode.GRID, targetSettings.drawerMode.first())
        } finally {
            sourceDatabase.close()
            targetDatabase.close()
        }
    }

    private fun settingsRepository(directory: File, prefix: String): SettingsRepository {
        val file = File(directory, "$prefix-${UUID.randomUUID()}.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create { file }
        return SettingsRepository(dataStore)
    }

    private fun backupRepository(
        database: AppDatabase,
        settings: SettingsRepository,
        context: android.content.Context,
    ) = BackupRepository(
        database = database,
        settings = settings,
        schedulerRescheduler = NoOpSchedulerRescheduler,
        sessionScheduler = SessionCompletionScheduler(context),
        clock = Clock.systemUTC(),
        ioDispatcher = Dispatchers.IO,
    )

    private object NoOpSchedulerRescheduler : SchedulerRescheduler {
        override suspend fun rescheduleAll() = Unit
        override suspend fun scheduleNextDigest(slot: String) = Unit
    }
}
