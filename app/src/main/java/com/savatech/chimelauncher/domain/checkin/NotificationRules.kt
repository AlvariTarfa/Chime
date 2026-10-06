package com.savatech.chimelauncher.domain.checkin

import java.time.LocalTime

fun isWithinQuietHours(
    time: LocalTime,
    quietStart: LocalTime?,
    quietEnd: LocalTime?,
): Boolean {
    if (quietStart == null || quietEnd == null || quietStart == quietEnd) return false
    return if (quietStart < quietEnd) {
        time >= quietStart && time < quietEnd
    } else {
        time >= quietStart || time < quietEnd
    }
}

fun shouldPostNudge(
    nudgeEnabled: Boolean,
    hasPriorityGoal: Boolean,
    completedTaskCountToday: Int,
): Boolean = nudgeEnabled && hasPriorityGoal && completedTaskCountToday == 0
