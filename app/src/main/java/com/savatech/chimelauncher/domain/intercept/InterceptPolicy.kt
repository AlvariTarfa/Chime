package com.savatech.chimelauncher.domain.intercept

import com.savatech.chimelauncher.data.settings.FrictionLevel
import com.savatech.chimelauncher.domain.model.AppCategory
import javax.inject.Inject

class InterceptPolicy @Inject constructor() {
    fun decide(
        category: AppCategory,
        hasActiveGrant: Boolean,
        frictionLevel: FrictionLevel,
        baseDelaySeconds: Int,
        openedTodayCount: Int,
        limitReached: Boolean = false,
    ): InterceptDecision {
        val configuredBaseDelay = if (baseDelaySeconds < 0) {
            frictionLevel.defaultBaseDelaySeconds
        } else {
            baseDelaySeconds
        }
        val delaySeconds = (
            configuredBaseDelay.toLong() +
                openedTodayCount.coerceAtLeast(0).toLong() * ESCALATION_SECONDS
            ).coerceAtMost(MAX_DELAY_SECONDS).toInt()

        if (limitReached) {
            return InterceptDecision.Pause(
                (delaySeconds.toLong() * LIMIT_DELAY_MULTIPLIER)
                    .coerceAtMost(MAX_LIMIT_DELAY_SECONDS).toInt(),
                PauseReason.LIMIT_REACHED,
            )
        }
        if (category != AppCategory.DISTRACTING || hasActiveGrant) return InterceptDecision.Allow

        return InterceptDecision.Pause(delaySeconds, PauseReason.DISTRACTING)
    }

    private companion object {
        const val ESCALATION_SECONDS = 3L
        const val MAX_DELAY_SECONDS = 30L
        const val LIMIT_DELAY_MULTIPLIER = 2L
        const val MAX_LIMIT_DELAY_SECONDS = 45L
    }
}
