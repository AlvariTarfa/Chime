package com.savatech.chimelauncher.feature.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.apps.AppConfigResult
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.settings.DrawerMode
import com.savatech.chimelauncher.core.launch.LaunchResult
import com.savatech.chimelauncher.core.launch.PauseLaunchArgs
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.feature.drawer.AppActionsSheet
import com.savatech.chimelauncher.feature.limits.DailyLimitWarningEffect
import com.savatech.chimelauncher.feature.focus.FocusQuickToggle
import com.savatech.chimelauncher.feature.focus.FocusModeViewModel
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGoals: () -> Unit,
    onCreateGoal: () -> Unit,
    onOpenPriorities: () -> Unit,
    onOpenGoal: (String) -> Unit,
    onNeedsPause: (PauseLaunchArgs) -> Unit,
    onManageFocusModes: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    focusModeViewModel: FocusModeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusState by focusModeViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val launchFailureMessage = stringResource(R.string.launch_failed)
    val pinLimitMessage = stringResource(R.string.pin_limit_reached)
    val appActionFailedMessage = stringResource(R.string.app_action_failed)
    val taskUpdateFailedMessage = stringResource(R.string.task_update_failed)
    val locale = LocalConfiguration.current.locales[0]
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, MMMM d", locale) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showHomeMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.taskUpdateFailed) {
        if (uiState.taskUpdateFailed) {
            snackbarHostState.showSnackbar(taskUpdateFailedMessage)
            viewModel.dismissTaskUpdateFailure()
        }
    }
    DailyLimitWarningEffect(snackbarHostState, viewModel::dailyLimitWarnings)

    fun launchApp(app: AppInfo) {
        coroutineScope.launch {
            when (val result = viewModel.requestLaunch(app)) {
                LaunchResult.Started -> Unit
                is LaunchResult.NeedsPause -> onNeedsPause(result.args)
                is LaunchResult.Failed -> snackbarHostState.showSnackbar(
                    result.error.localizedMessage ?: launchFailureMessage,
                )
            }
        }
    }

    fun setPinned(app: AppInfo, pinned: Boolean) {
        coroutineScope.launch {
            if (viewModel.setPinned(app.packageName, pinned) == AppConfigResult.PinLimitExceeded) {
                snackbarHostState.showSnackbar(pinLimitMessage)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .combinedClickable(onClick = {}, onLongClick = { showHomeMenu = true })
                .pointerInput(onOpenDrawer) {
                    var dragDistance = 0f
                    var drawerOpened = false
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (!drawerOpened) dragDistance += dragAmount
                            if (!drawerOpened && dragDistance < -80.dp.toPx()) {
                                drawerOpened = true
                                onOpenDrawer()
                            }
                        },
                        onDragEnd = {
                            dragDistance = 0f
                            drawerOpened = false
                        },
                        onDragCancel = {
                            dragDistance = 0f
                            drawerOpened = false
                        },
                    )
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FocusQuickToggle(
                    onManageModes = onManageFocusModes,
                    viewModel = focusModeViewModel,
                )
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ScrimCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            val localTime = uiState.currentTime.toLocalDateTime()
                            Text(
                                localTime.format(timeFormatter),
                                style = MaterialTheme.typography.displayLarge,
                            )
                            Text(localTime.format(dateFormatter), style = MaterialTheme.typography.titleMedium)
                            uiState.activeFocusModeName?.let { mode ->
                                Text(
                                    stringResource(R.string.active_focus_mode, mode),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }

                    if (focusState.activeMode?.id == "builtin-sleep") {
                        ScrimCard {
                            Column(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(stringResource(R.string.wind_down_title), style = MaterialTheme.typography.headlineSmall)
                                Text(stringResource(R.string.wind_down_message))
                                focusState.tomorrowPriorityTitle?.let { title ->
                                    Text(stringResource(R.string.tomorrow_priority, title))
                                }
                                Button(onClick = onOpenDrawer) {
                                    Text(stringResource(R.string.open_allowed_apps))
                                }
                            }
                        }
                    } else {
                        ScrimCard {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(stringResource(R.string.todays_focus), style = MaterialTheme.typography.titleLarge)
                                if (uiState.priorityGoals.isEmpty()) {
                                    Text(stringResource(R.string.no_priorities_prompt))
                                    Button(onClick = {
                                        if (uiState.hasGoals) onOpenPriorities() else onCreateGoal()
                                    }) {
                                        Text(stringResource(if (uiState.hasGoals) R.string.choose_priorities else R.string.create_goal))
                                    }
                                } else {
                                    uiState.priorityGoals.forEach { priority ->
                                        key(priority.overview.goal.id) {
                                            PriorityGoalContent(
                                                overview = priority.overview,
                                                unfinishedTasks = priority.unfinishedTasks,
                                                onOpenGoal = { onOpenGoal(priority.overview.goal.id) },
                                                onTaskChecked = viewModel::setTaskCompleted,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        uiState.nextTask?.let { next ->
                            ScrimCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.next_task), style = MaterialTheme.typography.titleMedium)
                                        Text(next.task.task.title, style = MaterialTheme.typography.bodyLarge)
                                        Text(next.goalTitle, style = MaterialTheme.typography.labelMedium)
                                    }
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = { checked ->
                                            viewModel.setTaskCompleted(next.task.task.id, checked)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.pinnedApps.isNotEmpty()) {
                    ScrimCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            uiState.pinnedApps.forEach { app ->
                                key(app.key) {
                                    DockAppItem(
                                        app = app,
                                        showIcon = uiState.drawerMode != DrawerMode.TEXT,
                                        iconCache = viewModel.iconCache,
                                        onClick = { launchApp(app) },
                                        onLongClick = { selectedApp = app },
                                    )
                                }
                            }
                        }
                    }
                }

                ScrimCard {
                    TextButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                    Text(stringResource(R.string.open_apps))
                    }
                }
            }
            DropdownMenu(
                expanded = showHomeMenu,
                onDismissRequest = { showHomeMenu = false },
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings_title)) },
                    onClick = {
                        showHomeMenu = false
                        onOpenSettings()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.manage_focus_modes)) },
                    onClick = {
                        showHomeMenu = false
                        onManageFocusModes()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.goals_title)) },
                    onClick = {
                        showHomeMenu = false
                        onOpenGoals()
                    },
                )
            }
        }
    }

    selectedApp?.let { app ->
        val iconCache = viewModel.iconCache
        if (iconCache != null) {
            AppActionsSheet(
                app = app,
                config = uiState.configs[app.packageName],
                iconCache = iconCache,
                onDismiss = { selectedApp = null },
                onPin = { pinned -> setPinned(app, pinned) },
                onHide = {
                    coroutineScope.launch { viewModel.hide(app.packageName) }
                    selectedApp = null
                },
                onCategory = { category: AppCategory ->
                    coroutineScope.launch { viewModel.setCategory(app.packageName, category) }
                    selectedApp = null
                },
                onDailyLimit = { minutes ->
                    coroutineScope.launch { viewModel.setDailyLimit(app.packageName, minutes) }
                },
                onSystemActionFailed = {
                    coroutineScope.launch { snackbarHostState.showSnackbar(appActionFailedMessage) }
                },
            )
        }
    }
}
