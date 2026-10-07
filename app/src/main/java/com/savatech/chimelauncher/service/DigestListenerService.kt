package com.savatech.chimelauncher.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import android.provider.Telephony
import com.savatech.chimelauncher.core.di.IoDispatcher
import com.savatech.chimelauncher.data.db.dao.AppConfigDao
import com.savatech.chimelauncher.data.db.dao.DigestItemDao
import com.savatech.chimelauncher.data.db.entities.DigestItem
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.data.focus.FocusRepository
import com.savatech.chimelauncher.domain.model.AppCategory
import com.savatech.chimelauncher.domain.notification.NotificationDecision
import com.savatech.chimelauncher.domain.notification.NotificationFilter
import com.savatech.chimelauncher.domain.notification.NotificationFilterInput
import com.savatech.chimelauncher.domain.notification.NotificationKind
import com.savatech.chimelauncher.domain.notification.truncateNotificationText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class DigestListenerService : NotificationListenerService() {
    @Inject lateinit var appConfigDao: AppConfigDao
    @Inject lateinit var digestItemDao: DigestItemDao
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var focusRepository: FocusRepository
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val packageName = sbn.packageName
        val filterInput = NotificationFilterInput(
            packageName = packageName,
            isFromThisApp = packageName == applicationContext.packageName,
            isOngoing = sbn.isOngoing,
            isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            kind = notification.category.toNotificationKind(),
            hasMediaSession = notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION),
            isDefaultDialer = packageName == getSystemService(TelecomManager::class.java)
                ?.defaultDialerPackage,
            isDefaultSms = packageName == Telephony.Sms.getDefaultSmsPackage(this),
            isAllowListed = false,
            appCategory = AppCategory.NEUTRAL,
            focusSuppressNotifications = false,
            digestEnabled = false,
        )
        serviceScope.launch {
            val decision = withContext(ioDispatcher) {
                val allowListed = packageName in settings.digestAllowList.first()
                val config = appConfigDao.getById(packageName)
                val activeMode = focusRepository.activeMode.first()
                NotificationFilter.decide(
                    filterInput.copy(
                        isAllowListed = allowListed,
                        appCategory = config?.category?.let(AppCategory::fromStorage)
                            ?: AppCategory.NEUTRAL,
                        focusSuppressNotifications = activeMode?.suppressNotifications == true,
                        digestEnabled = settings.digestEnabled.first(),
                    ),
                )
            }
            if (decision != NotificationDecision.DIGEST) return@launch
            withContext(ioDispatcher) {
                digestItemDao.insert(
                    DigestItem(
                        packageName = packageName,
                        title = truncateNotificationText(notification.extras.getCharSequence(Notification.EXTRA_TITLE)),
                        text = truncateNotificationText(notification.extras.getCharSequence(Notification.EXTRA_TEXT)),
                        postedAt = System.currentTimeMillis(),
                        delivered = false,
                    ),
                )
            }
            cancelNotification(sbn.key)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun String?.toNotificationKind(): NotificationKind = when (this) {
        Notification.CATEGORY_CALL -> NotificationKind.CALL
        Notification.CATEGORY_ALARM -> NotificationKind.ALARM
        Notification.CATEGORY_REMINDER -> NotificationKind.REMINDER
        Notification.CATEGORY_NAVIGATION -> NotificationKind.NAVIGATION
        else -> NotificationKind.OTHER
    }
}
