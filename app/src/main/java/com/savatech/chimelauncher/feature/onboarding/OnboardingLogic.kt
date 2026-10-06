package com.savatech.chimelauncher.feature.onboarding

import com.savatech.chimelauncher.domain.model.AppCategory

enum class OnboardingStep {
    WELCOME_GOALS,
    WELCOME_FRICTION,
    DEFAULT_LAUNCHER,
    USAGE_ACCESS,
    NOTIFICATIONS,
    FIRST_GOALS,
    CLASSIFY_APPS,
    FRICTION,
    DONE;

    fun next(): OnboardingStep? = entries.getOrNull(ordinal + 1)
    fun previous(): OnboardingStep? = entries.getOrNull(ordinal - 1)
}

fun suggestedDistractingPackages(installedPackageNames: Iterable<String>): Set<String> =
    installedPackageNames.toSet().intersect(SUGGESTED_DISTRACTING_PACKAGES)

fun suggestedAppCategories(installedPackageNames: Iterable<String>): Map<String, AppCategory> =
    installedPackageNames.toSet().associateWith { packageName ->
        if (packageName in SUGGESTED_DISTRACTING_PACKAGES) {
            AppCategory.DISTRACTING
        } else {
            AppCategory.NEUTRAL
        }
    }

fun classificationBatch(
    installedPackageNames: Iterable<String>,
    selectedCategories: Map<String, AppCategory>,
): Map<String, AppCategory> =
    installedPackageNames.toSet().associateWith { packageName ->
        selectedCategories[packageName] ?: AppCategory.NEUTRAL
    }

private val SUGGESTED_DISTRACTING_PACKAGES = setOf(
    "com.instagram.android",
    "com.facebook.katana",
    "com.zhiliaoapp.musically",
    "com.twitter.android",
    "com.snapchat.android",
    "com.reddit.frontpage",
    "com.netflix.mediaclient",
    "com.google.android.youtube",
)
