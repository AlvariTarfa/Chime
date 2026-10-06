package com.savatech.chimelauncher.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R

@Composable
fun OnboardingGoalsPage(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.onboarding_goals_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_goals_body), style = MaterialTheme.typography.bodyMedium)
        if (state.hasExistingGoals) {
            Text(stringResource(R.string.onboarding_existing_goals, state.existingGoalCount))
        } else {
            state.goals.forEachIndexed { index, draft ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.onboarding_goal_number, index + 1),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (state.goals.size > 1) {
                        TextButton(onClick = { viewModel.removeGoal(index) }) {
                            Text(stringResource(R.string.onboarding_remove_goal))
                        }
                    }
                }
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { viewModel.updateGoalTitle(index, it.take(80)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.onboarding_goal_title_hint)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = draft.task,
                    onValueChange = { viewModel.updateGoalTask(index, it.take(80)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.onboarding_goal_task_hint)) },
                    singleLine = true,
                )
            }
            if (state.goals.size < 3) {
                TextButton(onClick = viewModel::addGoal) {
                    Text(stringResource(R.string.onboarding_add_goal))
                }
            }
        }
    }
}
