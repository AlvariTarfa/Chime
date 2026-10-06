package com.savatech.chimelauncher.feature.intercept

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.db.entities.AppGrant
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.data.intercept.InterceptRepository
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.domain.model.InterceptOutcome
import com.savatech.chimelauncher.domain.limits.isValidLimitOverrideReason
import com.savatech.chimelauncher.domain.intercept.PauseReason
import com.savatech.chimelauncher.service.GrantExpiryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class InterceptUiState(
    val appLabel: String,
    val remainingSeconds: Int,
    val priorityGoalTitle: String? = null,
    val isBusy: Boolean = false,
    val launchFailed: Boolean = false,
    val actionFailed: Boolean = false,
    val usedMillis: Long = 0L,
    val limitMinutes: Int? = null,
    val limitOverrideReason: String = "",
    val isLimitReached: Boolean = false,
)

@HiltViewModel
class InterceptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val interceptRepository: InterceptRepository,
    private val appRepository: AppRepository,
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val expiryScheduler: GrantExpiryScheduler,
    private val clock: Clock,
) : ViewModel() {
    private val packageName: String = checkNotNull(savedStateHandle["packageName"])
    private val className: String = checkNotNull(savedStateHandle["className"])
    private val userSerial: Long = checkNotNull(savedStateHandle["userSerial"])
    private val appLabel: String = checkNotNull(savedStateHandle["appLabel"])
    private val reason: String = savedStateHandle["reason"] ?: "DISTRACTING"
    private val pauseReason = PauseReason.entries.firstOrNull { it.name == reason }
        ?: PauseReason.DISTRACTING
    private val usedMillis: Long = savedStateHandle["usedMillis"] ?: 0L
    private val limitMinutes: Int? = (savedStateHandle["limitMinutes"] ?: 0).takeIf { it > 0 }
    private val endAtMillis = savedStateHandle.get<Long>(COUNTDOWN_END_KEY)
        ?: (clock.millis() + checkNotNull(savedStateHandle.get<Int>("delaySeconds"))
            .toLong() * 1_000L).also { savedStateHandle[COUNTDOWN_END_KEY] = it }

    private val mutableUiState = MutableStateFlow(
        InterceptUiState(
            appLabel = appLabel,
            remainingSeconds = remainingSeconds(),
            usedMillis = usedMillis,
            limitMinutes = limitMinutes,
            isLimitReached = pauseReason == PauseReason.LIMIT_REACHED,
        ),
    )
    val uiState: StateFlow<InterceptUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            performAction {
                interceptRepository.logEvent(
                    packageName = packageName,
                    outcome = InterceptOutcome.SHOWN,
                    reason = reason,
                )
                true
            }
        }
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            val priority = goalRepository.observeDailyPriorities(today).first()
                .sortedBy { it.position }
                .firstNotNullOfOrNull { goalRepository.getGoal(it.goalId) }
            mutableUiState.value = mutableUiState.value.copy(priorityGoalTitle = priority?.title)
        }
        viewModelScope.launch {
            while (mutableUiState.value.remainingSeconds > 0) {
                mutableUiState.value = mutableUiState.value.copy(remainingSeconds = remainingSeconds())
                if (mutableUiState.value.remainingSeconds > 0) delay(COUNTDOWN_REFRESH_MILLIS)
            }
        }
    }

    suspend fun goBack(): Boolean {
        return performAction {
            interceptRepository.logEvent(
                packageName = packageName,
                outcome = InterceptOutcome.CANCELLED,
                reason = reason,
            )
            true
        }
    }

    suspend fun openOnce(): Boolean = open(null)

    suspend fun openFor(minutes: Int): Boolean {
        require(minutes in if (pauseReason == PauseReason.LIMIT_REACHED) {
            LIMIT_GRANT_MINUTES
        } else {
            ALLOWED_GRANT_MINUTES
        })
        return open(minutes)
    }

    fun updateLimitOverrideReason(value: String) {
        mutableUiState.value = mutableUiState.value.copy(limitOverrideReason = value)
    }

    suspend fun shouldRequestNotificationPermission(): Boolean =
        !settingsRepository.notificationPermissionRequested.first()

    suspend fun markNotificationPermissionRequested() {
        settingsRepository.setNotificationPermissionRequested(true)
    }

    private suspend fun open(grantedMinutes: Int?): Boolean {
        if (mutableUiState.value.remainingSeconds > 0) return false
        val isLimitOverride = pauseReason == PauseReason.LIMIT_REACHED
        val overrideReason = mutableUiState.value.limitOverrideReason.trim()
        if (isLimitOverride && !isValidLimitOverrideReason(overrideReason)) return false
        return performAction {
            interceptRepository.logEvent(
                packageName = packageName,
                outcome = if (isLimitOverride) InterceptOutcome.LIMIT_OVERRIDE else InterceptOutcome.OPENED,
                grantedMinutes = grantedMinutes,
                reason = if (isLimitOverride) overrideReason else reason,
            )
            if (grantedMinutes != null) {
                val grant: AppGrant = interceptRepository.createGrant(packageName, grantedMinutes)
                expiryScheduler.schedule(packageName, appLabel, grant.expiresAt)
            }
            appRepository.launch(
                AppInfo(
                    label = appLabel,
                    packageName = packageName,
                    className = className,
                    userSerial = userSerial,
                    isWorkProfile = false,
                ),
            ).fold(
                onSuccess = { true },
                onFailure = {
                    mutableUiState.value = mutableUiState.value.copy(launchFailed = true)
                    false
                },
            )
        }
    }

    private suspend fun performAction(action: suspend () -> Boolean): Boolean {
        if (mutableUiState.value.isBusy) return false
        mutableUiState.value = mutableUiState.value.copy(
            isBusy = true,
            launchFailed = false,
            actionFailed = false,
        )
        return try {
            action()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            mutableUiState.value = mutableUiState.value.copy(actionFailed = true)
            false
        } catch (exception: SQLiteException) {
            mutableUiState.value = mutableUiState.value.copy(actionFailed = true)
            false
        } catch (exception: SecurityException) {
            mutableUiState.value = mutableUiState.value.copy(actionFailed = true)
            false
        } catch (exception: IllegalStateException) {
            mutableUiState.value = mutableUiState.value.copy(actionFailed = true)
            false
        } finally {
            mutableUiState.value = mutableUiState.value.copy(isBusy = false)
        }
    }

    private fun remainingSeconds(): Int =
        ((endAtMillis - clock.millis() + 999L) / 1_000L).coerceAtLeast(0L).toInt()

    private companion object {
        const val COUNTDOWN_END_KEY = "countdown_end_at"
        const val COUNTDOWN_REFRESH_MILLIS = 200L
        val ALLOWED_GRANT_MINUTES = setOf(5, 10, 15)
        val LIMIT_GRANT_MINUTES = setOf(5, 10)
    }
}
