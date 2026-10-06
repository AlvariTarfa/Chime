package com.savatech.chimelauncher.feature.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.insights.CorrelationTakeaway
import com.savatech.chimelauncher.domain.insights.DayInsight
import com.savatech.chimelauncher.domain.insights.RangeInsight
import com.savatech.chimelauncher.domain.insights.correlationTakeaway
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TrendScreen(report: RangeInsight, focusScoreEnabled: Boolean) {
    val labels = chartLabels(report.days)
    val days = report.days
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (focusScoreEnabled) {
            report.focusScore?.let {
                Text(
                    stringResource(R.string.insights_focus_score, it),
                    style = MaterialTheme.typography.titleMedium,
                )
            } ?: Text(stringResource(R.string.insights_focus_score_unavailable))
        }
        ChartCard(stringResource(R.string.insights_daily_screen_time)) {
            BarChart(
                values = days.map { it.totalScreenMillis / MILLIS_PER_HOUR.toFloat() },
                labels = labels,
                highlightIndex = days.lastIndex,
                valueUnit = stringResource(R.string.insights_hours),
                summary = localizedSummary(days) { day ->
                    stringResource(
                        R.string.insights_chart_value,
                        formatDate(day.date),
                        formatDuration(day.totalScreenMillis),
                    )
                },
            )
        }
        ChartCard(stringResource(R.string.insights_goal_completion)) {
            LineChart(
                values = days.map { it.goalCompletionRate?.let { rate -> (rate * 100).toFloat() } },
                labels = labels,
                valueUnit = stringResource(R.string.percentage_unit),
                summary = localizedSummary(days) { day ->
                    day.goalCompletionRate?.let {
                        stringResource(R.string.insights_chart_percent, formatDate(day.date), (it * 100).toInt())
                    } ?: stringResource(R.string.insights_chart_no_priority, formatDate(day.date))
                },
            )
        }
        ChartCard(stringResource(R.string.insights_distracting_time)) {
            LineChart(
                values = days.map { it.distractingMillis / MILLIS_PER_MINUTE.toFloat() },
                labels = labels,
                valueUnit = stringResource(R.string.insights_minutes),
                summary = localizedSummary(days) { day ->
                    stringResource(
                        R.string.insights_chart_value,
                        formatDate(day.date),
                        categoryMinutes(day.distractingMillis),
                    )
                },
            )
        }
        CorrelationCard(days)
    }
}

@Composable
fun CorrelationCard(days: List<DayInsight>) {
    val labels = chartLabels(days)
    val takeaway = correlationTakeaway(days)
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.insights_correlation_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.insights_goal_linked_time), style = MaterialTheme.typography.labelLarge)
            LineChart(
                values = days.map { it.goalLinkedMillis / MILLIS_PER_MINUTE.toFloat() },
                labels = labels,
                valueUnit = stringResource(R.string.insights_minutes),
                summary = localizedSummary(days) { day ->
                    stringResource(
                        R.string.insights_chart_value,
                        formatDate(day.date),
                        categoryMinutes(day.goalLinkedMillis),
                    )
                },
            )
            Text(stringResource(R.string.insights_distracting_time), style = MaterialTheme.typography.labelLarge)
            LineChart(
                values = days.map { it.distractingMillis / MILLIS_PER_MINUTE.toFloat() },
                labels = labels,
                valueUnit = stringResource(R.string.insights_minutes),
                summary = localizedSummary(days) { day ->
                    stringResource(
                        R.string.insights_chart_value,
                        formatDate(day.date),
                        categoryMinutes(day.distractingMillis),
                    )
                },
            )
            Text(
                when (takeaway) {
                    CorrelationTakeaway.NotEnoughData -> stringResource(R.string.insights_correlation_not_enough)
                    is CorrelationTakeaway.Comparison -> when {
                        takeaway.minutesLessOnCompleteDays == 0 ->
                            stringResource(R.string.insights_correlation_similar)
                        takeaway.minutesLessOnCompleteDays > 0 ->
                            stringResource(R.string.insights_correlation_less, takeaway.minutesLessOnCompleteDays)
                        else -> {
                            val moreMinutes = -takeaway.minutesLessOnCompleteDays
                            stringResource(R.string.insights_correlation_more, moreMinutes)
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun WeeklySummary(days: List<DayInsight>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.insights_weekly_summary), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(
                R.string.insights_average_screen_time,
                formatDuration(days.sumOf(DayInsight::totalScreenMillis) / days.size.coerceAtLeast(1)),
            ),
        )
        days.minByOrNull(DayInsight::totalScreenMillis)?.let {
            Text(stringResource(R.string.insights_best_day, formatDate(it.date)))
        }
        days.maxByOrNull(DayInsight::totalScreenMillis)?.let {
            Text(stringResource(R.string.insights_worst_day, formatDate(it.date)))
        }
        days.forEach { day ->
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatDate(day.date))
                    Text(formatDuration(day.totalScreenMillis))
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun localizedSummary(
    days: List<DayInsight>,
    itemText: @Composable (DayInsight) -> String,
): String {
    val items = mutableListOf<String>()
    for (day in days) items += itemText(day)
    return items.joinToString()
}

private fun chartLabels(days: List<DayInsight>): List<String> =
    days.map { it.date.format(DateTimeFormatter.ofPattern("d", Locale.getDefault())) }

@Composable
internal fun categoryMinutes(millis: Long): String =
    pluralStringResource(
        R.plurals.insights_minutes_format,
        (millis / MILLIS_PER_MINUTE).toInt(),
        millis / MILLIS_PER_MINUTE,
    )

internal fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))

@Composable
internal fun formatDuration(millis: Long): String {
    val minutes = millis.coerceAtLeast(0) / MILLIS_PER_MINUTE
    return if (minutes >= 60) {
        stringResource(
            R.string.insights_duration_format,
            pluralStringResource(R.plurals.insights_hours_format, (minutes / 60).toInt(), minutes / 60),
            pluralStringResource(
                R.plurals.insights_minutes_format,
                (minutes % 60).toInt(),
                minutes % 60,
            ),
        )
    } else {
        pluralStringResource(R.plurals.insights_minutes_format, minutes.toInt(), minutes)
    }
}

@Composable
internal fun formatDuration(duration: Duration): String = formatDuration(duration.toMillis())

internal const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE
