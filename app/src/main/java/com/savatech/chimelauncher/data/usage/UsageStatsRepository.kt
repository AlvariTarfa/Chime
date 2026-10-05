package com.savatech.chimelauncher.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.savatech.chimelauncher.core.di.IoDispatcher

@Singleton
class UsageStatsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val usagePermission: UsagePermission,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UsageRepository {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val cache = ConcurrentHashMap<LocalDate, CachedDay>()

    override suspend fun dailyAppUsage(date: LocalDate): Map<String, Long> =
        dayStats(date)?.foregroundMillisByPackage.orEmpty()

    override suspend fun pickups(date: LocalDate): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return dayStats(date)?.pickups
    }

    override suspend fun firstPickup(date: LocalDate): LocalTime? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val pickupMillis = dayStats(date)?.firstPickupMillis ?: return null
        return Instant.ofEpochMilli(pickupMillis).atZone(ZoneId.systemDefault()).toLocalTime()
    }

    override suspend fun longestSession(date: LocalDate): Duration? =
        dayStats(date)?.longestSessionMillis?.let(Duration::ofMillis)

    private suspend fun dayStats(date: LocalDate): PairedUsageData? = withContext(ioDispatcher) {
        usagePermission.refresh()
        if (!usagePermission.hasPermission.value) return@withContext null

        val zone = ZoneId.systemDefault()
        val now = clock.instant()
        val today = LocalDate.now(clock.withZone(zone))
        val isPastDay = date.isBefore(today)
        val elapsedRealtime = SystemClock.elapsedRealtime()
        cache[date]?.let { cached ->
            val valid = if (isPastDay) {
                cached.isPastDay
            } else {
                !cached.isPastDay && elapsedRealtime - cached.cachedAtElapsedMillis < TODAY_CACHE_MILLIS
            }
            if (valid) return@withContext cached.data
        }

        val start = date.atStartOfDay(zone).toInstant()
        val nextDayStart = date.plusDays(1).atStartOfDay(zone).toInstant()
        val end = minOf(nextDayStart, now)
        val data = if (end <= start) {
            PairedUsageData(emptyMap(), 0, null, null)
        } else {
            pairUsageEvents(
                events = readEvents(start.toEpochMilli(), end.toEpochMilli()),
                windowStartMillis = start.toEpochMilli(),
                windowEndMillis = end.toEpochMilli(),
                ignoredPackages = launcherPackagesToIgnore(),
            )
        }
        if (isPastDay || date == today) {
            cache[date] = CachedDay(data, elapsedRealtime, isPastDay)
        }
        data
    }

    private fun readEvents(startMillis: Long, endMillis: Long): List<UsageEventRecord> {
        val usageEvents = usageStatsManager.queryEvents(startMillis, endMillis)
        val event = UsageEvents.Event()
        val records = ArrayList<UsageEventRecord>()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val type = eventType(event.eventType) ?: continue
            records += UsageEventRecord(
                timestamp = event.timeStamp,
                packageName = event.packageName.orEmpty(),
                className = event.className,
                type = type,
            )
        }
        return records
    }

    @Suppress("DEPRECATION")
    private fun eventType(eventType: Int): UsageEventType? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.ACTIVITY_RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.ACTIVITY_PAUSED
                else -> eventType(eventType, includePickups = true)
            }
        }
        return when (eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventType.MOVE_TO_FOREGROUND
            UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageEventType.MOVE_TO_BACKGROUND
            else -> eventType(eventType, includePickups = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        }
    }

    private fun eventType(eventType: Int, includePickups: Boolean): UsageEventType? = when {
        includePickups && eventType == UsageEvents.Event.SCREEN_INTERACTIVE ->
            UsageEventType.SCREEN_INTERACTIVE

        includePickups && eventType == UsageEvents.Event.KEYGUARD_HIDDEN ->
            UsageEventType.KEYGUARD_HIDDEN

        else -> null
    }

    private fun launcherPackagesToIgnore(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homeActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                homeIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)
        }
        return buildSet {
            add(context.packageName)
            homeActivities.forEach { resolveInfo ->
                val appInfo = resolveInfo.activityInfo?.applicationInfo
                if (appInfo != null && appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) {
                    add(appInfo.packageName)
                }
            }
        }
    }

    private data class CachedDay(
        val data: PairedUsageData,
        val cachedAtElapsedMillis: Long,
        val isPastDay: Boolean,
    )

    private companion object {
        const val TODAY_CACHE_MILLIS = 30_000L
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UsageModule {
    @Binds
    @Singleton
    abstract fun bindUsageRepository(repository: UsageStatsRepository): UsageRepository
}
