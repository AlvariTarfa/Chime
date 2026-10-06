package com.savatech.chimelauncher.domain.insights

import com.savatech.chimelauncher.domain.model.AppCategory
import java.time.LocalDate
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

data class AppDayUsage(
    val packageName: String,
    val label: String,
    val totalScreenMillis: Long,
    val category: AppCategory,
)

data class InterceptDayCounts(val shown: Int, val cancelled: Int)

data class DayInsight(
    val date: LocalDate,
    val totalScreenMillis: Long,
    val pickups: Int?,
    val firstPickup: java.time.LocalTime?,
    val longestSession: java.time.Duration?,
    val topApps: List<AppDayUsage>,
    val productiveMillis: Long,
    val neutralMillis: Long,
    val distractingMillis: Long,
    val goalLinkedMillis: Long,
    val interceptsShown: Int,
    val interceptsCancelled: Int,
    val goalCompletionRate: Double?,
    val withinLimits: Boolean,
)

data class RangeInsight(
    val days: List<DayInsight>,
    val averageScreenMillis: Long,
    val averageProductiveMillis: Long,
    val averageNeutralMillis: Long,
    val averageDistractingMillis: Long,
    val bestDay: DayInsight?,
    val worstDay: DayInsight?,
    val focusScore: Int?,
)

data class DayInsightInput(
    val date: LocalDate,
    val appUsage: Map<String, Long>,
    val labels: Map<String, String>,
    val categories: Map<String, AppCategory>,
    val linkedGoalIds: Map<String, String?>,
    val activeGoalIds: Set<String>,
    val pickups: Int?,
    val firstPickup: java.time.LocalTime?,
    val longestSession: java.time.Duration?,
    val intercepts: InterceptDayCounts,
    val goalCompletionRate: Double?,
    val appLimitsMillis: Map<String, Long>,
)

object InsightsAggregator {
    fun aggregateDay(input: DayInsightInput): DayInsight {
        val apps = input.appUsage
            .filterValues { it > 0L }
            .map { (packageName, millis) ->
                AppDayUsage(
                    packageName = packageName,
                    label = input.labels[packageName] ?: packageName,
                    totalScreenMillis = millis,
                    category = input.categories[packageName] ?: AppCategory.NEUTRAL,
                )
            }
            .sortedWith(compareByDescending<AppDayUsage> { it.totalScreenMillis }.thenBy { it.label })
        val productive = apps.filter { it.category == AppCategory.PRODUCTIVE }.sumOf { it.totalScreenMillis }
        val neutral = apps.filter { it.category == AppCategory.NEUTRAL }.sumOf { it.totalScreenMillis }
        val distracting = apps.filter { it.category == AppCategory.DISTRACTING }.sumOf { it.totalScreenMillis }
        val goalLinked = apps.filter { app ->
            input.linkedGoalIds[app.packageName]?.let(input.activeGoalIds::contains) == true
        }.sumOf(AppDayUsage::totalScreenMillis)
        val withinAppLimits = input.appLimitsMillis.all { (packageName, limit) ->
            (input.appUsage[packageName] ?: 0L) <= limit
        }
        return DayInsight(
            date = input.date,
            totalScreenMillis = apps.sumOf(AppDayUsage::totalScreenMillis),
            pickups = input.pickups,
            firstPickup = input.firstPickup,
            longestSession = input.longestSession,
            topApps = apps.take(5),
            productiveMillis = productive,
            neutralMillis = neutral,
            distractingMillis = distracting,
            goalLinkedMillis = goalLinked,
            interceptsShown = input.intercepts.shown,
            interceptsCancelled = input.intercepts.cancelled,
            goalCompletionRate = input.goalCompletionRate?.coerceIn(0.0, 1.0),
            withinLimits = withinAppLimits,
        )
    }

    fun aggregateRange(days: List<DayInsight>, includeFocusScore: Boolean = false): RangeInsight {
        val sorted = days.sortedBy(DayInsight::date)
        fun average(selector: (DayInsight) -> Long): Long =
            if (sorted.isEmpty()) 0L else sorted.sumOf(selector) / sorted.size

        val focus = if (!includeFocusScore || sorted.isEmpty()) {
            null
        } else {
            val goalCompletion = sorted.mapNotNull(DayInsight::goalCompletionRate)
                .takeIf { it.isNotEmpty() }?.average() ?: 0.0
            val underLimitsShare = sorted.count(DayInsight::withinLimits).toDouble() / sorted.size
            val shown = sorted.sumOf(DayInsight::interceptsShown)
            val cancelRate = if (shown == 0) {
                1.0
            } else {
                sorted.sumOf(DayInsight::interceptsCancelled).toDouble() / shown
            }.coerceIn(0.0, 1.0)
            ((goalCompletion * 0.5 + underLimitsShare * 0.3 + cancelRate * 0.2) * 100)
                .toInt()
                .coerceIn(0, 100)
        }
        return RangeInsight(
            days = sorted,
            averageScreenMillis = average(DayInsight::totalScreenMillis),
            averageProductiveMillis = average(DayInsight::productiveMillis),
            averageNeutralMillis = average(DayInsight::neutralMillis),
            averageDistractingMillis = average(DayInsight::distractingMillis),
            bestDay = sorted.minByOrNull(DayInsight::totalScreenMillis),
            worstDay = sorted.maxByOrNull(DayInsight::totalScreenMillis),
            focusScore = focus,
        )
    }
}

fun correlationTakeaway(days: List<DayInsight>): CorrelationTakeaway {
    if (days.size < MIN_CORRELATION_DAYS ||
        days.count { it.goalCompletionRate != null } < MIN_CORRELATION_DAYS
    ) return CorrelationTakeaway.NotEnoughData
    val complete = days.filter { it.goalCompletionRate == 1.0 }
    val other = days.filter { it.goalCompletionRate != null && it.goalCompletionRate < 1.0 }
    if (complete.isEmpty() || other.isEmpty()) return CorrelationTakeaway.NotEnoughData
    val differenceMinutes = (
        (other.map { it.distractingMillis }.average() -
            complete.map { it.distractingMillis }.average()) / MILLIS_PER_MINUTE
        ).roundToInt()
    return CorrelationTakeaway.Comparison(differenceMinutes)
}

sealed interface CorrelationTakeaway {
    data object NotEnoughData : CorrelationTakeaway
    data class Comparison(val minutesLessOnCompleteDays: Int) : CorrelationTakeaway
}

fun niceChartMaximum(value: Double): Double {
    if (!value.isFinite() || value <= 0.0) return 1.0
    val magnitude = 10.0.pow(kotlin.math.floor(log10(value)))
    val normalized = value / magnitude
    val nice = when {
        normalized <= 1.0 -> 1.0
        normalized <= 2.0 -> 2.0
        normalized <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * magnitude
}

private const val MIN_CORRELATION_DAYS = 7
private const val MILLIS_PER_MINUTE = 60_000L
