package com.savatech.chimelauncher.core.launch

import com.savatech.chimelauncher.core.di.IoDispatcher
import com.savatech.chimelauncher.data.apps.AppConfigSource
import com.savatech.chimelauncher.data.intercept.InterceptRepository
import com.savatech.chimelauncher.data.settings.InterceptSettings
import com.savatech.chimelauncher.data.usage.UsageAccess
import com.savatech.chimelauncher.data.usage.UsageRepository
import com.savatech.chimelauncher.domain.intercept.InterceptDecision
import com.savatech.chimelauncher.domain.intercept.InterceptPolicy
import com.savatech.chimelauncher.domain.limits.LimitState
import com.savatech.chimelauncher.domain.limits.effectiveLimit
import com.savatech.chimelauncher.domain.limits.evaluate
import com.savatech.chimelauncher.domain.model.AppCategory
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class InterceptEvaluation(
    val decision: InterceptDecision,
    val usedMillis: Long,
    val limitMinutes: Int?,
)

@Singleton
class InterceptCoordinator @Inject constructor(
    private val appConfigSource: AppConfigSource,
    private val interceptRepository: InterceptRepository,
    private val settings: InterceptSettings,
    private val policy: InterceptPolicy,
    private val usageRepository: UsageRepository,
    private val usagePermission: UsageAccess,
    private val clock: Clock,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun evaluate(packageName: String): InterceptEvaluation = withContext(ioDispatcher) {
        val config = appConfigSource.getConfig(packageName)
        val category = config?.category?.let(AppCategory::fromStorage) ?: AppCategory.NEUTRAL
        val limitMinutes = effectiveLimit(
            config?.dailyLimitMin,
            settings.categoryDailyLimits.first()[category],
        )
        val usedMillis = if (limitMinutes != null && usagePermission.isGranted()) {
            usageRepository.dailyAppUsage(LocalDate.now(clock))[packageName] ?: 0L
        } else {
            0L
        }
        val limitReached = evaluate(usedMillis, limitMinutes) == LimitState.Reached
        val hasGrant = interceptRepository.hasActiveGrant(packageName)
        val frictionLevel = settings.frictionLevel.first()
        val baseDelaySeconds = settings.baseDelaySeconds.first()
        val openedTodayCount = interceptRepository.openedTodayCount(packageName)
        InterceptEvaluation(
            decision = policy.decide(
                category = category,
                hasActiveGrant = hasGrant,
                frictionLevel = frictionLevel,
                baseDelaySeconds = baseDelaySeconds,
                openedTodayCount = openedTodayCount,
                limitReached = limitReached,
            ),
            usedMillis = usedMillis,
            limitMinutes = limitMinutes,
        )
    }
}
