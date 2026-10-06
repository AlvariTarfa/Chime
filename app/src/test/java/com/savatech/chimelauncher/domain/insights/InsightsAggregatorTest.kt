package com.savatech.chimelauncher.domain.insights

import com.savatech.chimelauncher.domain.model.AppCategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsAggregatorTest {
    @Test
    fun aggregatesCategoryTotalsAndTreatsMissingConfigAsNeutral() {
        val day = InsightsAggregator.aggregateDay(
            input(
                usage = mapOf("work" to 60_000L, "social" to 120_000L, "unknown" to 30_000L),
                categories = mapOf(
                    "work" to AppCategory.PRODUCTIVE,
                    "social" to AppCategory.DISTRACTING,
                ),
            ),
        )

        assertEquals(60_000L, day.productiveMillis)
        assertEquals(30_000L, day.neutralMillis)
        assertEquals(120_000L, day.distractingMillis)
        assertEquals(210_000L, day.totalScreenMillis)
        assertEquals(AppCategory.NEUTRAL, day.topApps.last().category)
    }

    @Test
    fun goalLinkedTimeIncludesOnlyActiveLinkedGoals() {
        val day = InsightsAggregator.aggregateDay(
            input(
                usage = mapOf("active" to 10L, "paused" to 20L, "unlinked" to 30L),
                linkedGoals = mapOf(
                    "active" to "goal-active",
                    "paused" to "goal-paused",
                ),
                activeGoals = setOf("goal-active"),
            ),
        )

        assertEquals(10L, day.goalLinkedMillis)
    }

    @Test
    fun emptyDataIsZeroAndSingleDayRangeHasAveragesAndExtrema() {
        val empty = InsightsAggregator.aggregateDay(input(usage = emptyMap()))
        assertEquals(0L, empty.totalScreenMillis)
        assertEquals(emptyList<AppDayUsage>(), empty.topApps)

        val range = InsightsAggregator.aggregateRange(listOf(empty))
        assertEquals(1, range.days.size)
        assertEquals(0L, range.averageScreenMillis)
        assertEquals(empty, range.bestDay)
        assertEquals(empty, range.worstDay)
        assertNull(range.focusScore)
    }

    @Test
    fun focusScoreIsOptionalAndCombinesTheDocumentedShares() {
        val day = InsightsAggregator.aggregateDay(
            input(
                usage = mapOf("app" to 1L),
                goalCompletionRate = 0.8,
                intercepts = InterceptDayCounts(shown = 4, cancelled = 2),
            ),
        )

        assertNull(InsightsAggregator.aggregateRange(listOf(day)).focusScore)
        assertEquals(80, InsightsAggregator.aggregateRange(listOf(day), true).focusScore)
        assertTrue(day.withinLimits)
        assertFalse(
            InsightsAggregator.aggregateDay(
                input(
                    usage = mapOf("app" to 1L),
                    goalCompletionRate = 1.0,
                    appLimits = mapOf("app" to 0L),
                ),
            ).withinLimits,
        )
    }

    @Test
    fun categoryDefaultLimitIsAppliedPerAppLikeLaunchEnforcement() {
        val day = InsightsAggregator.aggregateDay(
            input(
                usage = mapOf("one" to 60L, "two" to 60L),
                appLimits = mapOf("one" to 100L, "two" to 100L),
            ),
        )

        assertTrue(day.withinLimits)
    }

    @Test
    fun correlationRequiresSevenDaysWithCompletionData() {
        val six = (0..5).map { day(it, completion = 1.0, distractionMinutes = 10) }
        val sevenButIncomplete = six + day(6, completion = null, distractionMinutes = 10)
        assertEquals(CorrelationTakeaway.NotEnoughData, correlationTakeaway(six))
        assertEquals(CorrelationTakeaway.NotEnoughData, correlationTakeaway(sevenButIncomplete))
    }

    @Test
    fun correlationExplainsLowerAndHigherDistractingUsage() {
        val lower = (0..6).map { index ->
            if (index < 3) day(index, completion = 1.0, distractionMinutes = 10)
            else day(index, completion = 0.5, distractionMinutes = 70)
        }
        assertEquals(CorrelationTakeaway.Comparison(60), correlationTakeaway(lower))

        val higher = (0..6).map { index ->
            if (index < 3) day(index, completion = 1.0, distractionMinutes = 70)
            else day(index, completion = 0.5, distractionMinutes = 10)
        }
        assertEquals(CorrelationTakeaway.Comparison(-60), correlationTakeaway(higher))
    }

    @Test
    fun chartMaximumHandlesZeroAndTinyValues() {
        assertEquals(1.0, niceChartMaximum(0.0), 0.0)
        assertEquals(0.0001, niceChartMaximum(0.0001), 0.0000000001)
        assertEquals(0.2, niceChartMaximum(0.11), 0.0000001)
        assertEquals(200.0, niceChartMaximum(120.0), 0.0)
    }

    private fun input(
        usage: Map<String, Long>,
        categories: Map<String, AppCategory> = emptyMap(),
        linkedGoals: Map<String, String?> = emptyMap(),
        activeGoals: Set<String> = emptySet(),
        goalCompletionRate: Double? = null,
        intercepts: InterceptDayCounts = InterceptDayCounts(0, 0),
        appLimits: Map<String, Long> = emptyMap(),
    ) = DayInsightInput(
        date = LocalDate.of(2026, 10, 6),
        appUsage = usage,
        labels = emptyMap(),
        categories = categories,
        linkedGoalIds = linkedGoals,
        activeGoalIds = activeGoals,
        pickups = null,
        firstPickup = null,
        longestSession = null,
        intercepts = intercepts,
        goalCompletionRate = goalCompletionRate,
        appLimitsMillis = appLimits,
    )

    private fun day(index: Int, completion: Double?, distractionMinutes: Int): DayInsight =
        InsightsAggregator.aggregateDay(
            input(
                usage = mapOf("distracting" to distractionMinutes * 60_000L),
                categories = mapOf("distracting" to AppCategory.DISTRACTING),
                goalCompletionRate = completion,
            ).copy(date = LocalDate.of(2026, 10, 1).plusDays(index.toLong())),
        )
}
