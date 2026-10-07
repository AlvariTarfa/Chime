package com.savatech.chimelauncher.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object NotificationListenerAccess {
    fun isEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS,
        ).orEmpty()
        val component = ComponentName(context, DigestListenerService::class.java).flattenToString()
        return enabledListeners.split(':').contains(component)
    }

    private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"
}
