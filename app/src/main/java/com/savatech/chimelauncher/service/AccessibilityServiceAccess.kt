package com.savatech.chimelauncher.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.savatech.chimelauncher.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class AccessibilityServiceAccess @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun isEnabled(): Boolean = withContext(ioDispatcher) {
        val serviceComponent = ComponentName(context, ForegroundWatcherService::class.java)
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty().split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == serviceComponent }
    }
}
