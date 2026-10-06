package com.savatech.chimelauncher.domain.checkin

import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

fun nextDailyOccurrence(now: LocalDateTime, time: LocalTime): LocalDateTime {
    val today = now.toLocalDate().atTime(time)
    return if (today.isAfter(now)) today else today.plusDays(1)
}

fun nextWeeklyOccurrence(
    now: LocalDateTime,
    dayOfWeek: DayOfWeek,
    time: LocalTime,
): LocalDateTime {
    val daysUntil = (dayOfWeek.value - now.dayOfWeek.value + 7) % 7
    val occurrence = now.toLocalDate().plusDays(daysUntil.toLong()).atTime(time)
    return if (occurrence.isAfter(now)) occurrence else occurrence.plusWeeks(1)
}

fun delayUntilOccurrence(now: LocalDateTime, occurrence: LocalDateTime, zoneId: ZoneId): Long =
    Duration.between(now.atZone(zoneId).toInstant(), occurrence.atZone(zoneId).toInstant())
        .toMillis()
        .coerceAtLeast(0L)

class OccurrenceClock(private val clock: Clock) {
    fun nextDaily(time: LocalTime): LocalDateTime = nextDailyOccurrence(LocalDateTime.now(clock), time)

    fun nextWeekly(dayOfWeek: DayOfWeek, time: LocalTime): LocalDateTime =
        nextWeeklyOccurrence(LocalDateTime.now(clock), dayOfWeek, time)

    fun delayUntil(occurrence: LocalDateTime): Long =
        delayUntilOccurrence(LocalDateTime.now(clock), occurrence, ZoneId.systemDefault())
}
