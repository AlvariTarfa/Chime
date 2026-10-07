package com.savatech.chimelauncher.service

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

interface SchedulerRescheduler {
    suspend fun rescheduleAll()
    suspend fun scheduleNextDigest(slot: String)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerReschedulerModule {
    @Binds
    @Singleton
    abstract fun bindSchedulerRescheduler(facade: SchedulerFacade): SchedulerRescheduler
}
