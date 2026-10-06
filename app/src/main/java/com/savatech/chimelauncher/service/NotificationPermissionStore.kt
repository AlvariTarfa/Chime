package com.savatech.chimelauncher.service

import com.savatech.chimelauncher.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface NotificationPermissionStore {
    val notificationPermissionRequested: Flow<Boolean>
    suspend fun markNotificationPermissionRequested()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationPermissionStoreModule {
    @Binds
    @Singleton
    abstract fun bindNotificationPermissionStore(
        settingsRepository: SettingsRepository,
    ): NotificationPermissionStore
}
