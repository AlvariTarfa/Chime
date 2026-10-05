package com.savatech.chimelauncher.domain.focus

import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class FocusSchedule(
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val end: LocalTime,
) {
    fun toJson(): String = Json.encodeToString(
        ScheduleJson(days.map(DayOfWeek::name), start.toString(), end.toString()),
    )

    companion object {
        fun fromJson(value: String): FocusSchedule {
            val stored = Json.decodeFromString<ScheduleJson>(value)
            return FocusSchedule(
                days = stored.days.map(DayOfWeek::valueOf).toSet(),
                start = LocalTime.parse(stored.start),
                end = LocalTime.parse(stored.end),
            )
        }
    }
}

@Serializable
private data class ScheduleJson(
    val days: List<String>,
    val start: String,
    val end: String,
)
