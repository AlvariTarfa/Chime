package com.savatech.chimelauncher.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.domain.focus.ManualModeOverride
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : InterceptSettings {
    val drawerMode: Flow<DrawerMode> = dataStore.data.map { preferences ->
        DrawerMode.fromStorage(preferences[Keys.drawerMode] ?: DrawerMode.TEXT.name)
    }

    override val frictionLevel: Flow<FrictionLevel> = dataStore.data.map { preferences ->
        FrictionLevel.fromStorage(preferences[Keys.frictionLevel] ?: FrictionLevel.BALANCED.name)
    }

    override val baseDelaySeconds: Flow<Int> = dataStore.data.map { preferences ->
        preferences[Keys.baseDelaySeconds]
            ?: FrictionLevel.fromStorage(
                preferences[Keys.frictionLevel] ?: FrictionLevel.BALANCED.name,
            ).defaultBaseDelaySeconds
    }
    override val categoryDailyLimits: Flow<Map<AppCategory, Int>> = dataStore.data.map { preferences ->
        AppCategory.entries.mapNotNull { category ->
            preferences[Keys.categoryDailyLimit(category)]
                ?.takeIf { it > 0 }
                ?.let { category to it }
        }.toMap()
    }

    val morningCheckInTime: Flow<String> = dataStore.data.map { it[Keys.morningCheckInTime] ?: "07:30" }
    val morningCheckInEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.morningCheckInEnabled] ?: false }
    val eveningCheckInTime: Flow<String> = dataStore.data.map { it[Keys.eveningCheckInTime] ?: "21:00" }
    val eveningCheckInEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.eveningCheckInEnabled] ?: false }
    val quietHoursStart: Flow<String?> = dataStore.data.map { it[Keys.quietHoursStart] }
    val quietHoursEnd: Flow<String?> = dataStore.data.map { it[Keys.quietHoursEnd] }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromStorage(preferences[Keys.themeMode] ?: ThemeMode.SYSTEM.name)
    }

    val useDynamicColor: Flow<Boolean> = dataStore.data.map { it[Keys.useDynamicColor] ?: true }
    val iconShape: Flow<String> = dataStore.data.map { it[Keys.iconShape] ?: DEFAULT_ICON_SHAPE }
    val fontPreset: Flow<String> = dataStore.data.map { it[Keys.fontPreset] ?: DEFAULT_FONT_PRESET }
    val iconPackPackage: Flow<String?> = dataStore.data.map { it[Keys.iconPackPackage] }
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[Keys.onboardingCompleted] ?: false }
    val assumedMinutesPerOpen: Flow<Int> = dataStore.data.map { it[Keys.assumedMinutesPerOpen] ?: 5 }
    val notificationPermissionRequested: Flow<Boolean> =
        dataStore.data.map { it[Keys.notificationPermissionRequested] ?: false }
    val focusModesSeeded: Flow<Boolean> = dataStore.data.map { it[Keys.focusModesSeeded] ?: false }
    val manualModeOverride: Flow<ManualModeOverride?> = dataStore.data.map { preferences ->
        if (preferences[Keys.manualModeOverrideSet] != true) {
            null
        } else {
            ManualModeOverride(
                modeId = preferences[Keys.manualModeId]?.takeUnless { it == NONE_MODE_ID },
                until = preferences[Keys.manualModeUntil]?.let(LocalDateTime::parse),
            )
        }
    }

    suspend fun setDrawerMode(value: DrawerMode) = dataStore.edit { it[Keys.drawerMode] = value.name }
    suspend fun setFrictionLevel(value: FrictionLevel) = dataStore.edit { it[Keys.frictionLevel] = value.name }
    suspend fun setBaseDelaySeconds(value: Int) = dataStore.edit { it[Keys.baseDelaySeconds] = value }
    suspend fun setCategoryDailyLimit(category: AppCategory, minutes: Int?) {
        require(minutes == null || minutes in 1..MAX_DAILY_LIMIT_MINUTES)
        dataStore.edit { preferences ->
            val key = Keys.categoryDailyLimit(category)
            if (minutes == null) {
                preferences.remove(key)
            } else {
                preferences[key] = minutes
            }
        }
    }
    suspend fun setMorningCheckInTime(value: String) = dataStore.edit { it[Keys.morningCheckInTime] = value }
    suspend fun setMorningCheckInEnabled(value: Boolean) =
        dataStore.edit { it[Keys.morningCheckInEnabled] = value }
    suspend fun setEveningCheckInTime(value: String) = dataStore.edit { it[Keys.eveningCheckInTime] = value }
    suspend fun setEveningCheckInEnabled(value: Boolean) =
        dataStore.edit { it[Keys.eveningCheckInEnabled] = value }
    suspend fun setQuietHoursStart(value: String?) = updateNullableString(Keys.quietHoursStart, value)
    suspend fun setQuietHoursEnd(value: String?) = updateNullableString(Keys.quietHoursEnd, value)
    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[Keys.themeMode] = value.name }
    suspend fun setUseDynamicColor(value: Boolean) = dataStore.edit { it[Keys.useDynamicColor] = value }
    suspend fun setIconShape(value: String) = dataStore.edit { it[Keys.iconShape] = value }
    suspend fun setFontPreset(value: String) = dataStore.edit { it[Keys.fontPreset] = value }
    suspend fun setIconPackPackage(value: String?) = updateNullableString(Keys.iconPackPackage, value)
    suspend fun setOnboardingCompleted(value: Boolean) = dataStore.edit { it[Keys.onboardingCompleted] = value }
    suspend fun setAssumedMinutesPerOpen(value: Int) =
        dataStore.edit { it[Keys.assumedMinutesPerOpen] = value }
    suspend fun setNotificationPermissionRequested(value: Boolean) =
        dataStore.edit { it[Keys.notificationPermissionRequested] = value }
    suspend fun markFocusModesSeeded() = dataStore.edit { it[Keys.focusModesSeeded] = true }
    suspend fun setManualModeOverride(modeId: String?, until: LocalDateTime?) = dataStore.edit {
        it[Keys.manualModeOverrideSet] = true
        it[Keys.manualModeId] = modeId ?: NONE_MODE_ID
        if (until == null) it.remove(Keys.manualModeUntil) else it[Keys.manualModeUntil] = until.toString()
    }
    suspend fun clearManualModeOverride() = dataStore.edit {
        it.remove(Keys.manualModeOverrideSet)
        it.remove(Keys.manualModeId)
        it.remove(Keys.manualModeUntil)
    }

    private suspend fun updateNullableString(key: Preferences.Key<String>, value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }

    private object Keys {
        val drawerMode = stringPreferencesKey("drawer_mode")
        val frictionLevel = stringPreferencesKey("friction_level")
        val baseDelaySeconds = intPreferencesKey("base_delay_seconds")
        fun categoryDailyLimit(category: AppCategory) =
            intPreferencesKey("category_daily_limit_${category.name.lowercase()}")
        val morningCheckInTime = stringPreferencesKey("morning_check_in_time")
        val morningCheckInEnabled = booleanPreferencesKey("morning_check_in_enabled")
        val eveningCheckInTime = stringPreferencesKey("evening_check_in_time")
        val eveningCheckInEnabled = booleanPreferencesKey("evening_check_in_enabled")
        val quietHoursStart = stringPreferencesKey("quiet_hours_start")
        val quietHoursEnd = stringPreferencesKey("quiet_hours_end")
        val themeMode = stringPreferencesKey("theme_mode")
        val useDynamicColor = booleanPreferencesKey("use_dynamic_color")
        val iconShape = stringPreferencesKey("icon_shape")
        val fontPreset = stringPreferencesKey("font_preset")
        val iconPackPackage = stringPreferencesKey("icon_pack_package")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val assumedMinutesPerOpen = intPreferencesKey("assumed_minutes_per_open")
        val notificationPermissionRequested = booleanPreferencesKey("notification_permission_requested")
        val focusModesSeeded = booleanPreferencesKey("focus_modes_seeded")
        val manualModeOverrideSet = booleanPreferencesKey("manual_mode_override_set")
        val manualModeId = stringPreferencesKey("manual_mode_id")
        val manualModeUntil = stringPreferencesKey("manual_mode_until")
    }

    private companion object {
        const val DEFAULT_ICON_SHAPE = "circle"
        const val DEFAULT_FONT_PRESET = "system"
        const val MAX_DAILY_LIMIT_MINUTES = 720
        const val NONE_MODE_ID = "__none__"
    }
}
