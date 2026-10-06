package com.savatech.chimelauncher.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.insights.niceChartMaximum
import java.math.BigDecimal

@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    highlightIndex: Int?,
    valueUnit: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val maximumValue = niceChartMaximum(values.maxOrNull()?.toDouble() ?: 0.0)
    val maximum = maximumValue.toFloat()
    val maximumText = maximumValue.axisText()
    val description = stringResource(R.string.chart_accessibility_summary, summary, maximumText, valueUnit)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val regularColor = MaterialTheme.colorScheme.primary
    val highlightColor = MaterialTheme.colorScheme.tertiary
    Column(modifier.semantics { contentDescription = description }) {
        Text(
            stringResource(R.string.chart_maximum, maximumText, valueUnit),
            style = MaterialTheme.typography.labelSmall,
        )
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val bottom = size.height - 2.dp.toPx()
            val slot = size.width / values.size.coerceAtLeast(1)
            val barWidth = slot * 0.56f
            for (line in 0..4) {
                val y = bottom - bottom * line / 4f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            values.forEachIndexed { index, raw ->
                val height = bottom * raw.coerceAtLeast(0f) / maximum
                val left = slot * index + (slot - barWidth) / 2
                drawRect(
                    color = if (index == highlightIndex) highlightColor else regularColor,
                    topLeft = Offset(left, bottom - height),
                    size = androidx.compose.ui.geometry.Size(barWidth, height),
                )
            }
        }
        ChartLabels(labels)
    }
}

@Composable
fun LineChart(
    values: List<Float?>,
    labels: List<String>,
    valueUnit: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    val maximumValue = niceChartMaximum(
        values.filterNotNull().filter(Float::isFinite).maxOrNull()?.toDouble() ?: 0.0,
    )
    val maximum = maximumValue.toFloat()
    val maximumText = maximumValue.axisText()
    val description = stringResource(R.string.chart_accessibility_summary, summary, maximumText, valueUnit)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val lineColor = MaterialTheme.colorScheme.secondary
    Column(modifier.semantics { contentDescription = description }) {
        Text(
            stringResource(R.string.chart_maximum, maximumText, valueUnit),
            style = MaterialTheme.typography.labelSmall,
        )
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val top = 4.dp.toPx()
            val bottom = size.height - 4.dp.toPx()
            for (line in 0..4) {
                val y = bottom - (bottom - top) * line / 4f
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            if (values.isNotEmpty()) {
                val points = values.mapIndexed { index, raw ->
                    raw?.takeIf(Float::isFinite)?.let { value ->
                        val x = if (values.size == 1) size.width / 2 else size.width * index / (values.size - 1)
                        val y = bottom - (bottom - top) * value.coerceAtLeast(0f) / maximum
                        Offset(x, y)
                    }
                }
                points.forEachIndexed { index, point ->
                    if (point != null) {
                        drawCircle(lineColor, 3.dp.toPx(), point)
                        val next = points.getOrNull(index + 1)
                        if (next != null) {
                            drawLine(
                                lineColor,
                                point,
                                next,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                }
            }
        }
        ChartLabels(labels)
    }
}

private fun Double.axisText(): String =
    BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()

@Composable
private fun ChartLabels(labels: List<String>) {
    val visibleLabels = if (labels.size <= 7) {
        labels
    } else {
        listOf(0, labels.size / 3, labels.size * 2 / 3, labels.lastIndex)
            .distinct()
            .map(labels::get)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        visibleLabels.forEach { label ->
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
