package com.savatech.chimelauncher.data.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.savatech.chimelauncher.domain.model.AppCategory
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

interface InterceptSettings {
    val frictionLevel: Flow<FrictionLevel>
    val baseDelaySeconds: Flow<Int>
    val categoryDailyLimits: Flow<Map<AppCategory, Int>>
}

@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptSettingsModule {
    @Binds
    @Singleton
    abstract fun bindInterceptSettings(repository: SettingsRepository): InterceptSettings
}
