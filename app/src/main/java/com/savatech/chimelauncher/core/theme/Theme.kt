package com.savatech.chimelauncher.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Typography
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.settings.FontPreset
import com.savatech.chimelauncher.data.settings.LayoutDensityPreset
import com.savatech.chimelauncher.data.settings.ThemeMode

const val WallpaperScrimAlpha = 0.88f

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
)

data class ThemeSelection(
    val isDark: Boolean,
    val useDynamicColor: Boolean,
    val isAmoled: Boolean,
)

fun resolveThemeSelection(
    mode: ThemeMode,
    systemIsDark: Boolean,
    dynamicColorEnabled: Boolean,
    dynamicColorAvailable: Boolean,
): ThemeSelection {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    return ThemeSelection(
        isDark = dark,
        useDynamicColor = mode != ThemeMode.AMOLED && dynamicColorEnabled && dynamicColorAvailable,
        isAmoled = mode == ThemeMode.AMOLED,
    )
}

data class LayoutMetrics(
    val rowHeight: Int,
    val horizontalPadding: Int,
    val verticalPadding: Int,
    val itemSpacing: Int,
)

fun layoutMetrics(preset: LayoutDensityPreset): LayoutMetrics = when (preset) {
    LayoutDensityPreset.COMPACT -> LayoutMetrics(44, 12, 4, 6)
    LayoutDensityPreset.COMFORTABLE -> LayoutMetrics(52, 16, 8, 10)
    LayoutDensityPreset.SPACIOUS -> LayoutMetrics(64, 20, 12, 14)
}

val LocalLayoutMetrics = compositionLocalOf { layoutMetrics(LayoutDensityPreset.COMFORTABLE) }

fun iconShapeCornerPercent(shape: com.savatech.chimelauncher.data.settings.IconShape): Int? =
    when (shape) {
        com.savatech.chimelauncher.data.settings.IconShape.CIRCLE -> 50
        com.savatech.chimelauncher.data.settings.IconShape.SQUIRCLE -> 36
        com.savatech.chimelauncher.data.settings.IconShape.ROUNDED_SQUARE -> 24
        com.savatech.chimelauncher.data.settings.IconShape.NONE -> null
    }

private fun Typography.withFontFamily(fontPreset: FontPreset): Typography {
    val family = when (fontPreset) {
        FontPreset.DEFAULT -> null
        FontPreset.SANS_SERIF -> FontFamily.SansSerif
        FontPreset.SERIF -> FontFamily.Serif
        FontPreset.MONOSPACE -> FontFamily.Monospace
        FontPreset.CURSIVE -> FontFamily.Cursive
    } ?: return this
    return copy(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
    )
}

@Composable
fun ChimeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColorEnabled: Boolean = true,
    accentColorIndex: Int = 0,
    fontPreset: FontPreset = FontPreset.DEFAULT,
    densityPreset: LayoutDensityPreset = LayoutDensityPreset.COMFORTABLE,
    bedtimeMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val selection = resolveThemeSelection(
        mode = themeMode,
        systemIsDark = isSystemInDarkTheme() || bedtimeMode,
        dynamicColorEnabled = dynamicColorEnabled,
        dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    )
    val accentColorId = when (accentColorIndex.coerceIn(0, 7)) {
        0 -> R.color.accent_blue
        1 -> R.color.accent_teal
        2 -> R.color.accent_green
        3 -> R.color.accent_amber
        4 -> R.color.accent_orange
        5 -> R.color.accent_red
        6 -> R.color.accent_pink
        else -> R.color.accent_purple
    }
    val accent = colorResource(accentColorId)
    val baseScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            selection.useDynamicColor && selection.isDark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && selection.useDynamicColor ->
            dynamicLightColorScheme(context)
        selection.isDark -> DarkColors
        else -> LightColors
    }
    val accentedScheme = if (selection.useDynamicColor) {
        baseScheme
    } else {
        baseScheme.copy(
            primary = accent,
            onPrimary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
            secondary = accent,
            onSecondary = if (accent.luminance() > 0.5f) Color.Black else Color.White,
            tertiary = accent,
        )
    }
    val colorScheme = if (selection.isAmoled) {
        accentedScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
        )
    } else {
        accentedScheme
    }

    CompositionLocalProvider(LocalLayoutMetrics provides layoutMetrics(densityPreset)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ChimeTypography.withFontFamily(fontPreset),
            content = content,
        )
    }
}
