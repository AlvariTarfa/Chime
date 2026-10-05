package com.savatech.chimelauncher.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.BuildConfig
import com.savatech.chimelauncher.data.settings.DrawerMode
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.data.usage.UsagePermission
import com.savatech.chimelauncher.data.usage.UsageRepository
import com.savatech.chimelauncher.domain.model.AppCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val usagePermission: UsagePermission,
    private val usageRepository: UsageRepository,
    private val clock: Clock,
) : ViewModel() {
    val usagePermissionGranted: StateFlow<Boolean> = usagePermission.hasPermission.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        usagePermission.hasPermission.value,
    )

    init {
        usagePermission.refresh()
    }

    val drawerMode: StateFlow<DrawerMode> = settingsRepository.drawerMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DrawerMode.TEXT,
    )
    val categoryDailyLimits: StateFlow<Map<AppCategory, Int>> =
        settingsRepository.categoryDailyLimits.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyMap(),
        )

    fun setDrawerMode(mode: DrawerMode) {
        viewModelScope.launch { settingsRepository.setDrawerMode(mode) }
    }

    fun setCategoryDailyLimit(category: AppCategory, minutes: Int?) {
        viewModelScope.launch { settingsRepository.setCategoryDailyLimit(category, minutes) }
    }

    fun refreshUsagePermission() {
        usagePermission.refresh()
        if (BuildConfig.DEBUG && usagePermission.hasPermission.value) {
            viewModelScope.launch {
                val date = LocalDate.now(clock)
                val appUsage = usageRepository.dailyAppUsage(date)
                val pickupCount = usageRepository.pickups(date)
                Log.d(USAGE_DEBUG_TAG, "date=$date foregroundMillis=$appUsage pickups=$pickupCount")
            }
        }
    }

    fun usageAccessSettingsIntent() = usagePermission.settingsIntent()

    private companion object {
        const val USAGE_DEBUG_TAG = "ChimeUsageDebug"
    }
}
