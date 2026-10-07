package com.savatech.chimelauncher.core.launch

import android.database.sqlite.SQLiteException
import com.savatech.chimelauncher.data.apps.AppConfigSource
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.settings.InterceptSettings
import com.savatech.chimelauncher.data.usage.UsageAccess
import com.savatech.chimelauncher.data.usage.UsageRepository
import com.savatech.chimelauncher.domain.intercept.InterceptDecision
import com.savatech.chimelauncher.domain.intercept.PauseReason
import com.savatech.chimelauncher.domain.limits.effectiveLimit
import com.savatech.chimelauncher.domain.model.AppCategory
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

@Singleton
class AppLauncher @Inject constructor(
    private val appRepository: AppRepository,
    private val appConfigSource: AppConfigSource,
    private val interceptCoordinator: InterceptCoordinator,
    private val settings: InterceptSettings,
    private val usageRepository: UsageRepository,
    private val usagePermission: UsageAccess,
    private val clock: Clock,
) {
    private val warnedApps = mutableSetOf<Pair<LocalDate, String>>()

    suspend fun requestLaunch(app: AppInfo): LaunchResult {
        return try {
            val evaluation = interceptCoordinator.evaluate(app.packageName)
            when (val decision = evaluation.decision) {
                InterceptDecision.Allow -> launchApp(app)
                is InterceptDecision.Pause -> LaunchResult.NeedsPause(
                    PauseLaunchArgs(
                        packageName = app.packageName,
                        className = app.className,
                        userSerial = app.userSerial,
                        appLabel = app.label,
                        delaySeconds = decision.delaySeconds,
                        reason = decision.reason,
                        usedMillis = evaluation.usedMillis,
                        limitMinutes = evaluation.limitMinutes.takeIf {
                            decision.reason == PauseReason.LIMIT_REACHED
                        },
                    ),
                )
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            LaunchResult.Failed(exception)
        } catch (exception: SQLiteException) {
            LaunchResult.Failed(exception)
        } catch (exception: SecurityException) {
            LaunchResult.Failed(exception)
        } catch (exception: IllegalStateException) {
            LaunchResult.Failed(exception)
        }
    }

    suspend fun dailyLimitWarnings(): List<String> {
        val limitPermission = usagePermission.isGranted()
        if (!limitPermission) return emptyList()
        val date = LocalDate.now(clock)
        val usageByPackage = usageRepository.dailyAppUsage(date)
        val configs = appConfigSource.getAllConfigs()
        val categoryLimits = settings.categoryDailyLimits.first()
        synchronized(warnedApps) {
            warnedApps.removeAll { it.first != date }
        }
        return appRepository.apps.value
            .distinctBy(AppInfo::packageName)
            .mapNotNull { app ->
                val config = configs[app.packageName]
                val category = config?.category?.let(AppCategory::fromStorage) ?: AppCategory.NEUTRAL
                val limit = effectiveLimit(config?.dailyLimitMin, categoryLimits[category])
                    ?: return@mapNotNull null
                val usedMillis = usageByPackage[app.packageName] ?: 0L
                val limitMillis = limit.toLong() * MILLIS_PER_MINUTE
                if (usedMillis < limitMillis * WARNING_THRESHOLD_NUMERATOR / WARNING_THRESHOLD_DENOMINATOR) {
                    return@mapNotNull null
                }
                val isFirstWarning = synchronized(warnedApps) {
                    warnedApps.add(date to app.packageName)
                }
                app.label.takeIf { isFirstWarning }
            }
    }

    private suspend fun launchApp(app: AppInfo): LaunchResult =
        appRepository.launch(app).fold(
            onSuccess = { LaunchResult.Started },
            onFailure = LaunchResult::Failed,
        )

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val WARNING_THRESHOLD_NUMERATOR = 4L
        const val WARNING_THRESHOLD_DENOMINATOR = 5L
    }
}
