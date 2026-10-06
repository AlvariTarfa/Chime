package com.savatech.chimelauncher.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.settings.FrictionLevel

@Composable
fun OnboardingPage(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
    onOpenHomeChooser: () -> Unit,
    onOpenUsageSettings: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    when (state.step) {
        OnboardingStep.WELCOME_GOALS -> WelcomePage(
            R.string.onboarding_welcome_goals_title,
            R.string.onboarding_welcome_goals_body,
        )
        OnboardingStep.WELCOME_FRICTION -> WelcomePage(
            R.string.onboarding_welcome_friction_title,
            R.string.onboarding_welcome_friction_body,
        )
        OnboardingStep.DEFAULT_LAUNCHER -> PermissionPage(
            title = R.string.onboarding_default_launcher_title,
            body = R.string.onboarding_default_launcher_body,
            status = if (state.isDefaultHome) R.string.onboarding_status_done
                else R.string.onboarding_status_not_done,
            action = R.string.onboarding_open_home_chooser,
            onAction = onOpenHomeChooser,
        )
        OnboardingStep.USAGE_ACCESS -> PermissionPage(
            title = R.string.onboarding_usage_title,
            body = R.string.onboarding_usage_body,
            status = if (state.usageGranted) R.string.usage_access_granted
                else R.string.usage_access_not_granted,
            action = R.string.onboarding_open_usage_settings,
            onAction = onOpenUsageSettings,
        )
        OnboardingStep.NOTIFICATIONS -> PermissionPage(
            title = R.string.onboarding_notifications_title,
            body = R.string.onboarding_notifications_body,
            status = if (state.notificationGranted) R.string.onboarding_status_done
                else R.string.onboarding_status_not_done,
            action = R.string.onboarding_request_notifications,
            onAction = onRequestNotifications,
            apiNote = if (android.os.Build.VERSION.SDK_INT < 33) {
                R.string.onboarding_notifications_not_needed
            } else {
                null
            },
        )
        OnboardingStep.FIRST_GOALS -> OnboardingGoalsPage(state, viewModel)
        OnboardingStep.CLASSIFY_APPS -> OnboardingAppsPage(state, viewModel)
        OnboardingStep.FRICTION -> FrictionPage(state, viewModel)
        OnboardingStep.DONE -> DonePage(state)
    }
}

@Composable
private fun WelcomePage(title: Int, body: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PermissionPage(
    title: Int,
    body: Int,
    status: Int,
    action: Int,
    onAction: () -> Unit,
    apiNote: Int? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(status), style = MaterialTheme.typography.titleMedium)
        apiNote?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
        TextButton(onClick = onAction) { Text(stringResource(action)) }
    }
}

@Composable
private fun FrictionPage(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.onboarding_friction_title), style = MaterialTheme.typography.headlineMedium)
        if (state.frictionConfigured) {
            val current = when (state.frictionLevel) {
                FrictionLevel.GENTLE -> R.string.onboarding_friction_gentle
                FrictionLevel.BALANCED -> R.string.onboarding_friction_balanced
                FrictionLevel.STRICT -> R.string.onboarding_friction_strict
            }
            Text(
                stringResource(
                    R.string.onboarding_friction_saved,
                    stringResource(current, state.frictionLevel.defaultBaseDelaySeconds),
                ),
            )
        }
        FrictionLevel.entries.forEach { level ->
            val delay = level.defaultBaseDelaySeconds
            val (title, explanation) = when (level) {
                FrictionLevel.GENTLE -> R.string.onboarding_friction_gentle to
                    R.string.onboarding_friction_gentle_body
                FrictionLevel.BALANCED -> R.string.onboarding_friction_balanced to
                    R.string.onboarding_friction_balanced_body
                FrictionLevel.STRICT -> R.string.onboarding_friction_strict to
                    R.string.onboarding_friction_strict_body
            }
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(
                    role = Role.RadioButton,
                    onClick = { viewModel.setFrictionLevel(level) },
                ),
                shape = MaterialTheme.shapes.medium,
                color = if (state.frictionLevel == level) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.frictionLevel == level,
                        onClick = { viewModel.setFrictionLevel(level) },
                        label = { Text(stringResource(title, delay)) },
                    )
                    Text(stringResource(explanation), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun DonePage(state: OnboardingUiState) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(stringResource(R.string.onboarding_done_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(
                R.string.onboarding_done_summary,
                state.existingGoalCount,
                state.classifiedCount,
                state.frictionLevel.defaultBaseDelaySeconds,
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
