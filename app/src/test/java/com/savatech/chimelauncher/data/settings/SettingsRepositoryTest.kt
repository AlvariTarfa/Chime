package com.savatech.chimelauncher.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import com.savatech.chimelauncher.domain.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun settingsExposeDefaultsAndRoundTripValues() = runTest {
        val repository = repositoryFor(
            temporaryFolder.newFile("settings.preferences_pb"),
            backgroundScope,
        )

        assertEquals(DrawerMode.TEXT, repository.drawerMode.first())
        assertEquals(FrictionLevel.BALANCED, repository.frictionLevel.first())
        assertEquals(8, repository.baseDelaySeconds.first())
        assertEquals(emptyMap<AppCategory, Int>(), repository.categoryDailyLimits.first())
        assertEquals("07:30", repository.morningCheckInTime.first())
        assertEquals(false, repository.morningCheckInEnabled.first())
        assertEquals("21:00", repository.eveningCheckInTime.first())
        assertEquals(false, repository.eveningCheckInEnabled.first())
        assertNull(repository.quietHoursStart.first())
        assertNull(repository.quietHoursEnd.first())
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
        assertEquals(true, repository.useDynamicColor.first())
        assertEquals("circle", repository.iconShape.first())
        assertEquals("system", repository.fontPreset.first())
        assertNull(repository.iconPackPackage.first())
        assertEquals(false, repository.onboardingCompleted.first())
        assertEquals(5, repository.assumedMinutesPerOpen.first())

        repository.setDrawerMode(DrawerMode.GRID)
        repository.setFrictionLevel(FrictionLevel.STRICT)
        assertEquals(15, repository.baseDelaySeconds.first())
        repository.setBaseDelaySeconds(12)
        repository.setCategoryDailyLimit(AppCategory.DISTRACTING, 45)
        repository.setMorningCheckInTime("08:15")
        repository.setMorningCheckInEnabled(true)
        repository.setEveningCheckInTime("22:10")
        repository.setEveningCheckInEnabled(true)
        repository.setQuietHoursStart("22:30")
        repository.setQuietHoursEnd("06:45")
        repository.setThemeMode(ThemeMode.AMOLED)
        repository.setUseDynamicColor(false)
        repository.setIconShape("squircle")
        repository.setFontPreset("rounded")
        repository.setIconPackPackage("com.example.icons")
        repository.setOnboardingCompleted(true)
        repository.setAssumedMinutesPerOpen(8)

        assertEquals(DrawerMode.GRID, repository.drawerMode.first())
        assertEquals(FrictionLevel.STRICT, repository.frictionLevel.first())
        assertEquals(12, repository.baseDelaySeconds.first())
        assertEquals(
            mapOf(AppCategory.DISTRACTING to 45),
            repository.categoryDailyLimits.first(),
        )
        assertEquals("08:15", repository.morningCheckInTime.first())
        assertEquals(true, repository.morningCheckInEnabled.first())
        assertEquals("22:10", repository.eveningCheckInTime.first())
        assertEquals(true, repository.eveningCheckInEnabled.first())
        assertEquals("22:30", repository.quietHoursStart.first())
        assertEquals("06:45", repository.quietHoursEnd.first())
        assertEquals(ThemeMode.AMOLED, repository.themeMode.first())
        assertEquals(false, repository.useDynamicColor.first())
        assertEquals("squircle", repository.iconShape.first())
        assertEquals("rounded", repository.fontPreset.first())
        assertEquals("com.example.icons", repository.iconPackPackage.first())
        assertEquals(true, repository.onboardingCompleted.first())
        assertEquals(8, repository.assumedMinutesPerOpen.first())

        repository.setIconPackPackage(null)
        repository.setCategoryDailyLimit(AppCategory.DISTRACTING, null)
        assertNull(repository.iconPackPackage.first())
        assertEquals(emptyMap<AppCategory, Int>(), repository.categoryDailyLimits.first())
    }

    private fun repositoryFor(file: File, scope: CoroutineScope): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file },
        )
        return SettingsRepository(dataStore)
    }
}
