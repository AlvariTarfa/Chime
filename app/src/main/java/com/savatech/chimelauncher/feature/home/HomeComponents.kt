package com.savatech.chimelauncher.feature.home

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.core.theme.WallpaperScrimAlpha
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.IconCache
import com.savatech.chimelauncher.data.settings.IconShape
import com.savatech.chimelauncher.core.theme.LocalLayoutMetrics
import com.savatech.chimelauncher.domain.usecase.GoalOverview
import com.savatech.chimelauncher.feature.drawer.AppIcon

@Composable
internal fun PriorityGoalContent(
    overview: GoalOverview,
    unfinishedTasks: List<HomeTaskItem>,
    onOpenGoal: () -> Unit,
    onTaskChecked: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        TextButton(onClick = onOpenGoal, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = overview.goal.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        val progress = overview.dailyProgress ?: 0f
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        if (overview.dailyProgress == null) {
            Text(stringResource(R.string.nothing_scheduled_today), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                stringResource(
                    R.string.goal_daily_progress,
                    (progress * 100).toInt(),
                    overview.scheduledTasks.size,
                ),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        val visibleTasks = unfinishedTasks.take(MAX_VISIBLE_TASKS)
        visibleTasks.forEach { item ->
            key(item.task.task.id) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = false,
                        onCheckedChange = { checked -> onTaskChecked(item.task.task.id, checked) },
                    )
                    Text(item.task.task.title, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        val hiddenTaskCount = unfinishedTasks.size - visibleTasks.size
        if (hiddenTaskCount > 0) {
            Text(
                stringResource(R.string.more_tasks, hiddenTaskCount),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
internal fun DockAppItem(
    app: AppInfo,
    showIcon: Boolean,
    iconCache: IconCache?,
    iconShape: IconShape,
    iconPackPackage: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val layout = LocalLayoutMetrics.current
    Column(
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(
                horizontal = (layout.horizontalPadding / 2).dp,
                vertical = (layout.verticalPadding / 2).dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((layout.itemSpacing / 2).dp),
    ) {
        if (showIcon && iconCache != null) {
            AppIcon(app, iconCache, 40.dp, iconShape, iconPackPackage)
        }
        Text(app.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
internal fun ScrimCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = WallpaperScrimAlpha),
        ),
    ) {
        content()
    }
}

private const val MAX_VISIBLE_TASKS = 3
