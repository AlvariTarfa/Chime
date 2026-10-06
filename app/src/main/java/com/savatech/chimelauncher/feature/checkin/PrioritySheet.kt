package com.savatech.chimelauncher.feature.checkin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PrioritySheet(
    goals: List<GoalModel>,
    selectedGoalIds: List<String>,
    onToggle: (String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.choose_priorities), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.select_up_to_three_priorities))
            LazyColumn {
                items(goals.filter { it.status == GoalStatus.ACTIVE }, key = GoalModel::id) { goal ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = goal.id in selectedGoalIds,
                            onCheckedChange = { onToggle(goal.id, it) },
                            enabled = goal.id in selectedGoalIds || selectedGoalIds.size < 3,
                        )
                        Text(goal.title, modifier = Modifier.weight(1f))
                    }
                }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        }
    }
}
