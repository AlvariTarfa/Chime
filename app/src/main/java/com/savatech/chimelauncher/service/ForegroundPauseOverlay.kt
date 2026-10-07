package com.savatech.chimelauncher.service

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.core.launch.PauseLaunchArgs
import com.savatech.chimelauncher.domain.intercept.PauseReason
import com.savatech.chimelauncher.domain.limits.isValidLimitOverrideReason
import kotlinx.coroutines.delay

internal class AccessibilityOverlayOwners :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun resume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}

@Composable
internal fun ForegroundPauseOverlay(
    args: PauseLaunchArgs,
    onGoBack: (PauseLaunchArgs) -> Unit,
    onContinue: (Int?, String, () -> Unit) -> Unit,
    onShown: () -> Unit,
) {
    val endAt = remember(args) { SystemClock.elapsedRealtime() + args.delaySeconds * 1_000L }
    var remainingSeconds by remember(args) { mutableIntStateOf(args.delaySeconds) }
    var overrideReason by remember(args) { mutableStateOf("") }
    var actionFailed by remember(args) { mutableStateOf(false) }
    LaunchedEffect(args) {
        onShown()
        while (remainingSeconds > 0) {
            remainingSeconds = ((endAt - SystemClock.elapsedRealtime() + 999L) / 1_000L)
                .coerceAtLeast(0L).toInt()
            if (remainingSeconds > 0) delay(200L)
        }
    }
    val limitReached = args.reason == PauseReason.LIMIT_REACHED
    val canContinue = remainingSeconds == 0 &&
        (!limitReached || isValidLimitOverrideReason(overrideReason))
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(args.appLabel, style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(if (limitReached) R.string.daily_limit_reached else R.string.intercept_question),
                style = MaterialTheme.typography.titleLarge,
            )
            if (limitReached) {
                Text(
                    stringResource(
                        R.string.daily_limit_usage,
                        args.usedMillis / 60_000L,
                        args.limitMinutes ?: 0,
                    ),
                )
                OutlinedTextField(
                    value = overrideReason,
                    onValueChange = { overrideReason = it },
                    label = { Text(stringResource(R.string.limit_override_reason)) },
                )
            }
            Text(stringResource(R.string.intercept_countdown, remainingSeconds))
            if (actionFailed) {
                Text(
                    stringResource(R.string.intercept_action_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            (if (limitReached) listOf(5, 10) else listOf(5, 10, 15)).forEach { minutes ->
                Button(
                    enabled = canContinue,
                    onClick = { onContinue(minutes, overrideReason) { actionFailed = true } },
                ) {
                    Text(
                        if (remainingSeconds > 0) {
                            stringResource(R.string.intercept_grant_countdown, minutes, remainingSeconds)
                        } else {
                            stringResource(R.string.intercept_open_minutes, minutes)
                        },
                    )
                }
            }
            Button(
                enabled = canContinue,
                onClick = { onContinue(null, overrideReason) { actionFailed = true } },
            ) {
                Text(
                    if (remainingSeconds > 0) {
                        stringResource(R.string.intercept_continue_countdown, remainingSeconds)
                    } else {
                        stringResource(R.string.intercept_continue_once)
                    },
                )
            }
            TextButton(onClick = { onGoBack(args) }) {
                Text(stringResource(R.string.intercept_go_back))
            }
        }
    }
}
