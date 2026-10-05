package com.savatech.chimelauncher.feature.drawer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.IconCache
import com.savatech.chimelauncher.data.db.entities.AppConfig
import com.savatech.chimelauncher.domain.model.AppCategory
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppActionsSheet(
    app: AppInfo,
    config: AppConfig?,
    iconCache: IconCache,
    onDismiss: () -> Unit,
    onPin: (Boolean) -> Unit,
    onHide: () -> Unit,
    onCategory: (AppCategory) -> Unit,
    onDailyLimit: (Int?) -> Unit,
    onSystemActionFailed: () -> Unit,
) {
    val context = LocalContext.current
    val goalLinkViewModel: GoalLinkViewModel = hiltViewModel()
    val goalLinkState by goalLinkViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var showGoalChoices by remember { mutableStateOf(false) }
    var showDailyLimitDialog by remember { mutableStateOf(false) }
    var dailyLimitInput by remember(config?.dailyLimitMin) {
        mutableStateOf(config?.dailyLimitMin?.toString() ?: "0")
    }
    var linkedGoalId by remember(config?.linkedGoalId) {
        mutableStateOf(config?.linkedGoalId)
    }
    val isPinned = config?.pinned == true
    val category = AppCategory.fromStorage(config?.category ?: AppCategory.NEUTRAL.name)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppIcon(app, iconCache, 40.dp)
                Text(app.label, style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = { onPin(!isPinned); onDismiss() }) {
                Text(stringResource(if (isPinned) R.string.unpin_from_dock else R.string.pin_to_dock))
            }
            TextButton(onClick = onHide) { Text(stringResource(R.string.hide_app)) }
            TextButton(onClick = { showGoalChoices = !showGoalChoices }) {
                Text(stringResource(R.string.link_to_goal))
            }
            if (showGoalChoices) {
                TextButton(onClick = {
                    coroutineScope.launch {
                        if (goalLinkViewModel.setLinkedGoal(app.packageName, null)) {
                            linkedGoalId = null
                        }
                    }
                }) { Text(stringResource(R.string.no_linked_goal)) }
                goalLinkState.activeGoals.forEach { goal ->
                    TextButton(onClick = {
                        coroutineScope.launch {
                            if (goalLinkViewModel.setLinkedGoal(app.packageName, goal.id)) {
                                linkedGoalId = goal.id
                            }
                        }
                    }) {
                        Text(
                            stringResource(
                                if (goal.id == linkedGoalId) R.string.linked_goal_choice
                                else R.string.goal_choice,
                                goal.title,
                            ),
                        )
                    }
                }
                if (goalLinkState.failure) {
                    Text(
                        stringResource(R.string.link_goal_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            TextButton(onClick = { showDailyLimitDialog = true }) {
                Text(stringResource(R.string.daily_limit))
            }
            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(AppCategory.entries) { option ->
                    TextButton(onClick = { onCategory(option) }) {
                        Text(
                            text = stringResource(option.labelResource()),
                            color = if (option == category) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            TextButton(onClick = {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:${app.packageName}")),
                    )
                } catch (_: ActivityNotFoundException) {
                    onSystemActionFailed()
                } catch (_: SecurityException) {
                    onSystemActionFailed()
                }
                onDismiss()
            }) { Text(stringResource(R.string.app_info)) }
            if (!app.isSystemApp) {
                TextButton(onClick = {
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_DELETE).setData(Uri.parse("package:${app.packageName}")),
                        )
                    } catch (_: ActivityNotFoundException) {
                        onSystemActionFailed()
                    } catch (_: SecurityException) {
                        onSystemActionFailed()
                    }
                    onDismiss()
                }) { Text(stringResource(R.string.uninstall_app)) }
            }
        }
    }
    if (showDailyLimitDialog) {
        val minutes = dailyLimitInput.toIntOrNull()
        AlertDialog(
            onDismissRequest = { showDailyLimitDialog = false },
            title = { Text(stringResource(R.string.daily_limit)) },
            text = {
                OutlinedTextField(
                    value = dailyLimitInput,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit)) dailyLimitInput = value
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
                        onDailyLimit(minutes?.takeIf { it > 0 })
                        showDailyLimitDialog = false
                    },
                ) { Text(stringResource(R.string.save_daily_limit)) }
            },
            dismissButton = {
                TextButton(onClick = { showDailyLimitDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

private fun AppCategory.labelResource(): Int = when (this) {
    AppCategory.PRODUCTIVE -> R.string.category_productive
    AppCategory.NEUTRAL -> R.string.category_neutral
    AppCategory.DISTRACTING -> R.string.category_distracting
}
