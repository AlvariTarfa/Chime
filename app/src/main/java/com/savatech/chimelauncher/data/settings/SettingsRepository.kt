package com.savatech.chimelauncher.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.domain.focus.ManualModeOverride
import com.savatech.chimelauncher.service.NotificationPermissionStore
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement

interface OnboardingSettings {
    val onboardingCompleted: Flow<Boolean>
    val frictionLevel: Flow<FrictionLevel>
    val frictionConfigured: Flow<Boolean>
    suspend fun setOnboardingCompleted(value: Boolean)
    suspend fun setFrictionLevel(value: FrictionLevel)
    suspend fun setBaseDelaySeconds(value: Int)
}

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : InterceptSettings, NotificationPermissionStore, OnboardingSettings {
    private val backupAdapter = SettingsBackupAdapter(dataStore)
    val drawerMode: Flow<DrawerMode> = dataStore.data.map { preferences ->
        DrawerMode.fromStorage(preferences[Keys.drawerMode] ?: DrawerMode.TEXT.name)
    }

    override val frictionLevel: Flow<FrictionLevel> = dataStore.data.map { preferences ->
        FrictionLevel.fromStorage(preferences[Keys.frictionLevel] ?: FrictionLevel.BALANCED.name)
    }
    override val frictionConfigured: Flow<Boolean> =
        dataStore.data.map { it[Keys.frictionLevel] != null }

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
    val weeklyCheckInDay: Flow<String> = dataStore.data.map { it[Keys.weeklyCheckInDay] ?: "SUNDAY" }
    val weeklyCheckInTime: Flow<String> = dataStore.data.map { it[Keys.weeklyCheckInTime] ?: "17:00" }
    val weeklyCheckInEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.weeklyCheckInEnabled] ?: false }
    val nudgesEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.nudgesEnabled] ?: false }
    val quietHoursStart: Flow<String?> = dataStore.data.map { it[Keys.quietHoursStart] }
    val quietHoursEnd: Flow<String?> = dataStore.data.map { it[Keys.quietHoursEnd] }
    val digestEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.digestEnabled] ?: false }
    val digestFirstTime: Flow<String> = dataStore.data.map { it[Keys.digestFirstTime] ?: "12:00" }
    val digestSecondTime: Flow<String> = dataStore.data.map { it[Keys.digestSecondTime] ?: "18:00" }
    val digestAllowList: Flow<Set<String>> =
        dataStore.data.map { it[Keys.digestAllowList]?.toSet() ?: emptySet() }
    val digestConsentAccepted: Flow<Boolean> =
        dataStore.data.map { it[Keys.digestConsentAccepted] ?: false }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.fromStorage(preferences[Keys.themeMode] ?: ThemeMode.SYSTEM.name)
    }

    val useDynamicColor: Flow<Boolean> = dataStore.data.map { it[Keys.useDynamicColor] ?: true }
    val iconShape: Flow<IconShape> = dataStore.data.map {
        IconShape.fromStorage(it[Keys.iconShape] ?: IconShape.CIRCLE.name)
    }
    val fontPreset: Flow<FontPreset> = dataStore.data.map {
        FontPreset.fromStorage(it[Keys.fontPreset] ?: FontPreset.DEFAULT.name)
    }
    val layoutDensity: Flow<LayoutDensityPreset> = dataStore.data.map {
        LayoutDensityPreset.fromStorage(it[Keys.layoutDensity] ?: LayoutDensityPreset.COMFORTABLE.name)
    }
    val accentColorIndex: Flow<Int> = dataStore.data.map {
        (it[Keys.accentColorIndex] ?: 0).coerceIn(0, ACCENT_COLOR_COUNT - 1)
    }
    val iconPackPackage: Flow<String?> = dataStore.data.map { it[Keys.iconPackPackage] }
    val leftSwipeTarget: Flow<SwipeAppTarget?> = dataStore.data.map { it[Keys.leftSwipeTarget]?.toSwipeAppTarget() }
    val rightSwipeTarget: Flow<SwipeAppTarget?> = dataStore.data.map { it[Keys.rightSwipeTarget]?.toSwipeAppTarget() }
    override val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[Keys.onboardingCompleted] ?: false }
    val assumedMinutesPerOpen: Flow<Int> = dataStore.data.map { it[Keys.assumedMinutesPerOpen] ?: 5 }
    val focusScoreEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.focusScoreEnabled] ?: false }
    override val notificationPermissionRequested: Flow<Boolean> =
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
    override suspend fun setFrictionLevel(value: FrictionLevel) {
        dataStore.edit { it[Keys.frictionLevel] = value.name }
    }
    override suspend fun setBaseDelaySeconds(value: Int) {
        dataStore.edit { it[Keys.baseDelaySeconds] = value }
    }
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
    suspend fun setWeeklyCheckInDay(value: String) {
        require(java.time.DayOfWeek.entries.any { it.name == value })
        dataStore.edit { it[Keys.weeklyCheckInDay] = value }
    }
    suspend fun setWeeklyCheckInTime(value: String) = dataStore.edit { it[Keys.weeklyCheckInTime] = value }
    suspend fun setWeeklyCheckInEnabled(value: Boolean) =
        dataStore.edit { it[Keys.weeklyCheckInEnabled] = value }
    suspend fun setNudgesEnabled(value: Boolean) = dataStore.edit { it[Keys.nudgesEnabled] = value }
    suspend fun setDigestEnabled(value: Boolean) =
        dataStore.edit { it[Keys.digestEnabled] = value }
    suspend fun setDigestFirstTime(value: String) {
        java.time.LocalTime.parse(value)
        dataStore.edit { it[Keys.digestFirstTime] = value }
    }
    suspend fun setDigestSecondTime(value: String) {
        java.time.LocalTime.parse(value)
        dataStore.edit { it[Keys.digestSecondTime] = value }
    }
    suspend fun setDigestConsentAccepted() =
        dataStore.edit { it[Keys.digestConsentAccepted] = true }
    suspend fun setDigestAllowListed(packageName: String, allowed: Boolean) =
        dataStore.edit { preferences ->
            val packages = preferences[Keys.digestAllowList].orEmpty().toMutableSet()
            if (allowed) packages.add(packageName) else packages.remove(packageName)
            preferences[Keys.digestAllowList] = packages
        }
    suspend fun setQuietHoursStart(value: String?) = updateNullableString(Keys.quietHoursStart, value)
    suspend fun setQuietHoursEnd(value: String?) = updateNullableString(Keys.quietHoursEnd, value)
    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[Keys.themeMode] = value.name }
    suspend fun setUseDynamicColor(value: Boolean) = dataStore.edit { it[Keys.useDynamicColor] = value }
    suspend fun setIconShape(value: IconShape) = dataStore.edit { it[Keys.iconShape] = value.name }
    suspend fun setFontPreset(value: FontPreset) = dataStore.edit { it[Keys.fontPreset] = value.name }
    suspend fun setLayoutDensity(value: LayoutDensityPreset) =
        dataStore.edit { it[Keys.layoutDensity] = value.name }
    suspend fun setAccentColorIndex(value: Int) {
        require(value in 0 until ACCENT_COLOR_COUNT)
        dataStore.edit { it[Keys.accentColorIndex] = value }
    }
    suspend fun setIconPackPackage(value: String?) = updateNullableString(Keys.iconPackPackage, value)
    suspend fun setLeftSwipeTarget(value: SwipeAppTarget?) =
        updateNullableString(Keys.leftSwipeTarget, value?.toPreferenceValue())
    suspend fun setRightSwipeTarget(value: SwipeAppTarget?) =
        updateNullableString(Keys.rightSwipeTarget, value?.toPreferenceValue())
    override suspend fun setOnboardingCompleted(value: Boolean) {
        dataStore.edit { it[Keys.onboardingCompleted] = value }
    }
    suspend fun setAssumedMinutesPerOpen(value: Int) =
        dataStore.edit { it[Keys.assumedMinutesPerOpen] = value }
    suspend fun setFocusScoreEnabled(value: Boolean) =
        dataStore.edit { it[Keys.focusScoreEnabled] = value }
    suspend fun setNotificationPermissionRequested(value: Boolean) =
        dataStore.edit { it[Keys.notificationPermissionRequested] = value }
    override suspend fun markNotificationPermissionRequested() {
        setNotificationPermissionRequested(true)
    }
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

    suspend fun exportBackupSettings(): Map<String, JsonElement> = backupAdapter.export()
    fun validateBackupSettings(settings: Map<String, JsonElement>) = backupAdapter.validate(settings)
    fun recognizedBackupSettings(settings: Map<String, JsonElement>) = backupAdapter.recognized(settings)
    suspend fun importBackupSettings(settings: Map<String, JsonElement>, replace: Boolean) =
        backupAdapter.import(settings, replace)
    suspend fun clearAllSettings() = backupAdapter.clear()

    private suspend fun updateNullableString(key: Preferences.Key<String>, value: String?) {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }

    private fun SwipeAppTarget.toPreferenceValue(): String =
        "$packageName|$className|$userSerial"

    private fun String.toSwipeAppTarget(): SwipeAppTarget? {
        val parts = split('|')
        if (parts.size != 3 || parts[0].isBlank() || parts[1].isBlank()) return null
        val serial = parts[2].toLongOrNull() ?: return null
        return SwipeAppTarget(parts[0], parts[1], serial)
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
        val weeklyCheckInDay = stringPreferencesKey("weekly_check_in_day")
        val weeklyCheckInTime = stringPreferencesKey("weekly_check_in_time")
        val weeklyCheckInEnabled = booleanPreferencesKey("weekly_check_in_enabled")
        val nudgesEnabled = booleanPreferencesKey("nudges_enabled")
        val quietHoursStart = stringPreferencesKey("quiet_hours_start")
        val quietHoursEnd = stringPreferencesKey("quiet_hours_end")
        val digestEnabled = booleanPreferencesKey("digest_enabled")
        val digestFirstTime = stringPreferencesKey("digest_first_time")
        val digestSecondTime = stringPreferencesKey("digest_second_time")
        val digestAllowList = stringSetPreferencesKey("digest_allow_list")
        val digestConsentAccepted = booleanPreferencesKey("digest_consent_accepted")
        val themeMode = stringPreferencesKey("theme_mode")
        val useDynamicColor = booleanPreferencesKey("use_dynamic_color")
        val iconShape = stringPreferencesKey("icon_shape")
        val fontPreset = stringPreferencesKey("font_preset")
        val layoutDensity = stringPreferencesKey("layout_density")
        val accentColorIndex = intPreferencesKey("accent_color_index")
        val iconPackPackage = stringPreferencesKey("icon_pack_package")
        val leftSwipeTarget = stringPreferencesKey("left_swipe_target")
        val rightSwipeTarget = stringPreferencesKey("right_swipe_target")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val assumedMinutesPerOpen = intPreferencesKey("assumed_minutes_per_open")
        val focusScoreEnabled = booleanPreferencesKey("focus_score_enabled")
        val notificationPermissionRequested = booleanPreferencesKey("notification_permission_requested")
        val focusModesSeeded = booleanPreferencesKey("focus_modes_seeded")
        val manualModeOverrideSet = booleanPreferencesKey("manual_mode_override_set")
        val manualModeId = stringPreferencesKey("manual_mode_id")
        val manualModeUntil = stringPreferencesKey("manual_mode_until")
    }

    private companion object {
        const val ACCENT_COLOR_COUNT = 8
        const val MAX_DAILY_LIMIT_MINUTES = 720
        const val NONE_MODE_ID = "__none__"
    }
}
