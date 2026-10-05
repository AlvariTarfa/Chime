package com.savatech.chimelauncher.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.model.Recurrence
import com.savatech.chimelauncher.domain.model.TaskModel
import java.time.LocalTime
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TaskEditSheet(
    task: TaskModel?,
    hasMeasurableTarget: Boolean,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String, Recurrence, Int, String?, String) -> Unit,
) {
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var recurrenceName by rememberSaveable(task?.id) {
        mutableStateOf((task?.recurrence ?: Recurrence.DAILY).name)
    }
    var daysMask by rememberSaveable(task?.id) { mutableStateOf(task?.daysMask ?: 0) }
    var reminderTime by rememberSaveable(task?.id) { mutableStateOf(task?.reminderTime) }
    var todayValue by rememberSaveable(task?.id) { mutableStateOf(initialValue) }
    var showRecurrenceMenu by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var titleError by rememberSaveable { mutableStateOf(false) }
    var scheduleError by rememberSaveable { mutableStateOf(false) }
    var valueError by rememberSaveable { mutableStateOf(false) }
    val weekdays = stringArrayResource(R.array.weekdays)
    val recurrence = Recurrence.valueOf(recurrenceName)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(if (task == null) R.string.add_task else R.string.edit_task))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; titleError = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.task_title_label)) },
                isError = titleError,
                supportingText = {
                    if (titleError) Text(stringResource(R.string.task_title_required))
                },
                singleLine = true,
            )
            Column {
                OutlinedButton(onClick = { showRecurrenceMenu = true }) {
                    Text(stringResource(if (recurrence == Recurrence.DAILY) {
                        R.string.recurrence_daily
                    } else {
                        R.string.recurrence_weekdays
                    }))
                }
                DropdownMenu(
                    expanded = showRecurrenceMenu,
                    onDismissRequest = { showRecurrenceMenu = false },
                ) {
                    Recurrence.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(if (option == Recurrence.DAILY) {
                                    R.string.recurrence_daily
                                } else {
                                    R.string.recurrence_weekdays
                                }))
                            },
                            onClick = {
                                recurrenceName = option.name
                                showRecurrenceMenu = false
                            },
                        )
                    }
                }
            }
            if (recurrence == Recurrence.DAYS_OF_WEEK) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    weekdays.forEachIndexed { index, day ->
                        val selected = daysMask and (1 shl index) != 0
                        FilterChip(
                            selected = selected,
                            onClick = {
                                daysMask = if (selected) daysMask and (1 shl index).inv()
                                else daysMask or (1 shl index)
                                scheduleError = false
                            },
                            label = { Text(day) },
                        )
                    }
                    if (scheduleError) Text(
                        stringResource(R.string.choose_at_least_one_day),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(
                        reminderTime?.let { stringResource(R.string.reminder_at, it) }
                            ?: stringResource(R.string.add_reminder_time),
                    )
                }
                if (reminderTime != null) {
                    TextButton(onClick = { reminderTime = null }) {
                        Text(stringResource(R.string.clear_reminder))
                    }
                }
            }
            if (hasMeasurableTarget) {
                OutlinedTextField(
                    value = todayValue,
                    onValueChange = {
                        todayValue = it.filter { character ->
                            character.isDigit() || character == '.'
                        }
                        valueError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.today_value)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = valueError,
                    supportingText = {
                        if (valueError) Text(stringResource(R.string.today_value_invalid))
                    },
                    singleLine = true,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = {
                    titleError = title.isBlank()
                    scheduleError = recurrence == Recurrence.DAYS_OF_WEEK && daysMask == 0
                    valueError = hasMeasurableTarget && todayValue.isNotBlank() &&
                        (todayValue.toDoubleOrNull()?.let { it.isFinite() && it >= 0.0 } != true)
                    if (!titleError && !scheduleError &&
                        !valueError
                    ) {
                        onSave(title, recurrence, daysMask, reminderTime, todayValue)
                    }
                }) { Text(stringResource(R.string.save_task)) }
            }
        }
    }

    if (showTimePicker) {
        val initialTime = reminderTime?.let(LocalTime::parse) ?: LocalTime.NOON
        val pickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    reminderTime = String.format(
                        Locale.ROOT,
                        "%02d:%02d",
                        pickerState.hour,
                        pickerState.minute,
                    )
                    showTimePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
