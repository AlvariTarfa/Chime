package com.savatech.chimelauncher.feature.goals

import com.savatech.chimelauncher.service.NotificationPermissionStore
import com.savatech.chimelauncher.service.SchedulerRescheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal object TestSchedulerRescheduler : SchedulerRescheduler {
    override suspend fun rescheduleAll() = Unit
    override suspend fun scheduleNextDigest(slot: String) = Unit
}

internal object TestNotificationPermissionStore : NotificationPermissionStore {
    override val notificationPermissionRequested: Flow<Boolean> = MutableStateFlow(false)
    override suspend fun markNotificationPermissionRequested() = Unit
}
