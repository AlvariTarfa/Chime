package com.savatech.chimelauncher.domain.streak

import com.savatech.chimelauncher.domain.model.Recurrence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreakCalculatorTest {
    @Test
    fun dailyTaskCompletedLastThreeDaysIncludingToday_hasCurrentStreakThree() {
        val today = LocalDate.of(2024, 5, 10)
        val result = compute(dailyTask(today.minusDays(2)), dates(today.minusDays(2), 3), today)

        assertEquals(3, result.currentStreak)
    }

    @Test
    fun todayPending_preservesYesterdayStreak() {
        val today = LocalDate.of(2024, 5, 10)
        val result = compute(dailyTask(today.minusDays(2)), dates(today.minusDays(2), 2), today)

        assertEquals(2, result.currentStreak)
    }

    @Test
    fun missedYesterdayWithoutCredits_resetsStreak() {
        val today = LocalDate.of(2024, 5, 10)
        val result = compute(dailyTask(today.minusDays(4)), dates(today.minusDays(4), 3), today)

        assertEquals(0, result.currentStreak)
        assertEquals(3, result.longestStreak)
    }

    @Test
    fun sevenConsecutiveCompletions_awardsOneFreezeCredit() {
        val today = LocalDate.of(2024, 5, 10)
        val result = compute(dailyTask(today.minusDays(6)), dates(today.minusDays(6), 7), today)

        assertEquals(7, result.currentStreak)
        assertEquals(1, result.freezeCredits)
    }

    @Test
    fun freezeCreditPreservesStreakWithoutIncrementingMissedDay() {
        val start = LocalDate.of(2024, 5, 1)
        val afterMiss = compute(dailyTask(start), dates(start, 7), start.plusDays(8))

        assertEquals(7, afterMiss.currentStreak)
        assertEquals(0, afterMiss.freezeCredits)

        val afterNextCompletion = compute(
            dailyTask(start),
            dates(start, 7) + start.plusDays(8),
            start.plusDays(8),
        )
        assertEquals(8, afterNextCompletion.currentStreak)
    }

    @Test
    fun freezeCreditsAreCappedAtTwo() {
        val start = LocalDate.of(2024, 5, 1)

        assertEquals(1, compute(dailyTask(start), dates(start, 7), start.plusDays(6)).freezeCredits)
        assertEquals(2, compute(dailyTask(start), dates(start, 14), start.plusDays(13)).freezeCredits)
        assertEquals(2, compute(dailyTask(start), dates(start, 21), start.plusDays(20)).freezeCredits)
    }

    @Test
    fun mondayWednesdayFridayCompletions_preserveStreakAcrossSkippedDays() {
        val monday = LocalDate.of(2024, 4, 1)
        val daysMask = MONDAY_MASK or WEDNESDAY_MASK or FRIDAY_MASK
        val result = compute(
            TaskSpec(Recurrence.DAYS_OF_WEEK, daysMask, monday),
            setOf(monday, monday.plusDays(2), monday.plusDays(4)),
            monday.plusDays(4),
        )

        assertEquals(3, result.currentStreak)
    }

    @Test
    fun completionOnNonScheduledDay_isIgnored() {
        val monday = LocalDate.of(2024, 4, 1)
        val tuesday = monday.plusDays(1)
        val result = compute(
            TaskSpec(Recurrence.DAYS_OF_WEEK, MONDAY_MASK, monday),
            setOf(tuesday),
            tuesday,
        )

        assertEquals(0, result.currentStreak)
        assertNull(result.lastCompletedDate)
    }

    @Test
    fun completionsBeforeCreatedOn_areIgnored() {
        val createdOn = LocalDate.of(2024, 1, 3)
        val result = compute(
            dailyTask(createdOn),
            setOf(createdOn.minusDays(2), createdOn.minusDays(1), createdOn),
            createdOn,
        )

        assertEquals(1, result.currentStreak)
        assertEquals(createdOn, result.lastCompletedDate)
    }

    @Test
    fun emptyHistory_hasZeroStreakAndNoLastCompletedDate() {
        val today = LocalDate.of(2024, 5, 10)
        val result = compute(dailyTask(today.minusDays(3)), emptySet(), today)

        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
        assertEquals(0, result.freezeCredits)
        assertNull(result.lastCompletedDate)
    }

    @Test
    fun todayScheduledButIncomplete_doesNotBreakStreak() {
        val today = LocalDate.of(2024, 5, 10)
        val result = compute(dailyTask(today.minusDays(2)), dates(today.minusDays(2), 2), today)

        assertEquals(2, result.currentStreak)
    }

    @Test
    fun monthAndYearBoundaries_areIncludedInChronologicalStreak() {
        val start = LocalDate.of(2024, 12, 30)
        val result = compute(dailyTask(start), dates(start, 4), LocalDate.of(2025, 1, 2))

        assertEquals(4, result.currentStreak)
        assertEquals(LocalDate.of(2025, 1, 2), result.lastCompletedDate)
    }

    private fun dailyTask(createdOn: LocalDate) =
        TaskSpec(Recurrence.DAILY, daysMask = 0, createdOn = createdOn)

    private fun dates(start: LocalDate, count: Int): Set<LocalDate> =
        (0 until count).map { start.plusDays(it.toLong()) }.toSet()

    private companion object {
        const val MONDAY_MASK = 1 shl 0
        const val WEDNESDAY_MASK = 1 shl 2
        const val FRIDAY_MASK = 1 shl 4
    }
}
