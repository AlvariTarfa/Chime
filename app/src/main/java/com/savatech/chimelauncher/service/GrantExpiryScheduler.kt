package com.savatech.chimelauncher.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GrantExpiryScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun schedule(packageName: String, appLabel: String, expiresAt: Long) {
        createNotificationChannel()
        val alarmManager = requireNotNull(context.getSystemService(AlarmManager::class.java)) {
            "AlarmManager is unavailable."
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            expiresAt,
            pendingIntent(packageName, appLabel),
        )
    }

    fun cancel(packageName: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(packageName))
    }

    private fun pendingIntent(packageName: String, appLabel: String? = null): PendingIntent {
        val intent = Intent(context, GrantExpiryReceiver::class.java).apply {
            if (appLabel != null) putExtra(GrantExpiryReceiver.EXTRA_APP_LABEL, appLabel)
            data = android.net.Uri.parse("chime://grant-expiry/$packageName")
        }
        return PendingIntent.getBroadcast(
            context,
            packageName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        val manager = requireNotNull(context.getSystemService(NotificationManager::class.java)) {
            "NotificationManager is unavailable."
        }
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(com.savatech.chimelauncher.R.string.grant_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    companion object {
        const val CHANNEL_ID = "grant_expiry"
    }
}

class GrantExpiryReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val label = intent.getStringExtra(EXTRA_APP_LABEL) ?: return
        val notification = NotificationCompat.Builder(context, GrantExpiryScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(com.savatech.chimelauncher.R.string.app_name))
            .setContentText(context.getString(com.savatech.chimelauncher.R.string.grant_expired, label))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(intent.data?.toString().hashCode(), notification)
        } catch (_: SecurityException) {
            return
        }
    }

    companion object {
        const val EXTRA_APP_LABEL = "app_label"
    }
}
