package com.savatech.chimelauncher.feature.intercept

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.limits.isValidLimitOverrideReason
import kotlinx.coroutines.launch

@Composable
fun InterceptScreen(
    onFinish: () -> Unit,
    viewModel: InterceptViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingGrantMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        val minutes = pendingGrantMinutes
        pendingGrantMinutes = null
        if (minutes != null) {
            scope.launch {
                if (viewModel.openFor(minutes)) onFinish()
            }
        }
    }
    val back: () -> Unit = {
        scope.launch {
            if (viewModel.goBack()) onFinish()
        }
        Unit
    }
    BackHandler(onBack = back)

    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathing-scale",
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(state.appLabel, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Spacer(Modifier.height(32.dp))
            val limitMinutes = state.limitMinutes
            val isLimitReached = state.isLimitReached
            Text(
                stringResource(
                    if (isLimitReached) R.string.daily_limit_reached else R.string.intercept_question,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            if (isLimitReached) {
                Text(
                    stringResource(
                        R.string.daily_limit_usage,
                        state.usedMillis / 60_000L,
                        limitMinutes ?: 0,
                    ),
                )
                OutlinedTextField(
                    value = state.limitOverrideReason,
                    onValueChange = viewModel::updateLimitOverrideReason,
                    label = { Text(stringResource(R.string.limit_override_reason)) },
                    singleLine = false,
                )
            }
            state.priorityGoalTitle?.let { title ->
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.intercept_priority_goal, title))
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.intercept_countdown, state.remainingSeconds))
            if (state.launchFailed) {
                Text(
                    stringResource(R.string.launch_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.actionFailed) {
                Text(
                    stringResource(R.string.intercept_action_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(16.dp))
            (if (isLimitReached) listOf(5, 10) else listOf(5, 10, 15)).forEach { minutes ->
                Button(
                    enabled = state.remainingSeconds == 0 && !state.isBusy &&
                        (!isLimitReached || isValidLimitOverrideReason(state.limitOverrideReason)),
                    onClick = {
                        if (
                            Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                PackageManager.PERMISSION_GRANTED
                        ) {
                            scope.launch {
                                if (viewModel.shouldRequestNotificationPermission()) {
                                    viewModel.markNotificationPermissionRequested()
                                    pendingGrantMinutes = minutes
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    if (viewModel.openFor(minutes)) onFinish()
                                }
                            }
                        } else {
                            scope.launch {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    viewModel.markNotificationPermissionRequested()
                                }
                                if (viewModel.openFor(minutes)) onFinish()
                            }
                        }
                    },
                ) {
                    Text(
                        if (state.remainingSeconds > 0) {
                            stringResource(R.string.intercept_grant_countdown, minutes, state.remainingSeconds)
                        } else {
                            stringResource(R.string.intercept_open_minutes, minutes)
                        },
                    )
                }
            }
            Button(
                enabled = state.remainingSeconds == 0 && !state.isBusy &&
                    (!isLimitReached || isValidLimitOverrideReason(state.limitOverrideReason)),
                onClick = { scope.launch { if (viewModel.openOnce()) onFinish() } },
            ) {
                Text(
                    if (state.remainingSeconds > 0) {
                        stringResource(R.string.intercept_continue_countdown, state.remainingSeconds)
                    } else {
                        stringResource(R.string.intercept_continue_once)
                    },
                )
            }
            TextButton(onClick = back) {
                Text(stringResource(R.string.intercept_go_back))
            }
        }
    }
}
