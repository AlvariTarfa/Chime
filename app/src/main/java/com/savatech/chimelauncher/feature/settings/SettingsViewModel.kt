package com.savatech.chimelauncher.feature.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.BuildConfig
import com.savatech.chimelauncher.data.settings.DrawerMode
import com.savatech.chimelauncher.data.settings.FontPreset
import com.savatech.chimelauncher.data.settings.IconShape
import com.savatech.chimelauncher.data.settings.LayoutDensityPreset
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.data.settings.SwipeAppTarget
import com.savatech.chimelauncher.data.settings.ThemeMode
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.IconCache
import com.savatech.chimelauncher.data.apps.IconPackRepository
import com.savatech.chimelauncher.data.apps.InstalledIconPack
import com.savatech.chimelauncher.domain.usecase.ObserveVisibleAppsUseCase
import com.savatech.chimelauncher.data.usage.UsagePermission
import com.savatech.chimelauncher.data.usage.UsageRepository
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.service.SchedulerRescheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val iconPackRepository: IconPackRepository,
    visibleApps: ObserveVisibleAppsUseCase,
    val iconCache: IconCache,
    private val usagePermission: UsagePermission,
    private val usageRepository: UsageRepository,
    private val schedulerFacade: SchedulerRescheduler,
    private val clock: Clock,
) : ViewModel() {
    val themeMode = settingsRepository.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM,
    )
    val useDynamicColor = settingsRepository.useDynamicColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true,
    )
    val accentColorIndex = settingsRepository.accentColorIndex.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0,
    )
    val iconShape = settingsRepository.iconShape.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), IconShape.CIRCLE,
    )
    val fontPreset = settingsRepository.fontPreset.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), FontPreset.DEFAULT,
    )
    val layoutDensity = settingsRepository.layoutDensity.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), LayoutDensityPreset.COMFORTABLE,
    )
    val iconPackPackage = settingsRepository.iconPackPackage.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )
    val leftSwipeTarget = settingsRepository.leftSwipeTarget.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )
    val rightSwipeTarget = settingsRepository.rightSwipeTarget.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )
    val availableApps = visibleApps().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    private val mutableIconPacks = MutableStateFlow(IconPackUiState())
    val iconPacks: StateFlow<IconPackUiState> = mutableIconPacks

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
    val assumedMinutesPerOpen = settingsRepository.assumedMinutesPerOpen.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 5,
    )
    val focusScoreEnabled = settingsRepository.focusScoreEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )
    val morningCheckInEnabled = settingsRepository.morningCheckInEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )
    val morningCheckInTime = settingsRepository.morningCheckInTime.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "07:30",
    )
    val eveningCheckInEnabled = settingsRepository.eveningCheckInEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )
    val eveningCheckInTime = settingsRepository.eveningCheckInTime.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "21:00",
    )
    val weeklyCheckInEnabled = settingsRepository.weeklyCheckInEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )
    val weeklyCheckInTime = settingsRepository.weeklyCheckInTime.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "17:00",
    )
    val weeklyCheckInDay = settingsRepository.weeklyCheckInDay.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "SUNDAY",
    )
    val nudgesEnabled = settingsRepository.nudgesEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )
    val quietHoursStart = settingsRepository.quietHoursStart.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )
    val quietHoursEnd = settingsRepository.quietHoursEnd.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )
    val notificationPermissionRequested = settingsRepository.notificationPermissionRequested.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )

    fun setDrawerMode(mode: DrawerMode) {
        viewModelScope.launch { settingsRepository.setDrawerMode(mode) }
    }

    fun setThemeMode(mode: ThemeMode) = updateSettings { setThemeMode(mode) }
    fun setUseDynamicColor(enabled: Boolean) = updateSettings { setUseDynamicColor(enabled) }
    fun setAccentColorIndex(index: Int) = updateSettings { setAccentColorIndex(index) }
    fun setIconShape(shape: IconShape) = updateSettings { setIconShape(shape) }
    fun setFontPreset(preset: FontPreset) = updateSettings { setFontPreset(preset) }
    fun setLayoutDensity(preset: LayoutDensityPreset) = updateSettings {
        setLayoutDensity(preset)
    }
    fun setIconPack(packageName: String?) = updateSettings { setIconPackPackage(packageName) }
    fun setLeftSwipeApp(app: AppInfo?) = updateSettings {
        setLeftSwipeTarget(app?.asSwipeTarget())
    }
    fun setRightSwipeApp(app: AppInfo?) = updateSettings {
        setRightSwipeTarget(app?.asSwipeTarget())
    }

    fun refreshIconPacks() {
        viewModelScope.launch {
            mutableIconPacks.value = IconPackUiState(isLoading = true)
            try {
                mutableIconPacks.value = IconPackUiState(
                    packs = iconPackRepository.discoverInstalledPacks(),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableIconPacks.value = IconPackUiState(loadFailed = true)
            }
        }
    }

    private fun AppInfo.asSwipeTarget() = SwipeAppTarget(packageName, className, userSerial)

    private fun updateSettings(update: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.update() }
    }

    fun setCategoryDailyLimit(category: AppCategory, minutes: Int?) {
        viewModelScope.launch { settingsRepository.setCategoryDailyLimit(category, minutes) }
    }

    data class IconPackUiState(
        val packs: List<InstalledIconPack> = emptyList(),
        val isLoading: Boolean = false,
        val loadFailed: Boolean = false,
    )

    fun setAssumedMinutesPerOpen(minutes: Int) {
        viewModelScope.launch { settingsRepository.setAssumedMinutesPerOpen(minutes) }
    }

    fun setFocusScoreEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setFocusScoreEnabled(enabled) }
    }

    fun setMorningCheckInEnabled(enabled: Boolean) = updateNotifications {
        settingsRepository.setMorningCheckInEnabled(enabled)
    }
    fun setMorningCheckInTime(value: String) = updateNotifications {
        settingsRepository.setMorningCheckInTime(value)
    }
    fun setEveningCheckInEnabled(enabled: Boolean) = updateNotifications {
        settingsRepository.setEveningCheckInEnabled(enabled)
    }
    fun setEveningCheckInTime(value: String) = updateNotifications {
        settingsRepository.setEveningCheckInTime(value)
    }
    fun setWeeklyCheckInEnabled(enabled: Boolean) = updateNotifications {
        settingsRepository.setWeeklyCheckInEnabled(enabled)
    }
    fun setWeeklyCheckInTime(value: String) = updateNotifications {
        settingsRepository.setWeeklyCheckInTime(value)
    }
    fun setWeeklyCheckInDay(value: DayOfWeek) = updateNotifications {
        settingsRepository.setWeeklyCheckInDay(value.name)
    }
    fun setNudgesEnabled(enabled: Boolean) = updateNotifications {
        settingsRepository.setNudgesEnabled(enabled)
    }
    fun setQuietHoursStart(value: String?) = updateNotifications {
        settingsRepository.setQuietHoursStart(value)
    }
    fun setQuietHoursEnd(value: String?) = updateNotifications {
        settingsRepository.setQuietHoursEnd(value)
    }

    fun markNotificationPermissionRequested() {
        viewModelScope.launch { settingsRepository.setNotificationPermissionRequested(true) }
    }

    private fun updateNotifications(update: suspend () -> Unit) {
        viewModelScope.launch {
            update()
            schedulerFacade.rescheduleAll()
        }
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
