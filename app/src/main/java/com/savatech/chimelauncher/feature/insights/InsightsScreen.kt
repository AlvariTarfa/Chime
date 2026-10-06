package com.savatech.chimelauncher.feature.insights

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.insights.DayInsight
import com.savatech.chimelauncher.domain.model.AppCategory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class InsightsTab { TODAY, TRENDS, WEEK }

@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(InsightsTab.TODAY) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> if (uri != null) viewModel.export(uri) }
    val exportFileName = stringResource(
        R.string.insights_csv_filename,
        state.report?.days?.lastOrNull()?.date ?: LocalDate.now(),
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(
                stringResource(R.string.insights_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        if (!state.usagePermissionGranted) {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.insights_permission_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(stringResource(R.string.insights_permission_explanation))
                    TextButton(onClick = {
                        context.startActivity(viewModel.usageAccessSettingsIntent())
                    }) { Text(stringResource(R.string.insights_open_usage_settings)) }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InsightsTab.entries.forEach { tab ->
                    FilterChip(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                stringResource(
                                    when (tab) {
                                        InsightsTab.TODAY -> R.string.insights_today
                                        InsightsTab.TRENDS -> R.string.insights_trends
                                        InsightsTab.WEEK -> R.string.insights_week
                                    },
                                ),
                            )
                        },
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { exportLauncher.launch(exportFileName) }) {
                    Text(stringResource(R.string.insights_export_csv))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30).forEach { count ->
                    FilterChip(
                        selected = state.dayCount == count,
                        onClick = { viewModel.selectDays(count) },
                        label = {
                            Text(stringResource(if (count == 7) R.string.insights_seven_days else R.string.insights_thirty_days))
                        },
                    )
                }
            }
            if (state.exportFailed) {
                Text(
                    stringResource(R.string.insights_export_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.loadFailed) {
                Text(
                    stringResource(R.string.insights_load_failed),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            val report = state.report
            if (state.isLoading || report == null) {
                Text(stringResource(R.string.loading))
            } else {
                when (selectedTab) {
                    InsightsTab.TODAY -> TodayScreen(
                        day = report.days.last(),
                        estimatedMinutesSaved = state.estimatedMinutesSaved,
                    )
                    InsightsTab.TRENDS -> TrendScreen(
                        report = report,
                        focusScoreEnabled = state.focusScoreEnabled,
                    )
                    InsightsTab.WEEK -> WeeklySummary(report.days.takeLast(7))
                }
            }
        }
    }
}

@Composable
fun TodayScreen(day: DayInsight, estimatedMinutesSaved: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(formatDate(day.date), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.insights_total_screen_time), style = MaterialTheme.typography.titleMedium)
        Text(formatDuration(day.totalScreenMillis), style = MaterialTheme.typography.displaySmall)
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val pickupText = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    stringResource(R.string.insights_not_available)
                } else {
                    day.pickups?.toString() ?: stringResource(R.string.insights_no_data)
                }
                InsightValue(stringResource(R.string.insights_pickups), pickupText)
                InsightValue(
                    stringResource(R.string.insights_first_pickup),
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        stringResource(R.string.insights_not_available)
                    } else {
                        day.firstPickup?.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                            ?: stringResource(R.string.insights_no_data)
                    },
                )
                InsightValue(
                    stringResource(R.string.insights_longest_session),
                    day.longestSession?.let { formatDuration(it) }
                        ?: stringResource(R.string.insights_no_data),
                )
            }
        }
        Text(stringResource(R.string.insights_top_apps), style = MaterialTheme.typography.titleMedium)
        if (day.topApps.isEmpty()) {
            Text(stringResource(R.string.insights_empty_app_list))
        } else {
            day.topApps.forEach { app ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        Modifier.padding(end = 8.dp).size(12.dp),
                        shape = CircleShape,
                        color = categoryColor(app.category),
                    ) {}
                    Text(app.label, Modifier.weight(1f))
                    Text(formatDuration(app.totalScreenMillis))
                }
            }
        }
        CategorySplit(day)
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.insights_intercepts), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.insights_intercepts_shown, day.interceptsShown))
                Text(stringResource(R.string.insights_intercepts_cancelled, day.interceptsCancelled))
                Text(
                    pluralStringResource(
                        R.plurals.insights_estimated_saved,
                        estimatedMinutesSaved,
                        estimatedMinutesSaved,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CategorySplit(day: DayInsight) {
    val total = day.productiveMillis + day.neutralMillis + day.distractingMillis
    val description = stringResource(
        R.string.insights_category_accessibility,
        stringResource(R.string.insights_category_split),
        day.productiveMillis / MILLIS_PER_MINUTE,
        day.neutralMillis / MILLIS_PER_MINUTE,
        day.distractingMillis / MILLIS_PER_MINUTE,
    )
    Column(
        Modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(stringResource(R.string.insights_category_split), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth().height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
            if (total > 0L) {
                CategorySegment(day.productiveMillis, total, AppCategory.PRODUCTIVE)
                CategorySegment(day.neutralMillis, total, AppCategory.NEUTRAL)
                CategorySegment(day.distractingMillis, total, AppCategory.DISTRACTING)
            }
        }
        Text(
            "${stringResource(R.string.category_productive)} ${categoryMinutes(day.productiveMillis)} · " +
                "${stringResource(R.string.category_neutral)} ${categoryMinutes(day.neutralMillis)} · " +
                "${stringResource(R.string.category_distracting)} ${categoryMinutes(day.distractingMillis)}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RowScope.CategorySegment(millis: Long, total: Long, category: AppCategory) {
    if (millis > 0L) {
        Spacer(
            Modifier.weight(millis.toFloat() / total).fillMaxWidth().height(12.dp)
                .background(categoryColor(category)),
        )
    }
}

@Composable
private fun InsightValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value)
    }
}

@Composable
private fun categoryColor(category: AppCategory) = when (category) {
    AppCategory.PRODUCTIVE -> MaterialTheme.colorScheme.tertiary
    AppCategory.NEUTRAL -> MaterialTheme.colorScheme.secondary
    AppCategory.DISTRACTING -> MaterialTheme.colorScheme.error
}
