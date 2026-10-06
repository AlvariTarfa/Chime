package com.savatech.chimelauncher.feature.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.db.entities.CheckIn
import com.savatech.chimelauncher.domain.model.CheckInType
import com.savatech.chimelauncher.domain.model.GoalStatus

@Composable
fun CheckInScreen(
    type: String,
    onBack: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPrioritySheet by remember { mutableStateOf(false) }
    val kind = runCatching { CheckInType.valueOf(type) }.getOrNull()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    Scaffold { padding ->
        if (state.isLoading) {
            Text(stringResource(R.string.loading_check_in), modifier = Modifier.padding(padding).padding(16.dp))
        } else if (kind == null) {
            Text(stringResource(R.string.check_in_load_failed), modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(kind.titleResource()),
                    style = MaterialTheme.typography.headlineMedium,
                )
                when (kind) {
                    CheckInType.MORNING -> MorningContent(
                        state = state,
                        onChoosePriorities = { showPrioritySheet = true },
                        onEnergy = viewModel::setEnergy,
                        onIntention = viewModel::setIntention,
                    )
                    CheckInType.EVENING -> EveningContent(
                        state = state,
                        onCarryOver = viewModel::togglePriority,
                        onBlockedBy = viewModel::setBlockedBy,
                        onWin = viewModel::setWin,
                    )
                    CheckInType.WEEKLY -> WeeklyContent(state, viewModel)
                }
                state.error.let {
                    if (it) Text(
                        stringResource(R.string.check_in_save_failed),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        when (kind) {
                            CheckInType.MORNING -> viewModel.saveMorning()
                            CheckInType.EVENING -> viewModel.saveEvening()
                            CheckInType.WEEKLY -> viewModel.saveWeeklyReview()
                        }
                    }) { Text(stringResource(R.string.save_check_in)) }
                    TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
                }
            }
        }
    }
    if (showPrioritySheet) {
        PrioritySheet(
            goals = state.goals,
            selectedGoalIds = state.selectedGoalIds,
            onToggle = viewModel::togglePriority,
            onDismiss = { showPrioritySheet = false },
        )
    }
}

@Composable
private fun MorningContent(
    state: CheckInUiState,
    onChoosePriorities: () -> Unit,
    onEnergy: (Int) -> Unit,
    onIntention: (String) -> Unit,
) {
    Text(stringResource(R.string.morning_priorities_summary, state.selectedGoalIds.size))
    state.selectedGoalIds.forEach { id ->
        Text(state.goals.firstOrNull { it.id == id }?.title.orEmpty())
    }
    TextButton(onClick = onChoosePriorities) { Text(stringResource(R.string.choose_priorities)) }
    Text(stringResource(R.string.energy_level))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { energy ->
            TextButton(onClick = { onEnergy(energy) }) {
                Text(
                    text = energy.toString(),
                    color = if (state.energy == energy) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
    OutlinedTextField(
        value = state.intention,
        onValueChange = onIntention,
        label = { Text(stringResource(R.string.optional_intention)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EveningContent(
    state: CheckInUiState,
    onCarryOver: (String, Boolean) -> Unit,
    onBlockedBy: (String) -> Unit,
    onWin: (String) -> Unit,
) {
    Text(stringResource(R.string.completed_tasks_today), style = MaterialTheme.typography.titleMedium)
    if (state.completedTasks.isEmpty()) {
        Text(stringResource(R.string.no_completed_tasks_today))
    } else {
        state.completedTasks.forEach { (task, _) -> Text(task.title) }
    }
    OutlinedTextField(
        value = state.blockedBy,
        onValueChange = onBlockedBy,
        label = { Text(stringResource(R.string.what_blocked_you)) },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.win,
        onValueChange = onWin,
        label = { Text(stringResource(R.string.one_win)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.carry_over_priorities), style = MaterialTheme.typography.titleMedium)
    state.goals.filter { it.status == GoalStatus.ACTIVE }.forEach { goal ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = goal.id in state.selectedGoalIds,
                onCheckedChange = { onCarryOver(goal.id, it) },
                enabled = goal.id in state.selectedGoalIds || state.selectedGoalIds.size < 3,
            )
            Text(goal.title)
        }
    }
}

@Composable
private fun WeeklyContent(state: CheckInUiState, viewModel: CheckInViewModel) {
    Text(stringResource(R.string.weekly_review_subtitle))
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.weeklyReview, key = { it.goal.id }) { review ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(review.goal.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.weekly_goal_stats,
                            (review.completionRate * 100).toInt(),
                            review.currentStreak,
                        ),
                    )
                    Row {
                        TextButton(onClick = {
                            viewModel.changeGoalStatus(review.goal, GoalStatus.PAUSED)
                        }) { Text(stringResource(R.string.pause_goal)) }
                        TextButton(onClick = {
                            viewModel.changeGoalStatus(review.goal, GoalStatus.ARCHIVED)
                        }) { Text(stringResource(R.string.archive_goal)) }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckInHistoryScreen(
    onBack: () -> Unit,
    viewModel: CheckInViewModel = hiltViewModel(),
) {
    val entries by viewModel.history.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query = state.historySearch.trim()
    val filtered = entries.filter { it.searchable().contains(query, ignoreCase = true) }
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.check_in_history), style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = state.historySearch,
                onValueChange = viewModel::setHistorySearch,
                label = { Text(stringResource(R.string.search_check_ins)) },
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = CheckIn::id) { checkIn ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(stringResource(checkIn.type.toCheckInType().titleResource()))
                            Text(checkIn.date)
                            checkIn.notes?.takeIf(String::isNotBlank)?.let { Text(it) }
                            checkIn.payloadJson?.let { payload ->
                                Text(
                                    payload,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 4,
                                )
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onBack) { Text(stringResource(R.string.done)) }
        }
    }
}

private fun CheckIn.searchable(): String = listOfNotNull(notes, payloadJson, type, date).joinToString(" ")
private fun String.toCheckInType(): CheckInType =
    CheckInType.entries.firstOrNull { it.name == this } ?: CheckInType.MORNING
private fun CheckInType.titleResource(): Int = when (this) {
    CheckInType.MORNING -> R.string.morning_check_in_title
    CheckInType.EVENING -> R.string.evening_check_in_title
    CheckInType.WEEKLY -> R.string.weekly_review_title
}
