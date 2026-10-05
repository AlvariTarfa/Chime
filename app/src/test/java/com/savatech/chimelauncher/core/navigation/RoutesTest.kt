package com.savatech.chimelauncher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun routesExposeTheExpectedDestinations() {
        assertEquals("home", Routes.Home)
        assertEquals("drawer", Routes.Drawer)
        assertEquals("goals", Routes.Goals)
        assertEquals("goals/edit/{goalId}", Routes.GoalEdit)
        assertEquals("goals/detail/{goalId}", Routes.GoalDetail)
        assertEquals("goals/priorities", Routes.GoalPriorities)
        assertEquals("goals/edit/new", Routes.goalEdit())
        assertEquals("goals/detail/goal-id", Routes.goalDetail("goal-id"))
        assertEquals("settings", Routes.Settings)
    }
}
