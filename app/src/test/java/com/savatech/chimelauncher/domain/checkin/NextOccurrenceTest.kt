package com.savatech.chimelauncher.domain.checkin

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextOccurrenceTest {
    @Test
    fun dailyOccurrenceBeforeAfterAndExactlyAtConfiguredTime() {
        val time = LocalTime.of(8, 30)
        assertEquals(
            LocalDateTime.parse("2026-04-06T08:30"),
            nextDailyOccurrence(LocalDateTime.parse("2026-04-06T08:29"), time),
        )
        assertEquals(
            LocalDateTime.parse("2026-04-07T08:30"),
            nextDailyOccurrence(LocalDateTime.parse("2026-04-06T08:31"), time),
        )
        assertEquals(
            LocalDateTime.parse("2026-04-07T08:30"),
            nextDailyOccurrence(LocalDateTime.parse("2026-04-06T08:30"), time),
        )
    }

    @Test
    fun springForwardKeepsLocalOccurrenceAndUsesZoneForDelay() {
        val zone = ZoneId.of("America/New_York")
        val now = LocalDateTime.parse("2024-03-10T01:45")
        val occurrence = nextDailyOccurrence(now, LocalTime.of(2, 30))
        assertEquals(LocalDateTime.parse("2024-03-10T02:30"), occurrence)
        assertEquals(
            45 * 60_000L,
            delayUntilOccurrence(now, occurrence, zone),
        )
    }

    @Test
    fun fallBackUsesTheEarlierOffsetForAnAmbiguousLocalOccurrence() {
        val zone = ZoneId.of("America/New_York")
        val now = LocalDateTime.parse("2024-11-03T00:30")
        val occurrence = nextDailyOccurrence(now, LocalTime.of(1, 30))
        assertEquals(LocalDateTime.parse("2024-11-03T01:30"), occurrence)
        assertEquals(60 * 60_000L, delayUntilOccurrence(now, occurrence, zone))
    }

    @Test
    fun weeklyOccurrenceRollsForwardAndMovesToNextWeekWhenTimePassed() {
        assertEquals(
            LocalDateTime.parse("2026-04-06T09:00"),
            nextWeeklyOccurrence(
                LocalDateTime.parse("2026-04-03T18:00"),
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
            ),
        )
        assertEquals(
            LocalDateTime.parse("2026-04-13T09:00"),
            nextWeeklyOccurrence(
                LocalDateTime.parse("2026-04-06T09:00"),
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
            ),
        )
        assertTrue(
            nextWeeklyOccurrence(
                LocalDateTime.parse("2026-04-06T08:59"),
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
            ).isAfter(LocalDateTime.parse("2026-04-06T08:59")),
        )
    }
}
