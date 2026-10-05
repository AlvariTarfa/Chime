package com.savatech.chimelauncher.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCompletionScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun schedule(sessionId: Long, completesAt: Long) {
        createNotificationChannel()
        val alarm = requireNotNull(context.getSystemService(AlarmManager::class.java)) {
            "AlarmManager is unavailable."
        }
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            completesAt,
            pendingIntent(sessionId),
        )
    }

    fun cancel(sessionId: Long) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(sessionId))
    }

    private fun pendingIntent(sessionId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        sessionId.hashCode(),
        Intent(context, SessionCompleteReceiver::class.java).apply {
            data = android.net.Uri.parse("chime://focus-session/$sessionId")
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = requireNotNull(context.getSystemService(NotificationManager::class.java)) {
                "NotificationManager is unavailable."
            }
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(com.savatech.chimelauncher.R.string.focus_session_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "focus_session_complete"
    }
}

class SessionCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, SessionCompletionScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(com.savatech.chimelauncher.R.string.app_name))
            .setContentText(context.getString(com.savatech.chimelauncher.R.string.session_complete))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(
            intent.data?.toString().hashCode(),
            notification,
        )
    }
}
