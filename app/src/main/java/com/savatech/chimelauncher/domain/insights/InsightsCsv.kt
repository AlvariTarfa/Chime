package com.savatech.chimelauncher.domain.insights

import com.savatech.chimelauncher.domain.model.AppCategory
import java.time.LocalDate

data class InsightsCsvRow(
    val date: LocalDate,
    val packageName: String,
    val appLabel: String,
    val minutes: Long,
    val category: AppCategory,
)

fun escapeCsv(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }

fun insightsCsv(rows: List<InsightsCsvRow>): String = buildString {
    appendLine("date,package,app label,minutes,category")
    rows.forEach { row ->
        appendLine(
            listOf(
                row.date.toString(),
                row.packageName,
                row.appLabel,
                row.minutes.toString(),
                row.category.name,
            ).joinToString(",") { escapeCsv(it) },
        )
    }
}
