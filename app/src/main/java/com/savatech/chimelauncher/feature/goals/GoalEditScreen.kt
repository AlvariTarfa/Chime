package com.savatech.chimelauncher.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.model.GoalStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun GoalEditScreen(
    onBack: () -> Unit,
    viewModel: GoalEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories = stringArrayResource(R.array.goal_categories).toList()
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var confirmation by rememberSaveable { mutableStateOf<GoalConfirmation?>(null) }

    LaunchedEffect(categories) {
        if (state.category.isBlank()) viewModel.updateCategory(categories.last())
    }
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (state.goalId == null) R.string.create_goal else R.string.edit_goal),
                style = MaterialTheme.typography.headlineMedium,
            )
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goal_title_label)) },
                supportingText = {
                    when (state.titleError) {
                        GoalTitleError.REQUIRED -> Text(stringResource(R.string.goal_title_required))
                        GoalTitleError.TOO_LONG -> Text(stringResource(R.string.goal_title_too_long))
                        null -> Text(stringResource(R.string.character_count, state.title.length, 80))
                    }
                },
                isError = state.titleError != null,
                singleLine = true,
            )
            OutlinedTextField(
                value = state.why,
                onValueChange = viewModel::updateWhy,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.goal_why_label)) },
                supportingText = {
                    if (state.whyError) Text(stringResource(R.string.goal_why_too_long))
                    else Text(stringResource(R.string.character_count, state.why.length, 300))
                },
                isError = state.whyError,
                minLines = 2,
            )
            Column {
                OutlinedButton(onClick = { categoryMenuExpanded = true }) {
                    Text(stringResource(R.string.goal_category_value, state.category))
                }
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                viewModel.updateCategory(category)
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(
                        state.targetDate ?: stringResource(R.string.choose_target_date),
                    )
                }
                if (state.targetDate != null) {
                    TextButton(onClick = { viewModel.updateTargetDate(null) }) {
                        Text(stringResource(R.string.clear_target_date))
                    }
                }
            }
            Text(stringResource(R.string.measurable_target_optional))
            OutlinedTextField(
                value = state.unit,
                onValueChange = viewModel::updateUnit,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.target_unit)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = state.targetValue,
                onValueChange = viewModel::updateTargetValue,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.target_value)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = state.measurableTargetError,
                supportingText = {
                    if (state.measurableTargetError) {
                        Text(stringResource(R.string.target_value_invalid))
                    }
                },
                singleLine = true,
            )
            state.failure?.let { failure ->
                Text(stringResource(failure.messageResource()), color = MaterialTheme.colorScheme.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.save(categories) },
                    enabled = !state.isLoading,
                ) {
                    Text(stringResource(R.string.save_goal))
                }
                TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
            }
            if (state.goalId != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        confirmation = GoalConfirmation.ARCHIVE
                    }) { Text(stringResource(R.string.archive_goal)) }
                    TextButton(onClick = { confirmation = GoalConfirmation.DELETE }) {
                        Text(stringResource(R.string.delete_goal), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = state.targetDate?.let(LocalDate::parse)
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.updateTargetDate(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) { DatePicker(state = pickerState) }
    }

    confirmation?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = {
                Text(stringResource(if (action == GoalConfirmation.DELETE) R.string.delete_goal else R.string.archive_goal))
            },
            text = {
                Text(stringResource(if (action == GoalConfirmation.DELETE) {
                    R.string.confirm_delete_goal
                } else {
                    R.string.confirm_archive_goal
                }))
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmation = null
                    if (action == GoalConfirmation.DELETE) viewModel.deleteGoal()
                    else viewModel.setStatus(GoalStatus.ARCHIVED)
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private enum class GoalConfirmation { ARCHIVE, DELETE }

private fun GoalEditFailure.messageResource(): Int = when (this) {
    GoalEditFailure.NOT_FOUND -> R.string.goal_not_found
    GoalEditFailure.SAVE_FAILED -> R.string.goal_save_failed
    GoalEditFailure.DELETE_FAILED -> R.string.goal_delete_failed
}
