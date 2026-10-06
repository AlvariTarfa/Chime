package com.savatech.chimelauncher.domain.insights

import com.savatech.chimelauncher.data.apps.AppConfigSource
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.data.intercept.InterceptRepository
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.data.usage.UsageRepository
import com.savatech.chimelauncher.domain.limits.effectiveLimit
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.domain.model.GoalStatus
import com.savatech.chimelauncher.domain.model.InterceptOutcome
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskModel
import dagger.hilt.android.scopes.ViewModelScoped
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@ViewModelScoped
class GetInsightsUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val appConfigSource: AppConfigSource,
    private val appRepository: AppRepository,
    private val goalRepository: GoalRepository,
    private val interceptRepository: InterceptRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        dayCount: Int,
        includeFocusScore: Boolean,
    ): RangeInsight {
        require(dayCount == 7 || dayCount == 30)
        val today = LocalDate.now(clock)
        val dates = (dayCount - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val configs = appConfigSource.getAllConfigs()
        val labels = appLabels()
        val activeGoals = goalRepository.observeGoals(GoalStatus.ACTIVE).first()
        val activeGoalIds = activeGoals.mapTo(mutableSetOf()) { it.id }
        val categoryLimitsMinutes = settingsRepository.categoryDailyLimits.first()
        val days = dates.map { date ->
            val appUsage = usageRepository.dailyAppUsage(date)
            val priorities = goalRepository.observeDailyPriorities(date).first()
                .filter { it.goalId in activeGoalIds }
            val completion = completionRate(date, priorities.map { it.goalId }.distinct())
            val startMillis = date.atStartOfDay(clock.zone).toInstant().toEpochMilli()
            val endMillis = date.plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()
            val events = interceptRepository.eventsBetween(startMillis, endMillis)
            val shownEvents = events.count {
                InterceptOutcome.fromStorage(it.outcome) == InterceptOutcome.SHOWN
            }
            val completedInterceptions = events.count {
                InterceptOutcome.fromStorage(it.outcome) != InterceptOutcome.SHOWN
            }
            val shown = maxOf(shownEvents, completedInterceptions)
            val cancelled = events.count {
                InterceptOutcome.fromStorage(it.outcome) == InterceptOutcome.CANCELLED
            }
            val appLimits = (appUsage.keys + configs.keys + labels.keys).mapNotNull { packageName ->
                val config = configs[packageName]
                val category = config?.category?.let(AppCategory::fromStorage) ?: AppCategory.NEUTRAL
                effectiveLimit(config?.dailyLimitMin, categoryLimitsMinutes[category])
                    ?.let { packageName to it * MILLIS_PER_MINUTE }
            }.toMap()
            InsightsAggregator.aggregateDay(
                DayInsightInput(
                    date = date,
                    appUsage = appUsage,
                    labels = labels,
                    categories = configs.mapValues { (_, config) ->
                        AppCategory.fromStorage(config.category)
                    },
                    linkedGoalIds = configs.mapValues { (_, config) -> config.linkedGoalId },
                    activeGoalIds = activeGoalIds,
                    pickups = usageRepository.pickups(date),
                    firstPickup = usageRepository.firstPickup(date),
                    longestSession = usageRepository.longestSession(date),
                    intercepts = InterceptDayCounts(shown, cancelled),
                    goalCompletionRate = completion,
                    appLimitsMillis = appLimits,
                ),
            )
        }
        return InsightsAggregator.aggregateRange(days, includeFocusScore)
    }

    suspend fun csvRows(dayCount: Int): List<InsightsCsvRow> {
        require(dayCount == 7 || dayCount == 30)
        val today = LocalDate.now(clock)
        val configs = appConfigSource.getAllConfigs()
        val labels = appLabels()
        return (dayCount - 1 downTo 0).flatMap { offset ->
            val date = today.minusDays(offset.toLong())
            usageRepository.dailyAppUsage(date).map { (packageName, millis) ->
                val config = configs[packageName]
                InsightsCsvRow(
                    date = date,
                    packageName = packageName,
                    appLabel = labels[packageName] ?: packageName,
                    minutes = (millis + MILLIS_PER_MINUTE / 2) / MILLIS_PER_MINUTE,
                    category = config?.category?.let(AppCategory::fromStorage) ?: AppCategory.NEUTRAL,
                )
            }
        }
    }

    private suspend fun completionRate(date: LocalDate, goalIds: List<String>): Double? {
        if (goalIds.isEmpty()) return null
        val tasks = goalIds.flatMap { goalId ->
            goalRepository.observeTasks(goalId).first()
        }.filter { it.isScheduledOn(date) }
        if (tasks.isEmpty()) return null
        val logs = goalRepository.observeTaskLogs(date, date).first()
            .filter { it.completed }
            .mapTo(mutableSetOf()) { it.taskId }
        return tasks.count { it.id in logs }.toDouble() / tasks.size
    }

    private suspend fun appLabels(): Map<String, String> {
        if (!appRepository.isLoaded.value) {
            appRepository.isLoaded.first { it }
        }
        return appRepository.apps.value.associate { it.packageName to it.label }
    }

    private fun TaskModel.isScheduledOn(date: LocalDate): Boolean {
        val createdDate = Instant.ofEpochMilli(createdAt).atZone(clock.zone).toLocalDate()
        return createdDate <= date &&
            (recurrence == Recurrence.DAILY ||
                daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0)
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
