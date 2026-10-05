package com.savatech.chimelauncher.domain

import com.savatech.chimelauncher.data.apps.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSearchTest {
    @Test
    fun blankQueryReturnsEveryAppInInputOrder() {
        val apps = listOf(app("Zebra"), app("Alpha"))

        assertEquals(apps, filterApps(apps, "  "))
    }

    @Test
    fun prefixMatchesAreRankedBeforeContainsMatches() {
        val apps = listOf(app("Notes Pro"), app("My Notes"), app("Notes"))

        assertEquals(
            listOf("Notes", "Notes Pro", "My Notes"),
            filterApps(apps, "notes").map { it.label },
        )
    }

    @Test
    fun searchIgnoresAccentsAndCase() {
        assertEquals(listOf("Éclair"), filterApps(listOf(app("Éclair")), "e").map { it.label })
    }

    @Test
    fun queryWithNoMatchesReturnsAnEmptyList() {
        assertEquals(emptyList<AppInfo>(), filterApps(listOf(app("Calendar")), "music"))
    }

    @Test
    fun matchesStayAlphabeticalWithinEachRank() {
        val apps = listOf(app("Zoo Notes"), app("Notes Z"), app("Notes A"), app("App Notes"))

        assertEquals(
            listOf("Notes A", "Notes Z", "App Notes", "Zoo Notes"),
            filterApps(apps, "notes").map { it.label },
        )
    }

    @Test
    fun collatorEquivalentLabelsKeepTheirOriginalOrder() {
        val first = app("Cafe")
        val second = app("Café")

        assertEquals(listOf(first, second), filterApps(listOf(first, second), "c"))
    }

    private fun app(label: String) = AppInfo(label, "pkg.$label", "Main", 0, false)
}
