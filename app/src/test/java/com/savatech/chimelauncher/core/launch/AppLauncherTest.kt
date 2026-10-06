package com.savatech.chimelauncher.core.launch

import com.savatech.chimelauncher.data.apps.AppConfigSource
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.data.db.entities.AppGrant
import com.savatech.chimelauncher.data.db.entities.InterceptEvent
import com.savatech.chimelauncher.data.intercept.InterceptRepository
import com.savatech.chimelauncher.data.settings.FrictionLevel
import com.savatech.chimelauncher.data.settings.InterceptSettings
import com.savatech.chimelauncher.data.usage.UsageAccess
import com.savatech.chimelauncher.data.usage.UsageRepository
import com.savatech.chimelauncher.domain.intercept.InterceptPolicy
import com.savatech.chimelauncher.domain.intercept.PauseReason
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.domain.model.InterceptOutcome
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLauncherTest {
    @Test
    fun distractingAppRequiresPause() = runBlocking {
        val appRepository = FakeAppRepository()
        val result = launcher(AppCategory.DISTRACTING, false, appRepository).requestLaunch(TEST_APP)

        assertTrue(result is LaunchResult.NeedsPause)
        assertEquals(8, (result as LaunchResult.NeedsPause).args.delaySeconds)
        assertFalse(appRepository.launched)
    }

    @Test
    fun activeGrantAllowsDistractingApp() = runBlocking {
        val appRepository = FakeAppRepository()
        val result = launcher(AppCategory.DISTRACTING, true, appRepository).requestLaunch(TEST_APP)

        assertEquals(LaunchResult.Started, result)
        assertTrue(appRepository.launched)
    }

    @Test
    fun neutralAppLaunchesWithoutPause() = runBlocking {
        val appRepository = FakeAppRepository()
        val result = launcher(AppCategory.NEUTRAL, false, appRepository).requestLaunch(TEST_APP)

        assertEquals(LaunchResult.Started, result)
        assertTrue(appRepository.launched)
    }

    @Test
    fun reachedAppLimitPausesEvenWhenGrantIsActive() = runBlocking {
        val result = launcher(
            AppCategory.NEUTRAL,
            hasGrant = true,
            appRepository = FakeAppRepository(),
            appLimit = 2,
            usedMillis = 120_000L,
        ).requestLaunch(TEST_APP)

        assertTrue(result is LaunchResult.NeedsPause)
        val args = (result as LaunchResult.NeedsPause).args
        assertEquals(PauseReason.LIMIT_REACHED, args.reason)
        assertEquals(16, args.delaySeconds)
        assertEquals(2, args.limitMinutes)
        assertEquals(120_000L, args.usedMillis)
    }

    @Test
    fun missingUsagePermissionDisablesLimitEnforcement() = runBlocking {
        val appRepository = FakeAppRepository()
        val result = launcher(
            AppCategory.NEUTRAL,
            hasGrant = false,
            appRepository = appRepository,
            appLimit = 1,
            usedMillis = 120_000L,
            usagePermissionGranted = false,
        ).requestLaunch(TEST_APP)

        assertEquals(LaunchResult.Started, result)
        assertTrue(appRepository.launched)
    }

    @Test
    fun categoryDefaultLimitIsUsedWhenAppHasNoLimit() = runBlocking {
        val result = launcher(
            AppCategory.DISTRACTING,
            hasGrant = false,
            appRepository = FakeAppRepository(),
            categoryLimit = 1,
            usedMillis = 60_000L,
        ).requestLaunch(TEST_APP)

        assertTrue(result is LaunchResult.NeedsPause)
        assertEquals(PauseReason.LIMIT_REACHED, (result as LaunchResult.NeedsPause).args.reason)
    }

    @Test
    fun dailyLimitWarningIsShownOncePerAppAndDateAtEightyPercent() = runBlocking {
        val launcher = launcher(
            AppCategory.NEUTRAL,
            hasGrant = false,
            appRepository = FakeAppRepository(),
            appLimit = 10,
            usedMillis = 8 * 60_000L,
        )

        assertEquals(listOf(TEST_APP.label), launcher.dailyLimitWarnings())
        assertTrue(launcher.dailyLimitWarnings().isEmpty())
    }

    private fun launcher(
        category: AppCategory,
        hasGrant: Boolean,
        appRepository: FakeAppRepository,
        appLimit: Int? = null,
        categoryLimit: Int? = null,
        usedMillis: Long = 0L,
        usagePermissionGranted: Boolean = true,
    ) = AppLauncher(
        appRepository = appRepository,
        appConfigSource = object : AppConfigSource {
            private val config = AppConfig(
                packageName = TEST_APP.packageName,
                category = category.name,
                pinned = false,
                pinOrder = 0,
                hidden = false,
                dailyLimitMin = appLimit,
                linkedGoalId = null,
            )
            override suspend fun getConfig(packageName: String): AppConfig = config
            override suspend fun getAllConfigs(): Map<String, AppConfig> =
                mapOf(TEST_APP.packageName to config)
        },
        interceptRepository = object : InterceptRepository {
            override suspend fun hasActiveGrant(packageName: String) = hasGrant
            override suspend fun openedTodayCount(packageName: String) = 0
            override suspend fun logEvent(
                packageName: String,
                outcome: InterceptOutcome,
                grantedMinutes: Int?,
                reason: String?,
            ) = Unit
            override suspend fun createGrant(packageName: String, minutes: Int) =
                AppGrant(packageName, 0)
            override suspend fun eventsBetween(
                startInclusive: Long,
                endExclusive: Long,
            ): List<InterceptEvent> = emptyList()
        },
        settings = object : InterceptSettings {
            override val frictionLevel: Flow<FrictionLevel> = MutableStateFlow(FrictionLevel.BALANCED)
            override val baseDelaySeconds: Flow<Int> = MutableStateFlow(8)
            override val categoryDailyLimits: Flow<Map<AppCategory, Int>> =
                MutableStateFlow(if (categoryLimit == null) emptyMap() else mapOf(category to categoryLimit))
        },
        policy = InterceptPolicy(),
        usageRepository = object : UsageRepository {
            override suspend fun dailyAppUsage(date: LocalDate) = mapOf(TEST_APP.packageName to usedMillis)
            override suspend fun pickups(date: LocalDate): Int? = null
            override suspend fun firstPickup(date: LocalDate): LocalTime? = null
            override suspend fun longestSession(date: LocalDate): Duration? = null
        },
        usagePermission = UsageAccess { usagePermissionGranted },
        clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC),
    )

    private class FakeAppRepository : AppRepository {
        override val apps = MutableStateFlow(listOf(TEST_APP))
        override val isLoaded = MutableStateFlow(true)
        var launched = false
        override suspend fun launch(app: AppInfo): Result<Unit> {
            launched = true
            return Result.success(Unit)
        }
    }

    private companion object {
        val TEST_APP = AppInfo("Example", "com.example.app", "com.example.app.MainActivity", 0, false)
    }
}
