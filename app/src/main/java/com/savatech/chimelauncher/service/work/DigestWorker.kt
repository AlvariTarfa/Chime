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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.savatech.chimelauncher.MainActivity
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.core.navigation.Routes
import com.savatech.chimelauncher.domain.notification.DigestSummaryBuilder
import com.savatech.chimelauncher.service.notify.NotificationChannels
import java.time.Duration

class DigestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val slot = inputData.getString(KEY_SLOT) ?: return Result.failure()
        if (slot !in setOf(MIDDAY, EVENING)) return Result.failure()
        val deps = dependencies()
        try {
            val dao = deps.digestItemDao()
            dao.deleteDeliveredBefore(deps.clock().millis() - DIGEST_RETENTION_MILLIS)
            val items = dao.getUndelivered()
            val summary = DigestSummaryBuilder.build(items)
            if (
                summary != null &&
                (
                    Build.VERSION.SDK_INT < 33 ||
                        ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                    )
            ) {
                NotificationChannels.create(applicationContext)
                val appLabels = deps.appRepository().apps.value
                    .associate { it.packageName to it.label }
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(MainActivity.EXTRA_NOTIFICATION_ROUTE, Routes.DigestItems)
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    NOTIFICATION_ID,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val inbox = NotificationCompat.InboxStyle()
                summary.inboxLines.forEach { line ->
                    inbox.addLine(
                        applicationContext.getString(
                            R.string.digest_app_count,
                            appLabels[line.packageName] ?: line.packageName,
                            line.count,
                        ),
                    )
                }
                val notification = NotificationCompat.Builder(
                    applicationContext,
                    NotificationChannels.DIGEST,
                )
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(
                        applicationContext.getString(R.string.digest_notification_title, summary.itemCount),
                    )
                    .setContentText(applicationContext.getString(R.string.digest_notification_content))
                    .setStyle(inbox)
                    .setNumber(summary.itemCount)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
                dao.markDelivered(items.map { it.id })
            }
        } finally {
            deps.schedulerFacade().scheduleNextDigest(slot)
        }
        return Result.success()
    }

    companion object {
        const val WORKER_NAME = "notification-digest"
        const val KEY_SLOT = "digest_slot"
        const val MIDDAY = "midday"
        const val EVENING = "evening"
        const val NOTIFICATION_ID = 16_016
        private val DIGEST_RETENTION_MILLIS = Duration.ofDays(7).toMillis()
    }
}
