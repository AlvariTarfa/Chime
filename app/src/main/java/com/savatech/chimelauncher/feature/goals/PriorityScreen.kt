package com.savatech.chimelauncher.feature.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.usecase.SetDailyPrioritiesResult

@Composable
fun PriorityScreen(
    onBack: () -> Unit,
    viewModel: PriorityViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.daily_priorities), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.select_up_to_three_priorities))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    Text(stringResource(R.string.selected_priorities), style = MaterialTheme.typography.titleMedium)
                }
                items(state.selectedGoalIds, key = { it }) { goalId ->
                    val goal = state.activeGoals.firstOrNull { it.id == goalId }
                    if (goal != null) {
                        val index = state.selectedGoalIds.indexOf(goalId)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.priority_position, index + 1),
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(goal.title, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { viewModel.move(goal.id, -1) },
                                enabled = index > 0,
                            ) { Text(stringResource(R.string.move_up)) }
                            TextButton(
                                onClick = { viewModel.move(goal.id, 1) },
                                enabled = index < state.selectedGoalIds.lastIndex,
                            ) { Text(stringResource(R.string.move_down)) }
                            TextButton(onClick = { viewModel.toggle(goal.id, false) }) {
                                Text(stringResource(R.string.remove_priority))
                            }
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.active_goals_label), style = MaterialTheme.typography.titleMedium)
                }
                items(state.activeGoals.filterNot { it.id in state.selectedGoalIds }, key = GoalModel::id) { goal ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = false,
                            onCheckedChange = { checked -> viewModel.toggle(goal.id, checked) },
                        )
                        Text(goal.title, modifier = Modifier.weight(1f))
                    }
                }
            }
            state.error?.let { error ->
                Text(
                    text = stringResource(error.messageResource()),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::save) { Text(stringResource(R.string.save_priorities)) }
                TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

private fun SetDailyPrioritiesResult.messageResource(): Int = when (this) {
    SetDailyPrioritiesResult.Success -> R.string.priorities_saved
    SetDailyPrioritiesResult.TooManyPriorities -> R.string.too_many_priorities
    is SetDailyPrioritiesResult.DuplicateGoal -> R.string.duplicate_priority
    is SetDailyPrioritiesResult.GoalNotFound -> R.string.priority_goal_missing
    is SetDailyPrioritiesResult.GoalNotActive -> R.string.priority_goal_not_active
}
