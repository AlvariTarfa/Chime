package com.savatech.chimelauncher.data.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageEventPairingTest {
    @Test
    fun pairsSimpleForegroundAndBackground() {
        val result = pairUsageEvents(
            events = listOf(
                event(100, "app", UsageEventType.ACTIVITY_RESUMED),
                event(400, "app", UsageEventType.ACTIVITY_PAUSED),
            ),
            windowStartMillis = 0,
            windowEndMillis = 1_000,
        )

        assertEquals(mapOf("app" to 300L), result.foregroundMillisByPackage)
        assertEquals(300L, result.longestSessionMillis)
    }

    @Test
    fun pairsTwoAppsInterleaved() {
        val result = pairUsageEvents(
            events = listOf(
                event(100, "first", UsageEventType.ACTIVITY_RESUMED),
                event(200, "second", UsageEventType.ACTIVITY_RESUMED),
                event(500, "first", UsageEventType.ACTIVITY_PAUSED),
                event(700, "second", UsageEventType.ACTIVITY_PAUSED),
            ),
            windowStartMillis = 0,
            windowEndMillis = 1_000,
        )

        assertEquals(mapOf("first" to 400L, "second" to 500L), result.foregroundMillisByPackage)
    }

    @Test
    fun pauseAfterWindowStartCountsForegroundFromWindowStart() {
        val result = pairUsageEvents(
            events = listOf(event(250, "app", UsageEventType.ACTIVITY_PAUSED)),
            windowStartMillis = 100,
            windowEndMillis = 1_000,
        )

        assertEquals(mapOf("app" to 150L), result.foregroundMillisByPackage)
    }

    @Test
    fun countsOpenSessionThroughWindowEnd() {
        val result = pairUsageEvents(
            events = listOf(event(250, "app", UsageEventType.ACTIVITY_RESUMED)),
            windowStartMillis = 100,
            windowEndMillis = 900,
        )

        assertEquals(mapOf("app" to 650L), result.foregroundMillisByPackage)
    }

    @Test
    fun ignoresRepeatedOrphanPausesAfterInitialWindowStartSession() {
        val result = pairUsageEvents(
            events = listOf(
                event(200, "app", UsageEventType.ACTIVITY_PAUSED),
                event(300, "app", UsageEventType.ACTIVITY_PAUSED),
            ),
            windowStartMillis = 100,
            windowEndMillis = 1_000,
        )

        assertEquals(mapOf("app" to 100L), result.foregroundMillisByPackage)
    }

    @Test
    fun quickSwitchesBetweenComponentsAccumulateByPackage() {
        val result = pairUsageEvents(
            events = listOf(
                event(100, "app", UsageEventType.ACTIVITY_RESUMED, "One"),
                event(200, "app", UsageEventType.ACTIVITY_PAUSED, "One"),
                event(200, "app", UsageEventType.ACTIVITY_RESUMED, "Two"),
                event(350, "app", UsageEventType.ACTIVITY_PAUSED, "Two"),
            ),
            windowStartMillis = 0,
            windowEndMillis = 1_000,
        )

        assertEquals(mapOf("app" to 250L), result.foregroundMillisByPackage)
    }

    @Test
    fun duplicateResumeDoesNotRestartSession() {
        val result = pairUsageEvents(
            events = listOf(
                event(100, "app", UsageEventType.ACTIVITY_RESUMED),
                event(200, "app", UsageEventType.ACTIVITY_RESUMED),
                event(500, "app", UsageEventType.ACTIVITY_PAUSED),
            ),
            windowStartMillis = 0,
            windowEndMillis = 1_000,
        )

        assertEquals(mapOf("app" to 400L), result.foregroundMillisByPackage)
    }

    @Test
    fun emptyEventsReturnEmptyStats() {
        val result = pairUsageEvents(emptyList(), 100, 1_000)

        assertEquals(emptyMap<String, Long>(), result.foregroundMillisByPackage)
        assertEquals(0, result.pickups)
        assertNull(result.firstPickupMillis)
        assertNull(result.longestSessionMillis)
    }

    @Test
    fun countsPickupEventsAndSelectsFirst() {
        val result = pairUsageEvents(
            events = listOf(
                event(300, "android", UsageEventType.KEYGUARD_HIDDEN),
                event(150, "android", UsageEventType.SCREEN_INTERACTIVE),
                event(400, "android", UsageEventType.OTHER),
            ),
            windowStartMillis = 100,
            windowEndMillis = 500,
        )

        assertEquals(2, result.pickups)
        assertEquals(150L, result.firstPickupMillis)
    }

    @Test
    fun selectsLongestSingleSessionAndExcludesLauncherTotals() {
        val result = pairUsageEvents(
            events = listOf(
                event(100, "app-one", UsageEventType.ACTIVITY_RESUMED),
                event(250, "app-one", UsageEventType.ACTIVITY_PAUSED),
                event(200, "app-two", UsageEventType.ACTIVITY_RESUMED),
                event(600, "app-two", UsageEventType.ACTIVITY_PAUSED),
                event(100, "launcher", UsageEventType.ACTIVITY_RESUMED),
                event(900, "launcher", UsageEventType.ACTIVITY_PAUSED),
            ),
            windowStartMillis = 0,
            windowEndMillis = 1_000,
            ignoredPackages = setOf("launcher"),
        )

        assertEquals(400L, result.longestSessionMillis)
        assertEquals(mapOf("app-one" to 150L, "app-two" to 400L), result.foregroundMillisByPackage)
    }

    private fun event(
        timestamp: Long,
        packageName: String,
        type: UsageEventType,
        className: String? = "Activity",
    ) = UsageEventRecord(timestamp, packageName, className, type)
}
