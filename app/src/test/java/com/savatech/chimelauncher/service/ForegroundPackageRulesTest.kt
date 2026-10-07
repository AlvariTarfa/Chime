package com.savatech.chimelauncher.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundPackageRulesTest {
    private val rules = ForegroundPackageRules(
        appPackageName = "com.example.chime",
        systemUiPackageName = "com.android.systemui",
        homePackages = setOf("com.example.home"),
        inputMethodPackages = setOf("com.example.keyboard"),
    )

    @Test
    fun ignoresChimeSystemUiKeyboardHomeAndMissingPackages() {
        assertTrue(rules.shouldIgnore("com.example.chime"))
        assertTrue(rules.shouldIgnore("com.android.systemui"))
        assertTrue(rules.shouldIgnore("com.example.keyboard"))
        assertTrue(rules.shouldIgnore("com.example.home"))
        assertTrue(rules.shouldIgnore(null))
    }

    @Test
    fun doesNotIgnoreOtherApps() {
        assertFalse(rules.shouldIgnore("com.example.distracting"))
    }

    @Test
    fun debouncesOnlyTheSamePackageForTwoSeconds() {
        val debouncer = ForegroundPackageDebouncer()

        assertTrue(debouncer.shouldProcess("com.example.one", 1_000L))
        assertFalse(debouncer.shouldProcess("com.example.one", 2_999L))
        assertTrue(debouncer.shouldProcess("com.example.two", 2_999L))
        assertTrue(debouncer.shouldProcess("com.example.one", 3_000L))
    }
}
