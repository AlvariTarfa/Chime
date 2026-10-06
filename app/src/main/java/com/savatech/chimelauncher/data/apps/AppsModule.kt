package com.savatech.chimelauncher.data.apps

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppsModule {
    @Binds
    @Singleton
    abstract fun bindAppRepository(repository: LauncherAppsRepository): AppRepository

    @Binds
    @Singleton
    abstract fun bindAppConfigSource(repository: AppConfigRepository): AppConfigSource

    @Binds
    @Singleton
    abstract fun bindAppClassificationStore(repository: AppConfigRepository): AppClassificationStore
}
