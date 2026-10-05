package com.savatech.chimelauncher.domain.focus

import com.savatech.chimelauncher.data.db.entities.FocusMode
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveModeResolverTest {
    @Test
    fun overnightScheduleBelongsToItsStartDay() {
        val mode = mode("night", schedule(DayOfWeek.MONDAY, "22:00", "06:00"))
        assertEquals(
            mode,
            resolveActiveMode(listOf(mode), null, LocalDateTime.parse("2026-10-06T02:00:00")),
        )
        assertNull(resolveActiveMode(listOf(mode), null, LocalDateTime.parse("2026-10-07T02:00:00")))
    }

    @Test
    fun scheduleEndIsExclusiveAndNextDayStartsAreIndependent() {
        val mode = mode("morning", schedule(DayOfWeek.TUESDAY, "08:00", "09:00"))
        assertEquals(mode, resolveActiveMode(listOf(mode), null, LocalDateTime.parse("2026-10-06T08:00:00")))
        assertNull(resolveActiveMode(listOf(mode), null, LocalDateTime.parse("2026-10-06T09:00:00")))
        assertNull(resolveActiveMode(listOf(mode), null, LocalDateTime.parse("2026-10-07T08:30:00")))
    }

    @Test
    fun activeManualOverrideWinsUntilExpiration() {
        val scheduled = mode("scheduled", schedule(DayOfWeek.TUESDAY, "08:00", "09:00"))
        val manual = mode("manual")
        assertEquals(
            manual,
            resolveActiveMode(
                listOf(scheduled, manual),
                ManualModeOverride("manual", LocalDateTime.parse("2026-10-06T08:30:00")),
                LocalDateTime.parse("2026-10-06T08:10:00"),
            ),
        )
        assertEquals(
            scheduled,
            resolveActiveMode(
                listOf(scheduled, manual),
                ManualModeOverride("manual", LocalDateTime.parse("2026-10-06T08:30:00")),
                LocalDateTime.parse("2026-10-06T08:30:00"),
            ),
        )
    }

    @Test
    fun overlappingSchedulesChooseLatestStart() {
        val earlier = mode("early", schedule(DayOfWeek.TUESDAY, "07:00", "10:00"))
        val later = mode("late", schedule(DayOfWeek.TUESDAY, "08:00", "09:00"))
        assertEquals(
            later,
            resolveActiveMode(
                listOf(earlier, later),
                null,
                LocalDateTime.parse("2026-10-06T08:30:00"),
            ),
        )
    }

    @Test
    fun noSchedulesAndNoModesReturnsNull() {
        assertNull(resolveActiveMode(emptyList(), null, LocalDateTime.parse("2026-10-06T08:30:00")))
        assertNull(resolveActiveMode(listOf(mode("unscheduled")), null, LocalDateTime.parse("2026-10-06T08:30:00")))
    }

    private fun schedule(day: DayOfWeek, start: String, end: String) =
        FocusSchedule(setOf(day), LocalTime.parse(start), LocalTime.parse(end)).toJson()

    private fun mode(id: String, schedule: String? = null) = FocusMode(
        id = id,
        name = id,
        allowedPackages = "[]",
        scheduleJson = schedule,
        suppressNotifications = false,
        isBuiltIn = false,
    )
}
