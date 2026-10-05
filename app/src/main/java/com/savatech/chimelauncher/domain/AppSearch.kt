package com.savatech.chimelauncher.domain

import com.savatech.chimelauncher.data.apps.AppInfo
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
    val normalizedQuery = normalizeForSearch(query.trim())
    if (normalizedQuery.isBlank()) return apps

    val collator = Collator.getInstance(Locale.getDefault()).apply {
        strength = Collator.PRIMARY
        decomposition = Collator.CANONICAL_DECOMPOSITION
    }
    val matches = apps.mapNotNull { app ->
        val normalizedLabel = normalizeForSearch(app.label)
        when {
            normalizedLabel.startsWith(normalizedQuery) -> 0 to app
            normalizedLabel.contains(normalizedQuery) -> 1 to app
            else -> null
        }
    }
    return matches.sortedWith { left, right ->
        val rankOrder = left.first.compareTo(right.first)
        if (rankOrder != 0) rankOrder else collator.compare(left.second.label, right.second.label)
    }.map { it.second }
}

private fun normalizeForSearch(value: String): String =
    Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() ||
            Character.getType(it) == Character.COMBINING_SPACING_MARK.toInt() ||
            Character.getType(it) == Character.ENCLOSING_MARK.toInt()
        }
