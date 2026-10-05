package com.savatech.chimelauncher.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.settings.DrawerMode
import com.savatech.chimelauncher.domain.model.AppCategory

@Composable
fun SettingsScreen(
    onOpenHiddenApps: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val drawerMode by viewModel.drawerMode.collectAsStateWithLifecycle()
    val categoryDailyLimits by viewModel.categoryDailyLimits.collectAsStateWithLifecycle()
    val usagePermissionGranted by viewModel.usagePermissionGranted.collectAsStateWithLifecycle()
    var editingCategory by remember { mutableStateOf<AppCategory?>(null) }
    var categoryLimitInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshUsagePermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
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
