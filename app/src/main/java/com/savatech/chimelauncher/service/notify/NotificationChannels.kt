package com.savatech.chimelauncher.service.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.savatech.chimelauncher.R

object NotificationChannels {
    const val CHECK_INS = "check_ins"
    const val REMINDERS = "reminders"
    const val NUDGES = "nudges"
    const val SESSION_END = "focus_session_complete"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = requireNotNull(context.getSystemService(NotificationManager::class.java)) {
            "NotificationManager is unavailable."
        }
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHECK_INS,
                    context.getString(R.string.check_in_notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                NotificationChannel(
                    REMINDERS,
                    context.getString(R.string.task_reminder_notification_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
                NotificationChannel(
                    NUDGES,
                    context.getString(R.string.nudge_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
                NotificationChannel(
                    SESSION_END,
                    context.getString(R.string.focus_session_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ),
            ),
        )
    }
}
