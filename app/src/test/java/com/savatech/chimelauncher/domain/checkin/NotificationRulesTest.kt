package com.savatech.chimelauncher.domain.checkin

import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {
    @Test
    fun quietHoursHandleNormalOvernightAndDisabledRanges() {
        val start = LocalTime.of(9, 0)
        val end = LocalTime.of(17, 0)
        assertTrue(isWithinQuietHours(LocalTime.of(9, 0), start, end))
        assertTrue(isWithinQuietHours(LocalTime.of(16, 59), start, end))
        assertFalse(isWithinQuietHours(LocalTime.of(17, 0), start, end))
        assertFalse(isWithinQuietHours(LocalTime.of(8, 59), start, end))

        val overnightStart = LocalTime.of(22, 0)
        val overnightEnd = LocalTime.of(7, 0)
        assertTrue(isWithinQuietHours(LocalTime.of(23, 30), overnightStart, overnightEnd))
        assertTrue(isWithinQuietHours(LocalTime.of(6, 59), overnightStart, overnightEnd))
        assertFalse(isWithinQuietHours(LocalTime.of(7, 0), overnightStart, overnightEnd))
        assertFalse(isWithinQuietHours(LocalTime.NOON, overnightStart, overnightEnd))
        assertFalse(isWithinQuietHours(LocalTime.NOON, null, overnightEnd))
        assertFalse(isWithinQuietHours(LocalTime.NOON, start, start))
    }

    @Test
    fun nudgeRuleRequiresSettingPriorityAndZeroCompletedTasks() {
        assertTrue(shouldPostNudge(true, true, 0))
        assertFalse(shouldPostNudge(false, true, 0))
        assertFalse(shouldPostNudge(true, false, 0))
        assertFalse(shouldPostNudge(true, true, 1))
        assertFalse(shouldPostNudge(false, false, 1))
    }
}
