package com.savatech.chimelauncher.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.savatech.chimelauncher.data.db.dao.AppConfigDao
import com.savatech.chimelauncher.data.db.dao.AppGrantDao
import com.savatech.chimelauncher.data.db.dao.BackupDao
import com.savatech.chimelauncher.data.db.dao.CheckInDao
import com.savatech.chimelauncher.data.db.dao.DailyPriorityDao
import com.savatech.chimelauncher.data.db.dao.DigestItemDao
import com.savatech.chimelauncher.data.db.dao.FocusModeDao
import com.savatech.chimelauncher.data.db.dao.FocusSessionDao
import com.savatech.chimelauncher.data.db.dao.GoalDao
import com.savatech.chimelauncher.data.db.dao.InterceptEventDao
import com.savatech.chimelauncher.data.db.dao.TaskDao
import com.savatech.chimelauncher.data.db.dao.TaskLogDao
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.db.entities.AppGrant
import com.savatech.chimelauncher.data.db.entities.CheckIn
import com.savatech.chimelauncher.data.db.entities.DailyPriority
import com.savatech.chimelauncher.data.db.entities.DigestItem
import com.savatech.chimelauncher.data.db.entities.FocusMode
import com.savatech.chimelauncher.data.db.entities.FocusSession
import com.savatech.chimelauncher.data.db.entities.Goal
import com.savatech.chimelauncher.data.db.entities.InterceptEvent
import com.savatech.chimelauncher.data.db.entities.Task
import com.savatech.chimelauncher.data.db.entities.TaskLog

@Database(
    entities = [
        Goal::class,
        Task::class,
        TaskLog::class,
        DailyPriority::class,
        AppConfig::class,
        AppGrant::class,
        InterceptEvent::class,
        FocusMode::class,
        FocusSession::class,
        CheckIn::class,
        DigestItem::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun taskDao(): TaskDao
    abstract fun taskLogDao(): TaskLogDao
    abstract fun dailyPriorityDao(): DailyPriorityDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun appGrantDao(): AppGrantDao
    abstract fun interceptEventDao(): InterceptEventDao
    abstract fun focusModeDao(): FocusModeDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun checkInDao(): CheckInDao
    abstract fun digestItemDao(): DigestItemDao
    abstract fun backupDao(): BackupDao
}
