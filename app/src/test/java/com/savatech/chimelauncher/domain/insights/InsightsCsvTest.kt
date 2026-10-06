package com.savatech.chimelauncher.domain.insights

import com.savatech.chimelauncher.domain.model.AppCategory
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightsCsvTest {
    @Test
    fun escapesCommasQuotesNewlinesAndUnicode() {
        assertEquals("\"A, B\"", escapeCsv("A, B"))
        assertEquals("\"say \"\"yes\"\"\"", escapeCsv("say \"yes\""))
        assertEquals("\"line one\nline two\"", escapeCsv("line one\nline two"))
        assertEquals("東京", escapeCsv("東京"))
    }

    @Test
    fun exportsOneProperlyEscapedRowPerAppAndDay() {
        val csv = insightsCsv(
            listOf(
                InsightsCsvRow(
                    LocalDate.of(2026, 10, 6),
                    "com.example,app",
                    "Chime \"Focus\"",
                    12,
                    AppCategory.PRODUCTIVE,
                ),
                InsightsCsvRow(
                    LocalDate.of(2026, 10, 6),
                    "com.example.東京",
                    "東京",
                    4,
                    AppCategory.NEUTRAL,
                ),
            ),
        )

        assertEquals(
            "date,package,app label,minutes,category\n" +
                "2026-10-06,\"com.example,app\",\"Chime \"\"Focus\"\"\",12,PRODUCTIVE\n" +
                "2026-10-06,com.example.東京,東京,4,NEUTRAL\n",
            csv,
        )
    }
}
