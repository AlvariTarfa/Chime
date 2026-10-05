package com.savatech.chimelauncher.domain.intercept

import com.savatech.chimelauncher.data.settings.FrictionLevel
import com.savatech.chimelauncher.domain.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class InterceptPolicyTest {
    private val policy = InterceptPolicy()

    @Test
    fun baseDelayMatchesEveryFrictionLevel() {
        val expectedDelays = mapOf(
            FrictionLevel.GENTLE to 5,
            FrictionLevel.BALANCED to 8,
            FrictionLevel.STRICT to 15,
        )

        expectedDelays.forEach { (level, expected) ->
            assertEquals(
                InterceptDecision.Pause(expected, PauseReason.DISTRACTING),
                policy.decide(AppCategory.DISTRACTING, false, level, level.defaultBaseDelaySeconds, 0),
            )
        }
    }

    @Test
    fun openedCountEscalatesDelayAndCapsAtThirtySeconds() {
        assertEquals(
            InterceptDecision.Pause(14, PauseReason.DISTRACTING),
            policy.decide(AppCategory.DISTRACTING, false, FrictionLevel.BALANCED, 8, 2),
        )
        assertEquals(
            InterceptDecision.Pause(30, PauseReason.DISTRACTING),
            policy.decide(AppCategory.DISTRACTING, false, FrictionLevel.STRICT, 15, 100),
        )
    }

    @Test
    fun explicitBaseDelayOverridesTheFrictionDefault() {
        assertEquals(
            InterceptDecision.Pause(11, PauseReason.DISTRACTING),
            policy.decide(AppCategory.DISTRACTING, false, FrictionLevel.GENTLE, 11, 0),
        )
    }

    @Test
    fun activeGrantBypassesPause() {
        assertSame(
            InterceptDecision.Allow,
            policy.decide(AppCategory.DISTRACTING, true, FrictionLevel.STRICT, 15, 5),
        )
    }

    @Test
    fun nonDistractingCategoriesAreAllowed() {
        listOf(AppCategory.NEUTRAL, AppCategory.PRODUCTIVE).forEach { category ->
            assertSame(
                InterceptDecision.Allow,
                policy.decide(category, false, FrictionLevel.STRICT, 15, 0),
            )
        }
    }

    @Test
    fun reachedLimitOverridesGrantAndDoublesDelay() {
        assertEquals(
            InterceptDecision.Pause(16, PauseReason.LIMIT_REACHED),
            policy.decide(
                AppCategory.DISTRACTING,
                true,
                FrictionLevel.BALANCED,
                8,
                0,
                limitReached = true,
            ),
        )
    }

    @Test
    fun reachedLimitPausesNonDistractingAppsAndCapsDelayAtFortyFiveSeconds() {
        assertEquals(
            InterceptDecision.Pause(45, PauseReason.LIMIT_REACHED),
            policy.decide(
                AppCategory.PRODUCTIVE,
                false,
                FrictionLevel.STRICT,
                30,
                100,
                limitReached = true,
            ),
        )
    }
}
