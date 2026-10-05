package com.savatech.chimelauncher.feature.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.db.entities.FocusMode
import com.savatech.chimelauncher.domain.focus.FocusSchedule
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FocusModeEditor(
    mode: FocusMode?,
    apps: List<AppInfo>,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, Set<String>, FocusSchedule?, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val weekdayNames = stringArrayResource(R.array.weekdays)
    val initialSchedule = remember(mode) { mode?.scheduleJson?.let(FocusSchedule::fromJson) }
    var name by remember(mode) { mutableStateOf(mode?.name.orEmpty()) }
    var selectedPackages by remember(mode) {
        mutableStateOf(mode?.let { decodePackageList(it.allowedPackages) } ?: emptySet())
    }
    var scheduleEnabled by remember(mode) { mutableStateOf(initialSchedule != null) }
    var selectedDays by remember(mode) {
        mutableStateOf(initialSchedule?.days ?: DayOfWeek.entries.toSet())
    }
    var start by remember(mode) { mutableStateOf(initialSchedule?.start?.toString() ?: "09:00") }
    var end by remember(mode) { mutableStateOf(initialSchedule?.end?.toString() ?: "17:00") }
    var suppress by remember(mode) { mutableStateOf(mode?.suppressNotifications ?: false) }
    var pickingTime by remember { mutableStateOf(false) }
    var startPicker by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (mode == null) R.string.add_focus_mode else R.string.edit_focus_mode)) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.focus_mode_name)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        label = { Text(stringResource(R.string.search_apps)) },
                        singleLine = true,
                    )
                    Text(stringResource(R.string.allowed_apps), style = MaterialTheme.typography.titleSmall)
                }
                items(apps, key = AppInfo::key) { app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = app.packageName in selectedPackages,
                            onCheckedChange = { checked ->
                                selectedPackages = if (checked) selectedPackages + app.packageName
                                else selectedPackages - app.packageName
                            },
                        )
                        Text(app.label)
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.focus_schedule))
                        Switch(scheduleEnabled, onCheckedChange = { scheduleEnabled = it })
                    }
                    if (scheduleEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            DayOfWeek.entries.forEach { day ->
                                FilterChip(
                                    selected = day in selectedDays,
                                    onClick = {
                                        selectedDays = if (day in selectedDays) selectedDays - day
                                        else selectedDays + day
                                    },
                                    label = { Text(weekdayNames[day.value - 1]) },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { startPicker = true; pickingTime = true }) {
                                Text(stringResource(R.string.focus_start_time, start))
                            }
                            OutlinedButton(onClick = { startPicker = false; pickingTime = true }) {
                                Text(stringResource(R.string.focus_end_time, end))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.suppress_notifications))
                        Switch(suppress, onCheckedChange = { suppress = it })
                    }
                    if (error) {
                        Text(stringResource(R.string.focus_mode_invalid), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedStart = runCatchingTime(start)
                val parsedEnd = runCatchingTime(end)
                if (
                    name.isBlank() ||
                    (scheduleEnabled && (selectedDays.isEmpty() || parsedStart == null || parsedEnd == null))
                ) {
                    error = true
                } else {
                    val schedule = if (scheduleEnabled) {
                        FocusSchedule(selectedDays, requireNotNull(parsedStart), requireNotNull(parsedEnd))
                    } else {
                        null
                    }
                    onSave(name, selectedPackages, schedule, suppress)
                }
            }) { Text(stringResource(R.string.save_focus_mode)) }
        },
        dismissButton = {
            Row {
                onDelete?.let { TextButton(onClick = it) { Text(stringResource(R.string.delete_focus_mode)) } }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
    if (pickingTime) {
        val initial = LocalTime.parse(if (startPicker) start else end)
        val pickerState = rememberTimePickerState(initial.hour, initial.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = {
                Text(stringResource(if (startPicker) R.string.focus_start_time_title else R.string.focus_end_time_title))
            },
            text = { TimePicker(pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val value = "%02d:%02d".format(pickerState.hour, pickerState.minute)
                    if (startPicker) start = value else end = value
                    pickingTime = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private fun runCatchingTime(value: String): LocalTime? =
    try {
        LocalTime.parse(value)
    } catch (_: java.time.format.DateTimeParseException) {
        null
    }
