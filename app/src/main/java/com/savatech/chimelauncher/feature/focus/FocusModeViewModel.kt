package com.savatech.chimelauncher.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.database.sqlite.SQLiteException
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.AppRepository
import com.savatech.chimelauncher.data.db.entities.FocusMode
import com.savatech.chimelauncher.data.focus.FocusRepository
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.GoalStatus
import java.time.LocalDate
import com.savatech.chimelauncher.domain.focus.FocusSchedule
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.io.IOException
import kotlinx.serialization.encodeToString

data class FocusModesUiState(
    val modes: List<FocusMode> = emptyList(),
    val apps: List<AppInfo> = emptyList(),
    val activeMode: FocusMode? = null,
    val query: String = "",
    val tomorrowPriorityTitle: String? = null,
    val error: Boolean = false,
)

@HiltViewModel
class FocusModeViewModel @Inject constructor(
    private val focusRepository: FocusRepository,
    goalRepository: GoalRepository,
    appRepository: AppRepository,
    private val clock: Clock,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val error = MutableStateFlow(false)
    private val tomorrowDate = LocalDate.now(clock).plusDays(1)
    private val tomorrowPriority = combine(
        goalRepository.observeDailyPriorities(tomorrowDate),
        goalRepository.observeGoals(GoalStatus.ACTIVE),
    ) { priorities, goals ->
        val firstId = priorities.minByOrNull { it.position }?.goalId
        goals.firstOrNull { it.id == firstId }?.title
    }

    val uiState: StateFlow<FocusModesUiState> = combine(
        focusRepository.observeModes(),
        appRepository.apps,
        focusRepository.activeMode,
        query,
        tomorrowPriority,
    ) { modes, apps, activeMode, currentQuery, tomorrowTitle ->
        FocusModesUiState(
            modes = modes.sortedWith(compareByDescending(FocusMode::isBuiltIn).thenBy(FocusMode::name)),
            apps = apps.filter {
                currentQuery.isBlank() || it.label.contains(currentQuery, ignoreCase = true) ||
                    it.packageName.contains(currentQuery, ignoreCase = true)
            },
            activeMode = activeMode,
            query = currentQuery,
            tomorrowPriorityTitle = tomorrowTitle,
        )
    }.combine(error) { state, hasError ->
        state.copy(error = hasError)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusModesUiState())

    fun updateQuery(value: String) {
        query.value = value
    }

    fun setMode(modeId: String) {
        perform { focusRepository.setManualMode(modeId) }
    }

    fun pauseFor15Minutes() {
        perform {
            focusRepository.setManualMode(
                null,
                LocalDateTime.now(clock).plusMinutes(15),
            )
        }
    }

    fun endMode() {
        perform {
            focusRepository.setManualMode(
                null,
                LocalDateTime.now(clock).plusMinutes(1),
            )
        }
    }

    fun saveMode(
        id: String?,
        name: String,
        allowedPackages: Set<String>,
        schedule: FocusSchedule?,
        suppressNotifications: Boolean,
    ) {
        require(name.isNotBlank()) { "Focus mode name cannot be blank." }
        val existing = uiState.value.modes.firstOrNull { it.id == id }
        val mode = FocusMode(
            id = existing?.id ?: "custom-${UUID.randomUUID()}",
            name = name.trim(),
            allowedPackages = encodePackageList(allowedPackages),
            scheduleJson = schedule?.toJson(),
            suppressNotifications = suppressNotifications,
            isBuiltIn = existing?.isBuiltIn ?: false,
        )
        perform { focusRepository.save(mode) }
    }

    fun deleteMode(mode: FocusMode) {
        perform { focusRepository.delete(mode) }
    }

    private fun perform(action: suspend () -> Unit) {
        viewModelScope.launch {
            error.value = false
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalArgumentException) {
                error.value = true
            } catch (_: IllegalStateException) {
                error.value = true
            } catch (_: SecurityException) {
                error.value = true
            } catch (_: IOException) {
                error.value = true
            } catch (_: SQLiteException) {
                error.value = true
            }
        }
    }
}

fun decodePackageList(value: String): Set<String> =
    kotlinx.serialization.json.Json.decodeFromString<List<String>>(value).toSet()

private fun encodePackageList(packages: Set<String>): String =
    kotlinx.serialization.json.Json.encodeToString(packages.sorted().toList())
