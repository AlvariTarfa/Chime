package com.savatech.chimelauncher.feature.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.settings.DrawerMode
import com.savatech.chimelauncher.domain.model.AppCategory
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun SettingsScreen(
    onOpenHiddenApps: () -> Unit,
    onOpenCheckInHistory: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenDigest: () -> Unit,
    onOpenAccessibilityDisclosure: () -> Unit,
    onOpenDataManagement: () -> Unit,
    onRunSetupAgain: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val drawerMode by viewModel.drawerMode.collectAsStateWithLifecycle()
    val categoryDailyLimits by viewModel.categoryDailyLimits.collectAsStateWithLifecycle()
    val assumedMinutesPerOpen by viewModel.assumedMinutesPerOpen.collectAsStateWithLifecycle()
    val focusScoreEnabled by viewModel.focusScoreEnabled.collectAsStateWithLifecycle()
    val usagePermissionGranted by viewModel.usagePermissionGranted.collectAsStateWithLifecycle()
    val accessibilityServiceEnabled by viewModel.accessibilityServiceEnabled.collectAsStateWithLifecycle()
    val morningEnabled by viewModel.morningCheckInEnabled.collectAsStateWithLifecycle()
    val morningTime by viewModel.morningCheckInTime.collectAsStateWithLifecycle()
    val eveningEnabled by viewModel.eveningCheckInEnabled.collectAsStateWithLifecycle()
    val eveningTime by viewModel.eveningCheckInTime.collectAsStateWithLifecycle()
    val weeklyEnabled by viewModel.weeklyCheckInEnabled.collectAsStateWithLifecycle()
    val weeklyTime by viewModel.weeklyCheckInTime.collectAsStateWithLifecycle()
    val weeklyDay by viewModel.weeklyCheckInDay.collectAsStateWithLifecycle()
    val nudgesEnabled by viewModel.nudgesEnabled.collectAsStateWithLifecycle()
    val quietStart by viewModel.quietHoursStart.collectAsStateWithLifecycle()
    val quietEnd by viewModel.quietHoursEnd.collectAsStateWithLifecycle()
    val permissionRequested by viewModel.notificationPermissionRequested.collectAsStateWithLifecycle()
    var editingCategory by remember { mutableStateOf<AppCategory?>(null) }
    var categoryLimitInput by remember { mutableStateOf("") }
    var pendingPermissionSetting by remember { mutableStateOf<String?>(null) }
    var showPermissionDenied by remember { mutableStateOf(false) }
    var showWeekdays by remember { mutableStateOf(false) }
    var showEstimateEditor by remember { mutableStateOf(false) }
    var estimateInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUsagePermission()
                viewModel.refreshAccessibilityService()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(viewModel) { viewModel.refreshAccessibilityService() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val setting = pendingPermissionSetting
        pendingPermissionSetting = null
        if (granted) applyNotificationSetting(viewModel, setting)
        else showPermissionDenied = true
    }
    fun updateNotificationSetting(key: String, enabled: Boolean, update: () -> Unit) {
        if (!enabled || Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            update()
        } else if (permissionRequested) {
            showPermissionDenied = true
        } else {
            pendingPermissionSetting = key
            viewModel.markNotificationPermissionRequested()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.accessibility_pause_status), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(
                when (accessibilityServiceEnabled) {
                    true -> R.string.accessibility_service_enabled
                    false -> R.string.accessibility_service_disabled
                    null -> R.string.accessibility_service_checking
                },
            ),
        )
        TextButton(onClick = onOpenAccessibilityDisclosure) {
            Text(stringResource(R.string.accessibility_pause_title))
        }
        CustomizationSettings(viewModel)
        TextButton(onClick = onRunSetupAgain) {
            Text(stringResource(R.string.run_setup_again))
        }
        TextButton(onClick = onOpenDataManagement) {
            Text(stringResource(R.string.data_management_title))
        }
        TextButton(onClick = onOpenInsights) {
            Text(stringResource(R.string.insights_title))
        }
        TextButton(onClick = onOpenDigest) {
            Text(stringResource(R.string.digest_settings_title))
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(
                checked = focusScoreEnabled,
                onCheckedChange = viewModel::setFocusScoreEnabled,
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.focus_score_title))
                Text(
                    stringResource(R.string.focus_score_formula),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        TextButton(onClick = {
            estimateInput = assumedMinutesPerOpen.toString()
            showEstimateEditor = true
        }) {
            Text(stringResource(R.string.assumed_minutes_per_open, assumedMinutesPerOpen))
        }
        Text(stringResource(R.string.check_in_settings_title), style = MaterialTheme.typography.titleLarge)
        NotificationSettingRow(
            label = stringResource(R.string.morning_check_in_title),
            enabled = morningEnabled,
            time = morningTime,
            onEnabledChange = { enabled ->
                updateNotificationSetting("morning", enabled) { viewModel.setMorningCheckInEnabled(enabled) }
            },
            onTimeClick = {
                showTimePicker(context, morningTime) { viewModel.setMorningCheckInTime(it) }
            },
        )
        NotificationSettingRow(
            label = stringResource(R.string.evening_check_in_title),
            enabled = eveningEnabled,
            time = eveningTime,
            onEnabledChange = { enabled ->
                updateNotificationSetting("evening", enabled) { viewModel.setEveningCheckInEnabled(enabled) }
            },
            onTimeClick = {
                showTimePicker(context, eveningTime) { viewModel.setEveningCheckInTime(it) }
            },
        )
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(
                checked = weeklyEnabled,
                onCheckedChange = { enabled ->
                    updateNotificationSetting("weekly", enabled) { viewModel.setWeeklyCheckInEnabled(enabled) }
                },
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.weekly_review_title))
                Row {
                    TextButton(onClick = { showWeekdays = true }) {
                        Text(stringResource(DayOfWeek.valueOf(weeklyDay).labelResource()))
                    }
                    TextButton(onClick = {
                        showTimePicker(context, weeklyTime) { viewModel.setWeeklyCheckInTime(it) }
                    }) { Text(weeklyTime) }
                }
                DropdownMenu(expanded = showWeekdays, onDismissRequest = { showWeekdays = false }) {
                    DayOfWeek.entries.forEach { day ->
                        DropdownMenuItem(
                            text = { Text(stringResource(day.labelResource())) },
                            onClick = {
                                showWeekdays = false
                                viewModel.setWeeklyCheckInDay(day)
                            },
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Switch(
                checked = nudgesEnabled,
                onCheckedChange = { enabled ->
                    updateNotificationSetting("nudges", enabled) { viewModel.setNudgesEnabled(enabled) }
                },
            )
            Text(stringResource(R.string.daily_nudges))
        }
        TextButton(onClick = {
            if (quietStart == null || quietEnd == null) {
                viewModel.setQuietHoursStart("22:00")
                viewModel.setQuietHoursEnd("07:00")
            } else {
                viewModel.setQuietHoursStart(null)
                viewModel.setQuietHoursEnd(null)
            }
        }) {
            Text(stringResource(
                if (quietStart != null && quietEnd != null) R.string.disable_quiet_hours
                else R.string.enable_quiet_hours,
            ))
        }
        if (quietStart != null && quietEnd != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    showTimePicker(context, quietStart.orEmpty()) { viewModel.setQuietHoursStart(it) }
                }) { Text(stringResource(R.string.quiet_hours_start, quietStart.orEmpty())) }
                TextButton(onClick = {
                    showTimePicker(context, quietEnd.orEmpty()) { viewModel.setQuietHoursEnd(it) }
                }) { Text(stringResource(R.string.quiet_hours_end, quietEnd.orEmpty())) }
            }
        }
        Text(stringResource(R.string.check_in_timing_note), style = MaterialTheme.typography.bodySmall)
        if (showPermissionDenied) {
            Text(stringResource(R.string.notification_permission_denied), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = {
                showPermissionDenied = false
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }) { Text(stringResource(R.string.open_notification_settings)) }
        }
        TextButton(onClick = onOpenCheckInHistory) {
            Text(stringResource(R.string.check_in_history))
        }
        Text(stringResource(R.string.oem_battery_help), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null)),
            )
        }) { Text(stringResource(R.string.open_app_info)) }
        Text(stringResource(R.string.drawer_mode), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DrawerMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == drawerMode,
                    onClick = { viewModel.setDrawerMode(mode) },
                    label = { Text(stringResource(mode.labelResource())) },
                )
            }

        }
        Text(stringResource(R.string.category_daily_limits), style = MaterialTheme.typography.titleMedium)
        AppCategory.entries.forEach { category ->
            TextButton(onClick = {
                editingCategory = category
                categoryLimitInput = categoryDailyLimits[category]?.toString() ?: "0"
            }) {
                Text(
                    stringResource(category.labelResource()) + ": " +
                        stringResource(
                            R.string.category_daily_limit_value,
                            categoryDailyLimits[category] ?: 0,
                        ),
                )
            }
        }
        Surface(
            modifier = Modifier.clickable(role = Role.Button) {
                context.startActivity(viewModel.usageAccessSettingsIntent())
            },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.usage_access),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            if (usagePermissionGranted) {
                                R.string.usage_access_granted
                            } else {
                                R.string.usage_access_not_granted
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(
                        if (usagePermissionGranted) R.string.usage_access_explanation
                        else R.string.usage_access_limit_warning,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        TextButton(onClick = onOpenHiddenApps) {
            Text(stringResource(R.string.hidden_apps))
        }
    }
    editingCategory?.let { category ->
        val minutes = categoryLimitInput.toIntOrNull()
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = {
                Text(stringResource(
                    R.string.category_daily_limit_title,
                    stringResource(category.labelResource()),
                ))
            },
            text = {
                OutlinedTextField(
                    value = categoryLimitInput,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit)) categoryLimitInput = value
                    },
                    label = { Text(stringResource(R.string.daily_limit_minutes)) },
                    supportingText = { Text(stringResource(R.string.daily_limit_clear_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = minutes != null && minutes in 0..720,
                    onClick = {
                        viewModel.setCategoryDailyLimit(category, minutes?.takeIf { it > 0 })
                        editingCategory = null
                    },
                ) { Text(stringResource(R.string.save_daily_limit)) }
            },
            dismissButton = {
                TextButton(onClick = { editingCategory = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (showEstimateEditor) {
        val minutes = estimateInput.toIntOrNull()
        AlertDialog(
            onDismissRequest = { showEstimateEditor = false },
            title = { Text(stringResource(R.string.assumed_minutes_title)) },
            text = {
                OutlinedTextField(
                    value = estimateInput,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit)) estimateInput = value
                    },
                    label = { Text(stringResource(R.string.minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = minutes != null && minutes in 1..120,
                    onClick = {
                        viewModel.setAssumedMinutesPerOpen(checkNotNull(minutes))
                        showEstimateEditor = false
                    },
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEstimateEditor = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun NotificationSettingRow(
    label: String,
    enabled: Boolean,
    time: String,
    onEnabledChange: (Boolean) -> Unit,
    onTimeClick: () -> Unit,
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
        Text(label, modifier = Modifier.weight(1f))
        TextButton(onClick = onTimeClick) { Text(time) }
    }
}

private fun showTimePicker(
    context: android.content.Context,
    value: String,
    onSelected: (String) -> Unit,
) {
    val time = LocalTime.parse(value)
    TimePickerDialog(context, { _, hour, minute ->
        onSelected("%02d:%02d".format(hour, minute))
    }, time.hour, time.minute, true).show()
}

private fun applyNotificationSetting(viewModel: SettingsViewModel, setting: String?) {
    when (setting) {
        "morning" -> viewModel.setMorningCheckInEnabled(true)
        "evening" -> viewModel.setEveningCheckInEnabled(true)
        "weekly" -> viewModel.setWeeklyCheckInEnabled(true)
        "nudges" -> viewModel.setNudgesEnabled(true)
    }
}

private fun DrawerMode.labelResource(): Int = when (this) {
    DrawerMode.TEXT -> R.string.drawer_mode_text
    DrawerMode.ICONS -> R.string.drawer_mode_icons
    DrawerMode.GRID -> R.string.drawer_mode_grid
}

private fun AppCategory.labelResource(): Int = when (this) {
    AppCategory.PRODUCTIVE -> R.string.category_productive
    AppCategory.NEUTRAL -> R.string.category_neutral
    AppCategory.DISTRACTING -> R.string.category_distracting
}

private fun DayOfWeek.labelResource(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.monday
    DayOfWeek.TUESDAY -> R.string.tuesday
    DayOfWeek.WEDNESDAY -> R.string.wednesday
    DayOfWeek.THURSDAY -> R.string.thursday
    DayOfWeek.FRIDAY -> R.string.friday
    DayOfWeek.SATURDAY -> R.string.saturday
    DayOfWeek.SUNDAY -> R.string.sunday
}
