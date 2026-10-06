package com.savatech.chimelauncher.feature.goals

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.model.GoalStatus
import java.time.format.DateTimeFormatter

@Composable
fun GoalDetailScreen(
    onEditGoal: (String) -> Unit,
    onStartFocusSession: (String, String?) -> Unit,
    focusTaskId: String? = null,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionRequested by viewModel.notificationPermissionRequested.collectAsStateWithLifecycle()
    var taskBeingEdited by rememberSaveable { mutableStateOf<String?>(null) }
    var showTaskSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteGoalDialog by rememberSaveable { mutableStateOf(false) }
    var reminderPermissionDenied by rememberSaveable { mutableStateOf(false) }
    var pendingTaskSave by remember { mutableStateOf<TaskSaveRequest?>(null) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingTaskSave?.let { request ->
            viewModel.saveTask(
                request.taskId,
                request.title,
                request.recurrence,
                request.daysMask,
                request.reminderTime,
                request.todayValue,
            )
            if (!granted) reminderPermissionDenied = true
        }
        pendingTaskSave = null
    }

    fun saveTask(
        taskId: String?,
        title: String,
        recurrence: com.savatech.chimelauncher.domain.model.Recurrence,
        daysMask: Int,
        reminderTime: String?,
        todayValue: String,
    ) {
        val request = TaskSaveRequest(taskId, title, recurrence, daysMask, reminderTime, todayValue)
        val permissionGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        when {
            reminderTime == null || permissionGranted -> viewModel.saveTask(
                taskId, title, recurrence, daysMask, reminderTime, todayValue,
            )
            !permissionRequested -> {
                pendingTaskSave = request
                viewModel.markNotificationPermissionRequested()
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                reminderPermissionDenied = true
                viewModel.saveTask(taskId, title, recurrence, daysMask, reminderTime, todayValue)
            }
        }
    }

    Scaffold { padding ->
        when {
            state.isLoading -> Text(
                stringResource(R.string.loading_goals),
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            state.goal == null -> Text(
                stringResource(R.string.goal_not_found),
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            else -> {
                val goal = state.goal!!
                val listState = rememberLazyListState()
                LaunchedEffect(state.tasks, focusTaskId) {
                    val taskIndex = state.tasks.indexOfFirst { it.task.id == focusTaskId }
                    if (taskIndex >= 0) listState.animateScrollToItem(taskIndex + 1)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    state = listState,
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(goal.title, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                text = goal.why?.takeIf(String::isNotBlank)
                                    ?: stringResource(R.string.no_goal_why),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(goal.category, style = MaterialTheme.typography.labelLarge)
                            if (state.progress == null) {
                                Text(stringResource(R.string.nothing_scheduled_today))
                            } else {
                                LinearProgressIndicator(
                                    progress = { state.progress!! },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    stringResource(
                                        R.string.goal_progress_percent,
                                        (state.progress!! * 100).toInt(),
                                    ),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onEditGoal(goal.id) }) {
                                    Text(stringResource(R.string.edit_goal))
                                }
                                when (goal.status) {
                                    GoalStatus.ACTIVE -> {
                                        TextButton(onClick = { viewModel.updateStatus(GoalStatus.PAUSED) }) {
                                            Text(stringResource(R.string.pause_goal))
                                        }
                                        TextButton(onClick = { viewModel.updateStatus(GoalStatus.COMPLETED) }) {
                                            Text(stringResource(R.string.complete_goal))
                                        }
                                    }
                                    GoalStatus.PAUSED -> TextButton(
                                        onClick = { viewModel.updateStatus(GoalStatus.ACTIVE) },
                                    ) { Text(stringResource(R.string.resume_goal)) }
                                    else -> Unit
                                }
                                if (goal.status != GoalStatus.ARCHIVED) {
                                    TextButton(onClick = { viewModel.updateStatus(GoalStatus.ARCHIVED) }) {
                                        Text(stringResource(R.string.archive_goal))
                                    }
                                }
                            }
                            TextButton(onClick = { showDeleteGoalDialog = true }) {
                                Text(stringResource(R.string.delete_goal), color = MaterialTheme.colorScheme.error)
                            }
                            OutlinedButton(onClick = { onStartFocusSession(goal.id, null) }) {
                                Text(stringResource(R.string.start_focus_session))
                            }
                            Text(stringResource(R.string.tasks_for_today), style = MaterialTheme.typography.titleLarge)
                            Button(onClick = {
                                taskBeingEdited = null
                                showTaskSheet = true
                            }) { Text(stringResource(R.string.add_task)) }
                        }
                    }
                    items(state.tasks, key = { it.task.id }) { taskState ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = taskState.completedToday,
                                        onCheckedChange = { checked ->
                                            viewModel.toggleToday(taskState.task.id, checked)
                                        },
                                        enabled = taskState.task.isScheduledOn(java.time.LocalDate.now()),
                                    )
                                    Text(
                                        taskState.task.title,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    TextButton(onClick = {
                                        taskBeingEdited = taskState.task.id
                                        showTaskSheet = true
                                    }) { Text(stringResource(R.string.edit_task)) }
                                }
                                Text(
                                    stringResource(
                                        R.string.task_streak_and_freezes,
                                        taskState.streak.currentStreak,
                                        taskState.streak.freezeCredits,
                                    ),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { onStartFocusSession(goal.id, taskState.task.id) }) {
                                        Text(stringResource(R.string.start_focus_session))
                                    }
                                    TextButton(onClick = { viewModel.requestDeleteTask(taskState.task.id) }) {
                                        Text(stringResource(R.string.delete_task))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(stringResource(R.string.recent_logs), style = MaterialTheme.typography.titleLarge)
                    }
                    if (state.recentLogs.isEmpty()) {
                        item { Text(stringResource(R.string.no_recent_logs)) }
                    } else {
                        items(state.recentLogs, key = { it.id }) { log ->
                            val taskTitle = state.tasks.firstOrNull { it.task.id == log.taskId }
                                ?.task?.title.orEmpty()
                            Text(
                                stringResource(
                                    if (log.completed) R.string.log_completed else R.string.log_not_completed,
                                    taskTitle,
                                    log.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                ),
                            )
                        }
                    }
                    state.failure?.let { failure ->
                        item { Text(stringResource(failure.messageResource()), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    if (showTaskSheet) {
        val currentTask = state.tasks.firstOrNull { it.task.id == taskBeingEdited }?.task
        TaskEditSheet(
            task = currentTask,
            hasMeasurableTarget = state.goal?.targetValue != null,
            initialValue = state.tasks.firstOrNull { it.task.id == taskBeingEdited }?.todayValue.orEmpty(),
            onDismiss = { showTaskSheet = false },
            onSave = { title, recurrence, daysMask, reminderTime, todayValue ->
                saveTask(taskBeingEdited, title, recurrence, daysMask, reminderTime, todayValue)
                showTaskSheet = false
            },
        )
    }

    state.deletingTaskId?.let {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteTask,
            title = { Text(stringResource(R.string.delete_task)) },
            text = { Text(stringResource(R.string.confirm_delete_task)) },
            confirmButton = {
                TextButton(onClick = viewModel::deleteTask) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteTask) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showDeleteGoalDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteGoalDialog = false },
            title = { Text(stringResource(R.string.delete_goal)) },
            text = { Text(stringResource(R.string.confirm_delete_goal)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteGoalDialog = false
                    viewModel.deleteGoal()
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGoalDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (reminderPermissionDenied) {
        AlertDialog(
            onDismissRequest = { reminderPermissionDenied = false },
            title = { Text(stringResource(R.string.notification_permission_denied)) },
            text = { Text(stringResource(R.string.task_reminder_permission_help)) },
            confirmButton = {
                TextButton(onClick = {
                    reminderPermissionDenied = false
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                }) { Text(stringResource(R.string.open_notification_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { reminderPermissionDenied = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private data class TaskSaveRequest(
    val taskId: String?,
    val title: String,
    val recurrence: com.savatech.chimelauncher.domain.model.Recurrence,
    val daysMask: Int,
    val reminderTime: String?,
    val todayValue: String,
)

private fun GoalDetailFailure.messageResource(): Int = when (this) {
    GoalDetailFailure.NOT_FOUND -> R.string.goal_not_found
    GoalDetailFailure.UPDATE_FAILED -> R.string.goal_save_failed
    GoalDetailFailure.DELETE_FAILED -> R.string.goal_delete_failed
}
