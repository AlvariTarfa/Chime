package com.savatech.chimelauncher.service.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface WorkRequestScheduler {
    fun enqueueUnique(name: String, workerName: String, delayMillis: Long, input: Map<String, String>)
    fun cancelUnique(name: String)
    fun cancelTaskReminders()
    suspend fun cancelAll() = cancelTaskReminders()
}

@Singleton
class WorkManagerRequestScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : WorkRequestScheduler {
    override fun enqueueUnique(
        name: String,
        workerName: String,
        delayMillis: Long,
        input: Map<String, String>,
    ) {
        val workerClass = when (workerName) {
            CheckInWorker.WORKER_NAME -> CheckInWorker::class.java
            TaskReminderWorker.WORKER_NAME -> TaskReminderWorker::class.java
            NudgeWorker.WORKER_NAME -> NudgeWorker::class.java
            DigestWorker.WORKER_NAME -> DigestWorker::class.java
            else -> error("Unknown worker name: $workerName")
        }
        val request = OneTimeWorkRequest.Builder(workerClass)
            .setInitialDelay(delayMillis.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().apply {
                input.forEach { (key, value) -> putString(key, value) }
            }.build())
            .addTag(TASK_REMINDER_TAG.takeIf { workerName == TaskReminderWorker.WORKER_NAME } ?: name)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancelUnique(name: String) {
        WorkManager.getInstance(context).cancelUniqueWork(name)
    }

    override fun cancelTaskReminders() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TASK_REMINDER_TAG)
    }

    override suspend fun cancelAll() {
        withContext(Dispatchers.IO) {
            WorkManager.getInstance(context).cancelAllWork().result.get()
        }
    }

    companion object {
        const val TASK_REMINDER_TAG = "task-reminder"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkSchedulerModule {
    @Binds
    @Singleton
    abstract fun bindWorkRequestScheduler(scheduler: WorkManagerRequestScheduler): WorkRequestScheduler
}
