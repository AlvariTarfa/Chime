package com.savatech.chimelauncher.data.usage

data class UsageEventRecord(
    val timestamp: Long,
    val packageName: String,
    val className: String?,
    val type: UsageEventType,
)

enum class UsageEventType {
    ACTIVITY_RESUMED,
    ACTIVITY_PAUSED,
    MOVE_TO_FOREGROUND,
    MOVE_TO_BACKGROUND,
    SCREEN_INTERACTIVE,
    KEYGUARD_HIDDEN,
    OTHER,
}

data class PairedUsageData(
    val foregroundMillisByPackage: Map<String, Long>,
    val pickups: Int,
    val firstPickupMillis: Long?,
    val longestSessionMillis: Long?,
)

private data class ComponentKey(
    val packageName: String,
    val className: String?,
)

fun pairUsageEvents(
    events: List<UsageEventRecord>,
    windowStartMillis: Long,
    windowEndMillis: Long,
    ignoredPackages: Set<String> = emptySet(),
): PairedUsageData {
    if (windowEndMillis <= windowStartMillis) {
        return PairedUsageData(emptyMap(), 0, null, null)
    }

    val orderedEvents = events
        .asSequence()
        .filter { it.timestamp >= windowStartMillis && it.timestamp < windowEndMillis }
        .sortedBy(UsageEventRecord::timestamp)
        .toList()
    val pickupEvents = orderedEvents.filter {
        it.type == UsageEventType.SCREEN_INTERACTIVE ||
            it.type == UsageEventType.KEYGUARD_HIDDEN
    }
    val openSessions = mutableMapOf<ComponentKey, Long>()
    val consumedOrphanPauses = mutableSetOf<ComponentKey>()
    val totals = mutableMapOf<String, Long>()
    var longestSessionMillis: Long? = null

    fun recordSession(key: ComponentKey, startMillis: Long, endMillis: Long) {
        val duration = endMillis - startMillis
        if (duration <= 0L) return
        totals[key.packageName] = (totals[key.packageName] ?: 0L) + duration
        longestSessionMillis = maxOf(longestSessionMillis ?: 0L, duration)
    }

    orderedEvents.forEach { event ->
        if (event.packageName in ignoredPackages) return@forEach
        val key = ComponentKey(event.packageName, event.className)
        when (event.type) {
            UsageEventType.ACTIVITY_RESUMED,
            UsageEventType.MOVE_TO_FOREGROUND,
            -> openSessions.putIfAbsent(key, event.timestamp)

            UsageEventType.ACTIVITY_PAUSED,
            UsageEventType.MOVE_TO_BACKGROUND,
            -> {
                val startMillis = openSessions.remove(key)
                    ?: if (consumedOrphanPauses.add(key)) windowStartMillis else null
                if (startMillis != null) recordSession(key, startMillis, event.timestamp)
            }

            else -> Unit
        }
    }

    openSessions.forEach { (key, startMillis) ->
        recordSession(key, startMillis, windowEndMillis)
    }

    return PairedUsageData(
        foregroundMillisByPackage = totals.toMap(),
        pickups = pickupEvents.size,
        firstPickupMillis = pickupEvents.firstOrNull()?.timestamp,
        longestSessionMillis = longestSessionMillis,
    )
}
