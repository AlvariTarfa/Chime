package com.savatech.chimelauncher.feature.onboarding

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R

@Composable
fun OnboardingScreen(
    onGoHome: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val categoryOptions = stringArrayResource(R.array.goal_categories)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setNotificationGranted(granted) }
    val homeChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshDefaultHomeStatus() }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDefaultHomeStatus()
                viewModel.refreshUsageAccess()
                viewModel.setNotificationGranted(
                    Build.VERSION.SDK_INT < 33 ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED,
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        viewModel.setNotificationGranted(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(
                    R.string.onboarding_progress,
                    state.step.ordinal + 1,
                    OnboardingStep.entries.size,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        LinearProgressIndicator(
            progress = { (state.step.ordinal + 1).toFloat() / OnboardingStep.entries.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> OnboardingPage(
                    state = state,
                    viewModel = viewModel,
                    onOpenHomeChooser = {
                        val intent = if (Build.VERSION.SDK_INT >= 29) {
                            val roleManager = context.getSystemService(RoleManager::class.java)
                            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                                roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                            } else {
                                Intent(Settings.ACTION_HOME_SETTINGS)
                            }
                        } else {
                            Intent(Settings.ACTION_HOME_SETTINGS)
                        }
                        launchSystemIntent(
                            intent,
                            { homeChooserLauncher.launch(it) },
                            viewModel::reportFailure,
                        )
                    },
                    onOpenUsageSettings = {
                        launchSystemIntent(
                            viewModel.usageSettingsIntent(),
                            { context.startActivity(it) },
                            viewModel::reportFailure,
                        )
                    },
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
        }
        state.failure?.let { failure ->
            Text(
                text = stringResource(
                    when (failure) {
                        OnboardingFailure.LOAD_FAILED -> R.string.onboarding_failure_load
                        OnboardingFailure.GOALS_INVALID -> R.string.onboarding_failure_invalid_goals
                        OnboardingFailure.SAVE_FAILED -> R.string.onboarding_failure_save
                    },
                ),
                color = MaterialTheme.colorScheme.error,
            )
        }
        OnboardingControls(
            state = state,
            viewModel = viewModel,
            category = categoryOptions.lastOrNull().orEmpty(),
            onGoHome = onGoHome,
        )
    }
}

@Composable
private fun OnboardingControls(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
    category: String,
    onGoHome: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.step.previous() != null) {
            TextButton(onClick = viewModel::back) { Text(stringResource(R.string.back)) }
        } else {
            Spacer(Modifier.padding(horizontal = 24.dp))
        }
        when (state.step) {
            OnboardingStep.DONE -> Button(
                onClick = { viewModel.completeOnboarding(onGoHome) },
                enabled = !state.isSaving,
            ) { Text(stringResource(R.string.onboarding_go_home)) }
            OnboardingStep.FIRST_GOALS -> Button(
                onClick = {
                    if (state.hasExistingGoals) viewModel.next()
                    else viewModel.saveGoals(category, viewModel::next)
                },
                enabled = !state.isSaving,
            ) {
                Text(
                    stringResource(
                        if (state.hasExistingGoals) R.string.onboarding_next
                        else R.string.onboarding_save_continue,
                    ),
                )
            }
            OnboardingStep.CLASSIFY_APPS -> Button(
                onClick = { viewModel.saveClassifications(viewModel::next) },
                enabled = !state.isSaving,
            ) { Text(stringResource(R.string.onboarding_save_continue)) }
            OnboardingStep.FRICTION -> Button(
                onClick = { viewModel.saveFrictionLevel(viewModel::next) },
                enabled = !state.isSaving,
            ) { Text(stringResource(R.string.onboarding_next)) }
            else -> Button(onClick = viewModel::next) {
                Text(stringResource(R.string.onboarding_next))
            }
        }
        if (state.step !in setOf(
                OnboardingStep.WELCOME_GOALS,
                OnboardingStep.WELCOME_FRICTION,
                OnboardingStep.DONE,
            )
        ) {
            TextButton(onClick = viewModel::skip) {
                Text(stringResource(R.string.onboarding_skip))
            }
        } else {
            Spacer(Modifier.padding(horizontal = 32.dp))
        }
    }
}

private fun <T> launchSystemIntent(
    intent: Intent,
    launch: (Intent) -> T,
    onFailure: () -> Unit,
) {
    try {
        launch(intent)
    } catch (_: ActivityNotFoundException) {
        onFailure()
    } catch (_: SecurityException) {
        onFailure()
    }
}
