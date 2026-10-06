package com.savatech.chimelauncher.feature.onboarding

import com.savatech.chimelauncher.domain.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingLogicTest {
    @Test
    fun suggestionsIncludeOnlySupportedPackagesThatAreInstalled() {
        val installed = listOf(
            "com.instagram.android",
            "com.facebook.katana",
            "com.zhiliaoapp.musically",
            "com.twitter.android",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.netflix.mediaclient",
            "com.google.android.youtube",
            "com.example.other",
        )

        assertEquals(
            installed.toSet() - "com.example.other",
            suggestedDistractingPackages(installed),
        )
        assertEquals(
            installed.associateWith { packageName ->
                if (packageName == "com.example.other") AppCategory.NEUTRAL
                else AppCategory.DISTRACTING
            },
            suggestedAppCategories(installed),
        )
    }

    @Test
    fun classificationBatchMapsEveryInstalledAppAndIgnoresUninstalledEntries() {
        val selected = mapOf(
            "com.example.work" to AppCategory.PRODUCTIVE,
            "com.example.neutral" to AppCategory.NEUTRAL,
            "com.example.video" to AppCategory.DISTRACTING,
            "com.uninstalled.app" to AppCategory.DISTRACTING,
        )

        assertEquals(
            mapOf(
                "com.example.work" to AppCategory.PRODUCTIVE,
                "com.example.neutral" to AppCategory.NEUTRAL,
                "com.example.video" to AppCategory.DISTRACTING,
                "com.example.other" to AppCategory.NEUTRAL,
            ),
            classificationBatch(
                listOf(
                    "com.example.work",
                    "com.example.neutral",
                    "com.example.video",
                    "com.example.other",
                ),
                selected,
            ),
        )
    }
}
