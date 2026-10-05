package com.savatech.chimelauncher.feature.focus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.savatech.chimelauncher.R

@Composable
fun FocusSessionScreen(
    goalId: String?,
    taskId: String?,
    onBack: () -> Unit,
    viewModel: FocusSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var customMinutes by remember { mutableStateOf("") }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.start(goalId, taskId, selectedMinutes)
    }
    val presets = listOf(25, 50)
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.start_focus_session), style = MaterialTheme.typography.headlineMedium)
            if (state.completed) {
                Text(stringResource(R.string.session_complete), style = MaterialTheme.typography.titleLarge)
            }
            if (state.session == null) {
                Text(stringResource(R.string.focus_session_presets))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { minutes ->
                        FilterChip(
                            selected = selectedMinutes == minutes,
                            onClick = {
                                selectedMinutes = minutes
                                customMinutes = ""
                            },
                            label = {
                                Text(stringResource(if (minutes == 25) R.string.focus_session_25_5 else R.string.focus_session_50_10))
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = {
                        val value = it.filter(Char::isDigit)
                        customMinutes = value
                        selectedMinutes = value.toIntOrNull()
                            ?.takeIf { minutes -> minutes in 1..240 } ?: 0
                    },
                    label = { Text(stringResource(R.string.focus_session_custom)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                Button(
                    enabled = selectedMinutes in 1..240,
                    onClick = {
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.start(goalId, taskId, selectedMinutes)
                        }
                    },
                ) { Text(stringResource(R.string.focus_session_start)) }
            } else {
                val minutes = state.remainingMillis / 60_000
                val seconds = (state.remainingMillis % 60_000) / 1_000
                Text(stringResource(R.string.focus_session_active), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.focus_session_remaining, minutes.toInt(), seconds.toInt()))
                TextButton(onClick = viewModel::endEarly) {
                    Text(stringResource(R.string.focus_session_end_early))
                }
            }
            if (state.error) {
                Text(stringResource(R.string.focus_session_action_failed), color = MaterialTheme.colorScheme.error)
            }
            if (customMinutes.isNotEmpty() && selectedMinutes == 0) {
                Text(stringResource(R.string.focus_session_invalid_minutes), color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
        }
    }
}
