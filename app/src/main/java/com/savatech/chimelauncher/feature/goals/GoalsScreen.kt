package com.savatech.chimelauncher.feature.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.model.GoalStatus

@Composable
fun GoalsScreen(
    onCreateGoal: () -> Unit,
    onOpenGoal: (String) -> Unit,
    onOpenPriorities: () -> Unit,
    viewModel: GoalsListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateGoal) {
                Text(stringResource(R.string.create_goal))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.goals_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
                TextButton(onClick = onOpenPriorities) {
                    Text(stringResource(R.string.daily_priorities))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GoalStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.selectedStatus == status,
                        onClick = { viewModel.selectStatus(status) },
                        label = { Text(stringResource(status.labelResource())) },
                    )
                }
            }
            when {
                state.isLoading -> Text(stringResource(R.string.loading_goals))
                state.goals.isEmpty() -> Text(stringResource(R.string.no_goals))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.goals, key = { it.goal.id }) { goal ->
                        GoalCard(goal = goal, onClick = { onOpenGoal(goal.goal.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(goal: GoalCardUiState, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(goal.goal.title, style = MaterialTheme.typography.titleLarge)
            Text(goal.goal.category, style = MaterialTheme.typography.labelMedium)
            if (goal.dailyProgress == null) {
                Text(stringResource(R.string.nothing_scheduled_today))
            } else {
                LinearProgressIndicator(
                    progress = { goal.dailyProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(
                        R.string.goal_daily_progress,
                        (goal.dailyProgress * 100).toInt(),
                        goal.scheduledTaskCount,
                    ),
                )
            }
            Text(stringResource(R.string.best_current_streak, goal.currentStreak))
        }
    }
}

internal fun GoalStatus.labelResource(): Int = when (this) {
    GoalStatus.ACTIVE -> R.string.goal_status_active
    GoalStatus.PAUSED -> R.string.goal_status_paused
    GoalStatus.COMPLETED -> R.string.goal_status_completed
    GoalStatus.ARCHIVED -> R.string.goal_status_archived
}
