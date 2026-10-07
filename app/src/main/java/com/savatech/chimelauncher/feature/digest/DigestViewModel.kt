package com.savatech.chimelauncher.feature.digest

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.core.di.IoDispatcher
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.db.dao.DigestItemDao
import com.savatech.chimelauncher.data.db.entities.DigestItem
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.service.NotificationListenerAccess
import com.savatech.chimelauncher.service.SchedulerRescheduler
import com.savatech.chimelauncher.service.work.DigestWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DigestAppGroup(
    val packageName: String,
    val label: String,
    val items: List<DigestItem>,
)

@HiltViewModel
class DigestViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val digestItemDao: DigestItemDao,
    private val appRepository: AppRepository,
    private val scheduler: SchedulerRescheduler,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val digestEnabled = settings.digestEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )
    val digestFirstTime = settings.digestFirstTime.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "12:00",
    )
    val digestSecondTime = settings.digestSecondTime.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "18:00",
    )
    val digestConsentAccepted = settings.digestConsentAccepted.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false,
    )
    val allowListedPackages = settings.digestAllowList.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet(),
    )
    val availableApps = appRepository.apps
    private val mutableListenerAccess = kotlinx.coroutines.flow.MutableStateFlow(false)
    val listenerAccess: StateFlow<Boolean> = mutableListenerAccess
    val digestGroups: StateFlow<List<DigestAppGroup>> = combine(
        digestItemDao.observeAll(),
        appRepository.apps,
    ) { items, apps ->
        val labels = apps.associate { it.packageName to it.label }
        items.groupBy(DigestItem::packageName)
            .map { (packageName, appItems) ->
                DigestAppGroup(
                    packageName,
                    labels[packageName] ?: packageName,
                    appItems.sortedByDescending(DigestItem::postedAt),
                )
            }
            .sortedBy(DigestAppGroup::label)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshListenerAccess() {
        viewModelScope.launch {
            mutableListenerAccess.value = withContext(ioDispatcher) {
                NotificationListenerAccess.isEnabled(context)
            }
        }
    }

    fun setDigestEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDigestEnabled(enabled)
            scheduler.rescheduleAll()
        }
    }

    suspend fun acceptConsentAndEnableDigest() {
        settings.setDigestConsentAccepted()
        settings.setDigestEnabled(true)
        scheduler.rescheduleAll()
    }

    fun setDigestTime(slot: String, value: String) {
        viewModelScope.launch {
            when (slot) {
                DigestWorker.MIDDAY -> settings.setDigestFirstTime(value)
                DigestWorker.EVENING -> settings.setDigestSecondTime(value)
                else -> error("Unknown digest schedule slot: $slot")
            }
            scheduler.rescheduleAll()
        }
    }

    fun setAllowListed(packageName: String, allowed: Boolean) {
        viewModelScope.launch { settings.setDigestAllowListed(packageName, allowed) }
    }

    fun clearAll() {
        viewModelScope.launch {
            digestItemDao.deleteAll()
            NotificationManagerCompat.from(context).cancel(DigestWorker.NOTIFICATION_ID)
        }
    }
}
