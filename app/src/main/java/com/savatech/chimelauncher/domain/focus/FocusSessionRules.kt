package com.savatech.chimelauncher.domain.focus

import com.savatech.chimelauncher.data.db.entities.TaskLog

fun remainingSessionMillis(startedAt: Long, plannedMinutes: Int, now: Long): Long {
    require(plannedMinutes > 0) { "plannedMinutes must be positive." }
    val deadline = Math.addExact(startedAt, Math.multiplyExact(plannedMinutes.toLong(), MILLIS_PER_MINUTE))
    return (deadline - now).coerceAtLeast(0L)
}

fun creditSessionToTaskLog(
    existing: TaskLog?,
    taskId: String?,
    date: String,
    goalUnit: String?,
    plannedMinutes: Int,
): TaskLog? {
    require(plannedMinutes > 0) { "plannedMinutes must be positive." }
    if (taskId == null) return null
    val increment = timeUnitIncrement(plannedMinutes, goalUnit)
    return TaskLog(
        id = existing?.id ?: 0,
        taskId = taskId,
        date = date,
        value = increment?.let { (existing?.value ?: 0.0) + it } ?: existing?.value,
        completed = true,
    )
}

private fun timeUnitIncrement(minutes: Int, unit: String?): Double? =
    when (unit?.trim()?.lowercase()) {
        "minute", "minutes", "min", "mins" -> minutes.toDouble()
        "hour", "hours", "hr", "hrs" -> minutes / 60.0
        "second", "seconds", "sec", "secs" -> minutes * 60.0
        else -> null
    }

private const val MILLIS_PER_MINUTE = 60_000L
