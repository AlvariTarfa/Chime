package com.savatech.chimelauncher.domain.limits

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitEvaluatorTest {
    @Test
    fun absentOrZeroLimitHasNoLimit() {
        assertEquals(LimitState.NoLimit, evaluate(100_000L, null))
        assertEquals(LimitState.NoLimit, evaluate(100_000L, 0))
    }

    @Test
    fun evaluatesUnderExactlyAtAndOverLimit() {
        assertEquals(LimitState.Under(1L), evaluate(119_999L, 2))
        assertEquals(LimitState.Reached, evaluate(120_000L, 2))
        assertEquals(LimitState.Reached, evaluate(120_001L, 2))
    }

    @Test
    fun appLimitOverridesCategoryAndZeroClearsAppOverride() {
        assertEquals(20, effectiveLimit(20, 30))
        assertEquals(30, effectiveLimit(null, 30))
        assertEquals(0, effectiveLimit(0, 30))
        assertEquals(null, effectiveLimit(null, null))
        assertEquals(0, effectiveLimit(0, null))
    }

    @Test
    fun overrideReasonMustContainTenNonWhitespaceCharactersAfterTrimming() {
        assertTrue(isValidLimitOverrideReason("  because I need it  "))
        assertFalse(isValidLimitOverrideReason(" 123456789 "))
        assertFalse(isValidLimitOverrideReason("          "))
    }
}
