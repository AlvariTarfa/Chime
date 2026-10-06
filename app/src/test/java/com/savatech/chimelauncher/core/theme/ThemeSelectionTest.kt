package com.savatech.chimelauncher.core.theme

import com.savatech.chimelauncher.data.settings.IconShape
import com.savatech.chimelauncher.data.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSelectionTest {
    @Test
    fun systemModeFollowsSystemAndUsesDynamicOnlyWhenAvailableAndEnabled() {
        assertEquals(
            ThemeSelection(isDark = false, useDynamicColor = true, isAmoled = false),
            resolveThemeSelection(ThemeMode.SYSTEM, false, true, true),
        )
        assertEquals(
            ThemeSelection(isDark = true, useDynamicColor = false, isAmoled = false),
            resolveThemeSelection(ThemeMode.SYSTEM, true, true, false),
        )
        assertFalse(resolveThemeSelection(ThemeMode.SYSTEM, false, false, true).useDynamicColor)
    }

    @Test
    fun lightModeRemainsLightAndHonorsDynamicAvailability() {
        val selection = resolveThemeSelection(ThemeMode.LIGHT, true, true, true)
        assertFalse(selection.isDark)
        assertTrue(selection.useDynamicColor)
        assertFalse(selection.isAmoled)
    }

    @Test
    fun darkModeRemainsDarkAndHonorsDynamicAvailability() {
        val selection = resolveThemeSelection(ThemeMode.DARK, false, true, true)
        assertTrue(selection.isDark)
        assertTrue(selection.useDynamicColor)
        assertFalse(selection.isAmoled)
    }

    @Test
    fun amoledIsDarkAndDisablesDynamicColor() {
        val selection = resolveThemeSelection(ThemeMode.AMOLED, false, true, true)
        assertTrue(selection.isDark)
        assertFalse(selection.useDynamicColor)
        assertTrue(selection.isAmoled)
    }

    @Test
    fun shapeMappingMatchesDocumentedCornerGeometry() {
        assertEquals(50, iconShapeCornerPercent(IconShape.CIRCLE))
        assertEquals(36, iconShapeCornerPercent(IconShape.SQUIRCLE))
        assertEquals(24, iconShapeCornerPercent(IconShape.ROUNDED_SQUARE))
        assertEquals(null, iconShapeCornerPercent(IconShape.NONE))
    }
}
