package com.savatech.chimelauncher.domain.notification

import com.savatech.chimelauncher.data.db.entities.DigestItem

data class DigestSummary(
    val itemCount: Int,
    val appCounts: Map<String, Int>,
    val inboxLines: List<DigestAppCount>,
)

data class DigestAppCount(val packageName: String, val count: Int)

object DigestSummaryBuilder {
    fun build(items: List<DigestItem>): DigestSummary? {
        if (items.isEmpty()) return null
        val counts = items.groupingBy(DigestItem::packageName).eachCount().toSortedMap()
        return DigestSummary(
            itemCount = items.size,
            appCounts = counts,
            inboxLines = counts.entries.take(MAX_INBOX_LINES).map { (packageName, count) ->
                DigestAppCount(packageName, count)
            },
        )
    }

    const val MAX_INBOX_LINES = 5
}

fun truncateNotificationText(value: CharSequence?): String? =
    value?.toString()?.take(MAX_NOTIFICATION_TEXT_LENGTH)

const val MAX_NOTIFICATION_TEXT_LENGTH = 200
