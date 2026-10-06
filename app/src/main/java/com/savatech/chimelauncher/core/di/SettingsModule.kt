package com.savatech.chimelauncher.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.savatech.chimelauncher.data.settings.OnboardingSettings
import com.savatech.chimelauncher.data.settings.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(SETTINGS_FILE_NAME)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    abstract class OnboardingSettingsModule {
        @Binds
        @Singleton
        abstract fun bindOnboardingSettings(repository: SettingsRepository): OnboardingSettings
    }

    private const val SETTINGS_FILE_NAME = "settings"
}
