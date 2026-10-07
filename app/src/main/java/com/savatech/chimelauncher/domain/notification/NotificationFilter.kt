package com.savatech.chimelauncher.domain.notification

import com.savatech.chimelauncher.domain.model.AppCategory

enum class NotificationKind {
    CALL,
    ALARM,
    REMINDER,
    NAVIGATION,
    OTHER,
}

data class NotificationFilterInput(
    val packageName: String,
    val isFromThisApp: Boolean,
    val isOngoing: Boolean,
    val isGroupSummary: Boolean,
    val kind: NotificationKind,
    val hasMediaSession: Boolean,
    val isDefaultDialer: Boolean,
    val isDefaultSms: Boolean,
    val isAllowListed: Boolean,
    val appCategory: AppCategory,
    val focusSuppressNotifications: Boolean,
    val digestEnabled: Boolean,
)

enum class NotificationDecision {
    IGNORE,
    DIGEST,
}

object NotificationFilter {
    fun decide(input: NotificationFilterInput): NotificationDecision {
        if (input.isOngoing || input.isGroupSummary || input.isFromThisApp) {
            return NotificationDecision.IGNORE
        }
        if (
            input.kind in setOf(
                NotificationKind.CALL,
                NotificationKind.ALARM,
                NotificationKind.REMINDER,
                NotificationKind.NAVIGATION,
            ) ||
            input.hasMediaSession ||
            input.isDefaultDialer ||
            input.isDefaultSms
        ) {
            return NotificationDecision.IGNORE
        }
        if (input.isAllowListed) return NotificationDecision.IGNORE
        return if (
            input.appCategory == AppCategory.DISTRACTING &&
            (input.focusSuppressNotifications || input.digestEnabled)
        ) {
            NotificationDecision.DIGEST
        } else {
            NotificationDecision.IGNORE
        }
    }
}
