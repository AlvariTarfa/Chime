package com.savatech.chimelauncher.domain.streak

import com.savatech.chimelauncher.domain.model.Recurrence
import java.time.LocalDate

data class TaskSpec(
    val recurrence: Recurrence,
    val daysMask: Int,
    val createdOn: LocalDate,
)

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val freezeCredits: Int,
    val lastCompletedDate: LocalDate?,
)

fun compute(task: TaskSpec, completedDates: Set<LocalDate>, today: LocalDate): StreakResult {
    var streak = 0
    var longest = 0
    var freezeCredits = 0
    var lastCompletedDate: LocalDate? = null

    var date = task.createdOn
    while (!date.isAfter(today)) {
        if (isScheduled(task, date)) {
            if (date in completedDates) {
                streak++
                longest = maxOf(longest, streak)
                lastCompletedDate = date
                if (streak % 7 == 0) freezeCredits = minOf(freezeCredits + 1, MAX_FREEZE_CREDITS)
            } else if (date.isBefore(today)) {
                if (freezeCredits > 0) {
                    freezeCredits--
                } else {
                    streak = 0
                }
            }
        }
        date = date.plusDays(1)
    }

    return StreakResult(
        currentStreak = streak,
        longestStreak = longest,
        freezeCredits = freezeCredits,
        lastCompletedDate = lastCompletedDate,
    )
}

private fun isScheduled(task: TaskSpec, date: LocalDate): Boolean =
    task.recurrence == Recurrence.DAILY ||
        task.daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0

private const val MAX_FREEZE_CREDITS = 2
