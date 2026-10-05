package com.savatech.chimelauncher.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageEnumTest {
    @Test
    fun goalStatusReadsKnownValuesAndDefaultsUnknownValues() {
        assertEquals(GoalStatus.ARCHIVED, GoalStatus.fromStorage("ARCHIVED"))
        assertEquals(GoalStatus.ACTIVE, GoalStatus.fromStorage("unknown"))
    }

    @Test
    fun recurrenceReadsKnownValuesAndDefaultsUnknownValues() {
        assertEquals(Recurrence.DAYS_OF_WEEK, Recurrence.fromStorage("DAYS_OF_WEEK"))
        assertEquals(Recurrence.DAILY, Recurrence.fromStorage("unknown"))
    }

    @Test
    fun appCategoryReadsKnownValuesAndDefaultsUnknownValues() {
        assertEquals(AppCategory.PRODUCTIVE, AppCategory.fromStorage("PRODUCTIVE"))
        assertEquals(AppCategory.NEUTRAL, AppCategory.fromStorage("unknown"))
    }

    @Test
    fun interceptOutcomeReadsKnownValuesAndDefaultsUnknownValues() {
        assertEquals(InterceptOutcome.LIMIT_OVERRIDE, InterceptOutcome.fromStorage("LIMIT_OVERRIDE"))
        assertEquals(InterceptOutcome.CANCELLED, InterceptOutcome.fromStorage("unknown"))
    }

    @Test
    fun checkInTypeReadsKnownValuesAndDefaultsUnknownValues() {
        assertEquals(CheckInType.WEEKLY, CheckInType.fromStorage("WEEKLY"))
        assertEquals(CheckInType.MORNING, CheckInType.fromStorage("unknown"))
    }
}
