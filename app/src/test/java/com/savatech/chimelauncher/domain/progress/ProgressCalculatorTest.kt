package com.savatech.chimelauncher.domain.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressCalculatorTest {
    @Test
    fun dailyProgress_returnsNullWhenNothingIsScheduled() {
        assertNull(dailyProgress(scheduledTaskCount = 0, completedCount = 0))
        assertNull(dailyProgress(scheduledTaskCount = -1, completedCount = 0))
    }

    @Test
    fun dailyProgress_clampsCompletionCountToZeroThroughOne() {
        assertEquals(0f, dailyProgress(scheduledTaskCount = 2, completedCount = -1))
        assertEquals(1f, dailyProgress(scheduledTaskCount = 2, completedCount = 3))
        assertEquals(0.5f, dailyProgress(scheduledTaskCount = 2, completedCount = 1))
    }

    @Test
    fun overallProgress_returnsNullForMissingOrNonPositiveTarget() {
        assertNull(overallProgress(1.0, null))
        assertNull(overallProgress(1.0, 0.0))
        assertNull(overallProgress(1.0, -1.0))
    }

    @Test
    fun overallProgress_clampsResultToZeroThroughOne() {
        assertEquals(0f, overallProgress(-1.0, 2.0))
        assertEquals(1f, overallProgress(3.0, 2.0))
        assertEquals(0.5f, overallProgress(1.0, 2.0))
    }
}
