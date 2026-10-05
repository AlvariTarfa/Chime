package com.savatech.chimelauncher.feature.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.core.launch.AppLauncher
import com.savatech.chimelauncher.core.launch.LaunchResult
import com.savatech.chimelauncher.data.apps.AppConfigRepository
import com.savatech.chimelauncher.data.apps.AppConfigResult
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.IconCache
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.settings.DrawerMode
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.domain.filterApps
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.domain.usecase.ObserveVisibleAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DrawerUiState(
    val query: String = "",
    val apps: List<AppInfo> = emptyList(),
    val configs: Map<String, AppConfig> = emptyMap(),
    val drawerMode: DrawerMode = DrawerMode.TEXT,
    val isLoading: Boolean = true,
)

@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val repository: AppRepository,
    private val appLauncher: AppLauncher,
    private val appConfigRepository: AppConfigRepository,
    val iconCache: IconCache,
    visibleApps: ObserveVisibleAppsUseCase,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<DrawerUiState> = combine(
        query,
        visibleApps(),
        repository.isLoaded,
        appConfigRepository.observeAll(),
        settingsRepository.drawerMode,
    ) { currentQuery, apps, isLoaded, configs, drawerMode ->
        DrawerUiState(
            query = currentQuery,
            apps = filterApps(apps, currentQuery),
            configs = configs,
            drawerMode = drawerMode,
            isLoading = !isLoaded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrawerUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    suspend fun requestLaunch(app: AppInfo): LaunchResult = appLauncher.requestLaunch(app)
    suspend fun setPinned(packageName: String, pinned: Boolean): AppConfigResult =
        appConfigRepository.setPinned(packageName, pinned)
    suspend fun hide(packageName: String) = appConfigRepository.setHidden(packageName, true)
    suspend fun setCategory(packageName: String, category: AppCategory) =
        appConfigRepository.setCategory(packageName, category)
    suspend fun setDailyLimit(packageName: String, minutes: Int?) =
        appConfigRepository.setDailyLimit(packageName, minutes)
    suspend fun dailyLimitWarnings(): List<String> = appLauncher.dailyLimitWarnings()
}
