package com.savatech.chimelauncher.data.usage

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

interface UsageRepository {
    suspend fun dailyAppUsage(date: LocalDate): Map<String, Long>

    suspend fun pickups(date: LocalDate): Int?

    suspend fun firstPickup(date: LocalDate): LocalTime?

    suspend fun longestSession(date: LocalDate): Duration?
}
