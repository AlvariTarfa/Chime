package com.savatech.chimelauncher.data.backup

import androidx.core.app.NotificationManagerCompat
import androidx.room.withTransaction
import com.savatech.chimelauncher.core.di.IoDispatcher
import com.savatech.chimelauncher.data.db.AppDatabase
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.service.GrantExpiryScheduler
import com.savatech.chimelauncher.service.SessionCompletionScheduler
import com.savatech.chimelauncher.service.work.WorkRequestScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@Singleton
class PrivacyRepository @Inject constructor(
    private val database: AppDatabase,
    private val settings: SettingsRepository,
    private val workScheduler: WorkRequestScheduler,
    private val sessionScheduler: SessionCompletionScheduler,
    private val grantScheduler: GrantExpiryScheduler,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun deleteAllData() = withContext(ioDispatcher) {
        val grants = database.backupDao().appGrants()
        val sessions = database.focusSessionDao().observeAll().first()
        workScheduler.cancelAll()
        sessions.forEach { sessionScheduler.cancel(it.id) }
        grants.forEach { grantScheduler.cancel(it.packageName) }
        NotificationManagerCompat.from(context).cancelAll()

        database.withTransaction {
            val dao = database.backupDao()
            dao.clearTaskLogs()
            dao.clearTasks()
            dao.clearDailyPriorities()
            dao.clearAppConfigs()
            dao.clearAppGrants()
            dao.clearInterceptEvents()
            dao.clearFocusSessions()
            dao.clearCheckIns()
            dao.clearDigestItems()
            dao.clearFocusModes()
            dao.clearGoals()
            settings.clearAllSettings()
        }
    }
}
