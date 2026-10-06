package com.savatech.chimelauncher.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.core.theme.LocalLayoutMetrics
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.settings.FontPreset
import com.savatech.chimelauncher.data.settings.IconShape
import com.savatech.chimelauncher.data.settings.LayoutDensityPreset
import com.savatech.chimelauncher.data.settings.SwipeAppTarget
import com.savatech.chimelauncher.data.settings.ThemeMode
import com.savatech.chimelauncher.feature.drawer.AppIcon

@Composable
fun CustomizationSettings(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.useDynamicColor.collectAsStateWithLifecycle()
    val accentIndex by viewModel.accentColorIndex.collectAsStateWithLifecycle()
    val iconShape by viewModel.iconShape.collectAsStateWithLifecycle()
    val fontPreset by viewModel.fontPreset.collectAsStateWithLifecycle()
    val density by viewModel.layoutDensity.collectAsStateWithLifecycle()
    val iconPack by viewModel.iconPackPackage.collectAsStateWithLifecycle()
    val leftTarget by viewModel.leftSwipeTarget.collectAsStateWithLifecycle()
    val rightTarget by viewModel.rightSwipeTarget.collectAsStateWithLifecycle()
    val apps by viewModel.availableApps.collectAsStateWithLifecycle()
    val packs by viewModel.iconPacks.collectAsStateWithLifecycle()
    var showPackPicker by remember { mutableStateOf(false) }
    var selectingGesture by remember { mutableStateOf<GestureSide?>(null) }
    val currentApp = apps.firstOrNull()
    val selectedPackName = packs.packs.firstOrNull { it.packageName == iconPack }?.label
    val useAccentPalette =
        !dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S || themeMode == ThemeMode.AMOLED
    val layout = LocalLayoutMetrics.current

    LaunchedEffect(viewModel) { viewModel.refreshIconPacks() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.customization_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.theme_mode_title), style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.chunked(2).forEach { rowModes ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowModes.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(stringResource(mode.labelResource())) },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = dynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onCheckedChange = viewModel::setUseDynamicColor,
            )
            Text(stringResource(R.string.dynamic_color_title))
        }
        if (useAccentPalette) {
            Text(stringResource(R.string.accent_color_title), style = MaterialTheme.typography.titleMedium)
            ACCENT_COLORS.chunked(4).forEachIndexed { rowIndex, rowColors ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowColors.forEachIndexed { localIndex, (colorId, labelId) ->
                        val index = rowIndex * 4 + localIndex
                        FilterChip(
                            selected = accentIndex == index,
                            onClick = { viewModel.setAccentColorIndex(index) },
                            label = {
                                Box(
                                    Modifier
                                        .height(18.dp)
                                        .padding(end = 2.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Surface(
                                        modifier = Modifier.size(14.dp),
                                        color = colorResource(colorId),
                                        shape = MaterialTheme.shapes.small,
                                    ) {
                                        Box(Modifier.size(14.dp))
                                    }
                                }
                                Text(stringResource(labelId))
                            },
                        )
                    }
                }
            }
        }

        Text(stringResource(R.string.icon_shape_title), style = MaterialTheme.typography.titleMedium)
        ChoiceRows(
            values = IconShape.entries,
            selected = iconShape,
            label = { stringResource(it.labelResource()) },
            onSelect = viewModel::setIconShape,
        )
        Text(stringResource(R.string.font_preset_title), style = MaterialTheme.typography.titleMedium)
        ChoiceRows(
            values = FontPreset.entries,
            selected = fontPreset,
            label = { stringResource(it.labelResource()) },
            onSelect = viewModel::setFontPreset,
        )
        Text(stringResource(R.string.layout_density_title), style = MaterialTheme.typography.titleMedium)
        ChoiceRows(
            values = LayoutDensityPreset.entries,
            selected = density,
            label = { stringResource(it.labelResource()) },
            onSelect = viewModel::setLayoutDensity,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                Modifier.padding(
                    horizontal = layout.horizontalPadding.dp,
                    vertical = layout.verticalPadding.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(layout.itemSpacing.dp),
            ) {
                Text(stringResource(R.string.customization_preview), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.customization_preview_text), style = MaterialTheme.typography.bodyLarge)
                if (currentApp != null) {
                    AppIcon(
                        currentApp,
                        viewModel.iconCache,
                        48.dp,
                        iconShape,
                        iconPack,
                    )
                }
                Text(
                    stringResource(R.string.density_preview_row, layout.rowHeight),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(layout.rowHeight.dp)
                        .padding(horizontal = layout.horizontalPadding.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Text(stringResource(R.string.icon_pack_title), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { showPackPicker = true }) {
            Text(selectedPackName ?: stringResource(R.string.icon_pack_none))
        }
        if (packs.isLoading) Text(stringResource(R.string.icon_packs_loading))
        if (packs.loadFailed) Text(
            stringResource(R.string.icon_packs_load_failed),
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = viewModel::refreshIconPacks) {
            Text(stringResource(R.string.refresh_icon_packs))
        }

        Text(stringResource(R.string.home_gestures_title), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { selectingGesture = GestureSide.LEFT }) {
            Text(
                stringResource(
                    R.string.swipe_left_setting,
                    targetLabel(leftTarget, apps) ?: stringResource(R.string.gesture_app_none),
                ),
            )
        }
        TextButton(onClick = { selectingGesture = GestureSide.RIGHT }) {
            Text(
                stringResource(
                    R.string.swipe_right_setting,
                    targetLabel(rightTarget, apps) ?: stringResource(R.string.gesture_app_none),
                ),
            )
        }
    }

    if (showPackPicker) {
        AlertDialog(
            onDismissRequest = { showPackPicker = false },
            title = { Text(stringResource(R.string.icon_pack_title)) },
            text = {
                LazyColumn {
                    item {
                        TextButton(onClick = {
                            viewModel.setIconPack(null)
                            showPackPicker = false
                        }) { Text(stringResource(R.string.icon_pack_none)) }
                    }
                    items(packs.packs, key = { it.packageName }) { pack ->
                        TextButton(onClick = {
                            viewModel.setIconPack(pack.packageName)
                            showPackPicker = false
                        }) { Text(pack.label) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPackPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    selectingGesture?.let { side ->
        AlertDialog(
            onDismissRequest = { selectingGesture = null },
            title = {
                Text(stringResource(if (side == GestureSide.LEFT) R.string.swipe_left_title else R.string.swipe_right_title))
            },
            text = {
                LazyColumn {
                    item {
                        TextButton(onClick = {
                            setGestureTarget(viewModel, side, null)
                            selectingGesture = null
                        }) { Text(stringResource(R.string.gesture_app_none)) }
                    }
                    items(apps, key = AppInfo::key) { app ->
                        TextButton(onClick = {
                            setGestureTarget(viewModel, side, app)
                            selectingGesture = null
                        }) { Text(app.label) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectingGesture = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun <T> ChoiceRows(
    values: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    values.chunked(3).forEach { rowValues ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            rowValues.forEach { value ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label(value)) },
                )
            }
        }
    }
}

private fun setGestureTarget(viewModel: SettingsViewModel, side: GestureSide, app: AppInfo?) {
    if (side == GestureSide.LEFT) viewModel.setLeftSwipeApp(app) else viewModel.setRightSwipeApp(app)
}

private fun targetLabel(target: SwipeAppTarget?, apps: List<AppInfo>): String? =
    target?.let { chosen ->
        apps.firstOrNull {
            it.packageName == chosen.packageName &&
                it.className == chosen.className &&
                it.userSerial == chosen.userSerial
        }?.label
    }

private enum class GestureSide { LEFT, RIGHT }

private fun ThemeMode.labelResource(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_mode_system
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
    ThemeMode.AMOLED -> R.string.theme_mode_amoled
}

private fun IconShape.labelResource(): Int = when (this) {
    IconShape.CIRCLE -> R.string.icon_shape_circle
    IconShape.SQUIRCLE -> R.string.icon_shape_rounded
    IconShape.ROUNDED_SQUARE -> R.string.icon_shape_rounded_square
    IconShape.NONE -> R.string.icon_shape_none
}

private fun FontPreset.labelResource(): Int = when (this) {
    FontPreset.DEFAULT -> R.string.font_default
    FontPreset.SANS_SERIF -> R.string.font_sans_serif
    FontPreset.SERIF -> R.string.font_serif
    FontPreset.MONOSPACE -> R.string.font_monospace
    FontPreset.CURSIVE -> R.string.font_cursive
}

private fun LayoutDensityPreset.labelResource(): Int = when (this) {
    LayoutDensityPreset.COMPACT -> R.string.density_compact
    LayoutDensityPreset.COMFORTABLE -> R.string.density_comfortable
    LayoutDensityPreset.SPACIOUS -> R.string.density_spacious
}

private val ACCENT_COLORS = listOf(
    R.color.accent_blue to R.string.accent_blue,
    R.color.accent_teal to R.string.accent_teal,
    R.color.accent_green to R.string.accent_green,
    R.color.accent_amber to R.string.accent_amber,
    R.color.accent_orange to R.string.accent_orange,
    R.color.accent_red to R.string.accent_red,
    R.color.accent_pink to R.string.accent_pink,
    R.color.accent_purple to R.string.accent_purple,
)
