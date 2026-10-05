package com.savatech.chimelauncher.data.apps

import kotlinx.coroutines.flow.StateFlow

interface AppRepository {
    val apps: StateFlow<List<AppInfo>>
    val isLoaded: StateFlow<Boolean>

    suspend fun launch(app: AppInfo): Result<Unit>
}
