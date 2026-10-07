package com.savatech.chimelauncher.domain.notification

import com.savatech.chimelauncher.data.db.entities.DigestItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DigestSummaryBuilderTest {
    @Test
    fun groupsCountsByAppAndLimitsExpandableLinesToFive() {
        val items = (1..7).map { index -> digestItem("app.$index") } +
            listOf(digestItem("app.1"), digestItem("app.1"))

        val summary = DigestSummaryBuilder.build(items)

        assertEquals(9, summary?.itemCount)
        assertEquals(3, summary?.appCounts?.get("app.1"))
        assertEquals(7, summary?.appCounts?.size)
        assertEquals(5, summary?.inboxLines?.size)
        assertEquals(DigestAppCount("app.1", 3), summary?.inboxLines?.first())
    }

    @Test
    fun emptyDigestHasNoSummary() {
        assertNull(DigestSummaryBuilder.build(emptyList()))
    }

    @Test
    fun truncatesStoredTitleAndTextAtTwoHundredCharacters() {
        val longText = "x".repeat(250)

        assertEquals(200, truncateNotificationText(longText)?.length)
        assertEquals("short", truncateNotificationText("short"))
        assertEquals(null, truncateNotificationText(null))
    }

    private fun digestItem(packageName: String) = DigestItem(
        packageName = packageName,
        title = null,
        text = null,
        postedAt = 0,
        delivered = false,
    )
}
