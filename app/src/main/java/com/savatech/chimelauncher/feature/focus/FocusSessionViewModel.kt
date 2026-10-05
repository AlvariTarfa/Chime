package com.savatech.chimelauncher.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.db.entities.FocusSession
import com.savatech.chimelauncher.data.focus.FocusSessionRepository
import java.time.Clock
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import android.database.sqlite.SQLiteException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusSessionUiState(
    val session: FocusSession? = null,
    val remainingMillis: Long = 0,
    val error: Boolean = false,
    val completed: Boolean = false,
)

@HiltViewModel
class FocusSessionViewModel @Inject constructor(
    private val repository: FocusSessionRepository,
    private val clock: Clock,
) : ViewModel() {
    private val error = MutableStateFlow(false)
    private val completionRequested = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<FocusSessionUiState> = combine(
        repository.observeActiveSession(),
        repository.observeLatestSession(),
        secondTicker(clock),
        error,
    ) { session, latest, now, hasError ->
        val remaining = session?.let {
            com.savatech.chimelauncher.domain.focus.remainingSessionMillis(
                it.startedAt,
                it.plannedMinutes,
                now,
            )
        } ?: 0L
        if (session != null && remaining == 0L && completionRequested.value != session.id) {
            completionRequested.value = session.id
            viewModelScope.launch {
                try {
                    repository.reconcileCompletedSessions(now)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: IllegalArgumentException) {
                    error.value = true
                } catch (_: IllegalStateException) {
                    error.value = true
                } catch (_: SecurityException) {
                    error.value = true
                } catch (_: SQLiteException) {
                    error.value = true
                }
            }
        }
        FocusSessionUiState(
            session = session,
            remainingMillis = remaining,
            error = hasError,
            completed = session == null && latest?.completed == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusSessionUiState())

    fun start(goalId: String?, taskId: String?, minutes: Int) {
        viewModelScope.launch {
            error.value = false
            try {
                repository.start(goalId, taskId, minutes)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalArgumentException) {
                error.value = true
            } catch (_: IllegalStateException) {
                error.value = true
            } catch (_: SecurityException) {
                error.value = true
            } catch (_: SQLiteException) {
                error.value = true
            }
        }
    }

    fun endEarly() {
        val id = uiState.value.session?.id ?: return
        viewModelScope.launch {
            try {
                repository.endEarly(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalArgumentException) {
                error.value = true
            } catch (_: IllegalStateException) {
                error.value = true
            } catch (_: SecurityException) {
                error.value = true
            } catch (_: SQLiteException) {
                error.value = true
            }
        }
    }
}

private fun secondTicker(clock: Clock) = flow {
    while (true) {
        emit(clock.millis())
        delay(1_000)
    }
}
