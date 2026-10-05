package com.savatech.chimelauncher.feature.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.db.entities.FocusMode

@Composable
fun FocusModesScreen(
    onBack: () -> Unit,
    viewModel: FocusModeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FocusMode?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
                Text(stringResource(R.string.focus_modes), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = { creating = true }) { Text(stringResource(R.string.add_focus_mode)) }
            }
            state.activeMode?.let { mode ->
                Text(stringResource(R.string.active_focus_mode, mode.name))
                TextButton(onClick = viewModel::endMode) { Text(stringResource(R.string.end_focus_mode)) }
            }
            if (state.error) {
                Text(
                    stringResource(R.string.focus_mode_action_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.modes, key = FocusMode::id) { mode ->
                    Card {
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.setMode(mode.id) }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(mode.name)
                                Text(
                                    if (mode.scheduleJson == null) stringResource(R.string.no_focus_schedule)
                                    else stringResource(R.string.focus_schedule_set),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            TextButton(onClick = { editing = mode }) {
                                Text(stringResource(R.string.edit_focus_mode))
                            }
                        }
                    }
                }
            }
        }
    }

    val selected = editing
    if (creating || selected != null) {
        FocusModeEditor(
            mode = selected,
            apps = state.apps,
            query = state.query,
            onQueryChange = viewModel::updateQuery,
            onDismiss = {
                editing = null
                creating = false
            },
            onSave = { name, packages, schedule, suppress ->
                viewModel.saveMode(selected?.id, name, packages, schedule, suppress)
                editing = null
                creating = false
            },
            onDelete = selected?.takeUnless(FocusMode::isBuiltIn)?.let { mode ->
                { viewModel.deleteMode(mode); editing = null }
            },
        )
    }
}

@Composable
fun FocusQuickToggle(
    onManageModes: () -> Unit,
    viewModel: FocusModeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    Column {
        androidx.compose.foundation.layout.Box {
            FilterChip(
                selected = state.activeMode != null,
                onClick = { expanded = true },
                label = {
                    Text(
                        state.activeMode?.let { stringResource(R.string.active_focus_mode, it.name) }
                            ?: stringResource(R.string.choose_focus_mode),
                    )
                },
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.modes.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.name) },
                        onClick = {
                            viewModel.setMode(mode.id)
                            expanded = false
                        },
                    )
                }
                if (state.activeMode != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.end_focus_mode)) },
                        onClick = {
                            viewModel.endMode()
                            expanded = false
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.manage_focus_modes)) },
                    onClick = {
                        expanded = false
                        onManageModes()
                    },
                )
            }
        }
        if (state.error) {
            Text(stringResource(R.string.focus_mode_action_failed), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun FocusModeDrawerBanner(viewModel: FocusModeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.activeMode != null || state.error) {
        Column {
            state.activeMode?.let { mode ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.active_focus_mode, mode.name),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    TextButton(onClick = viewModel::pauseFor15Minutes) {
                        Text(stringResource(R.string.pause_mode_15_minutes))
                    }
                }
            }
            if (state.error) {
                Text(
                    stringResource(R.string.focus_mode_action_failed),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
