package com.savatech.chimelauncher.domain.focus

import com.savatech.chimelauncher.data.db.entities.FocusMode
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class ManualModeOverride(
    val modeId: String?,
    val until: LocalDateTime?,
)

class ActiveModeResolver @Inject constructor(
    private val clock: Clock,
) {
    fun resolve(
        modes: List<FocusMode>,
        manualOverride: ManualModeOverride?,
        now: LocalDateTime = LocalDateTime.now(clock),
    ): FocusMode? = resolveActiveMode(modes, manualOverride, now)
}

fun resolveActiveMode(
    modes: List<FocusMode>,
    manualOverride: ManualModeOverride?,
    now: LocalDateTime,
): FocusMode? {
    if (manualOverride != null && (manualOverride.until == null || now < manualOverride.until)) {
        return manualOverride.modeId?.let { id -> modes.firstOrNull { it.id == id } }
    }

    return modes.mapNotNull { mode ->
        val json = mode.scheduleJson ?: return@mapNotNull null
        val schedule = FocusSchedule.fromJson(json)
        val startDate = matchingStartDate(schedule, now) ?: return@mapNotNull null
        mode to LocalDateTime.of(startDate, schedule.start)
    }.maxWithOrNull(compareBy<Pair<FocusMode, LocalDateTime>> { it.second }.thenBy { it.first.id })
        ?.first
}

private fun matchingStartDate(
    schedule: FocusSchedule,
    now: LocalDateTime,
): LocalDate? {
    val today = now.toLocalDate()
    if (schedule.end > schedule.start) {
        return today.takeIf {
            now.dayOfWeek in schedule.days && now.toLocalTime() >= schedule.start &&
                now.toLocalTime() < schedule.end
        }
    }
    if (now.toLocalTime() >= schedule.start && now.dayOfWeek in schedule.days) return today
    val yesterday = today.minusDays(1)
    return yesterday.takeIf {
        now.toLocalTime() < schedule.end && yesterday.dayOfWeek in schedule.days
    }
}
