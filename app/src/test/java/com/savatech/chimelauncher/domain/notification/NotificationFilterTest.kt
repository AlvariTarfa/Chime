package com.savatech.chimelauncher.domain.notification

import com.savatech.chimelauncher.domain.model.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationFilterTest {
    @Test
    fun ignoresOngoingGroupSummaryAndOwnNotificationsFirst() {
        assertDecision(input(isOngoing = true), NotificationDecision.IGNORE)
        assertDecision(input(isGroupSummary = true), NotificationDecision.IGNORE)
        assertDecision(input(isFromThisApp = true), NotificationDecision.IGNORE)
    }

    @Test
    fun ignoresExcludedCategoriesMediaDialerAndSmsBeforeAllowList() {
        listOf(
            NotificationKind.CALL,
            NotificationKind.ALARM,
            NotificationKind.REMINDER,
            NotificationKind.NAVIGATION,
        ).forEach { kind ->
            assertDecision(input(kind = kind), NotificationDecision.IGNORE)
        }
        assertDecision(input(hasMediaSession = true), NotificationDecision.IGNORE)
        assertDecision(input(isDefaultDialer = true), NotificationDecision.IGNORE)
        assertDecision(input(isDefaultSms = true), NotificationDecision.IGNORE)
    }

    @Test
    fun allowListOverridesDigestAndFocusSuppression() {
        assertDecision(
            input(isAllowListed = true, focusSuppressNotifications = true, digestEnabled = true),
            NotificationDecision.IGNORE,
        )
    }

    @Test
    fun onlyDistractingAppsAreDigestedWhenDigestOrSuppressModeIsActive() {
        assertDecision(
            input(appCategory = AppCategory.NEUTRAL, digestEnabled = true),
            NotificationDecision.IGNORE,
        )
        assertDecision(
            input(appCategory = AppCategory.NEUTRAL, focusSuppressNotifications = true),
            NotificationDecision.IGNORE,
        )
        assertDecision(
            input(appCategory = AppCategory.DISTRACTING, digestEnabled = true),
            NotificationDecision.DIGEST,
        )
        assertDecision(
            input(appCategory = AppCategory.DISTRACTING, focusSuppressNotifications = true),
            NotificationDecision.DIGEST,
        )
    }

    private fun assertDecision(input: NotificationFilterInput, expected: NotificationDecision) {
        assertEquals(expected, NotificationFilter.decide(input))
    }

    private fun input(
        packageName: String = "other.app",
        isFromThisApp: Boolean = false,
        isOngoing: Boolean = false,
        isGroupSummary: Boolean = false,
        kind: NotificationKind = NotificationKind.OTHER,
        hasMediaSession: Boolean = false,
        isDefaultDialer: Boolean = false,
        isDefaultSms: Boolean = false,
        isAllowListed: Boolean = false,
        appCategory: AppCategory = AppCategory.DISTRACTING,
        focusSuppressNotifications: Boolean = false,
        digestEnabled: Boolean = true,
    ) = NotificationFilterInput(
        packageName,
        isFromThisApp,
        isOngoing,
        isGroupSummary,
        kind,
        hasMediaSession,
        isDefaultDialer,
        isDefaultSms,
        isAllowListed,
        appCategory,
        focusSuppressNotifications,
        digestEnabled,
    )
}
