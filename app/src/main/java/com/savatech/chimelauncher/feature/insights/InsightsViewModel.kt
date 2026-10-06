package com.savatech.chimelauncher.feature.insights

import android.net.Uri
import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.core.di.IoDispatcher
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.data.usage.UsagePermission
import com.savatech.chimelauncher.domain.insights.GetInsightsUseCase
import com.savatech.chimelauncher.domain.insights.RangeInsight
import com.savatech.chimelauncher.domain.insights.insightsCsv
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InsightsUiState(
    val report: RangeInsight? = null,
    val dayCount: Int = 7,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val usagePermissionGranted: Boolean = false,
    val estimatedMinutesSaved: Int = 0,
    val focusScoreEnabled: Boolean = false,
    val exportFailed: Boolean = false,
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getInsights: GetInsightsUseCase,
    private val usagePermission: UsagePermission,
    private val settingsRepository: SettingsRepository,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val mutableState = MutableStateFlow(InsightsUiState())
    val state: StateFlow<InsightsUiState> = mutableState.asStateFlow()

    init {
        usagePermission.refresh()
        viewModelScope.launch {
            settingsRepository.focusScoreEnabled.collect { enabled ->
                val changed = mutableState.value.focusScoreEnabled != enabled
                mutableState.value = mutableState.value.copy(
                    usagePermissionGranted = usagePermission.hasPermission.value,
                    focusScoreEnabled = enabled,
                )
                if (changed || mutableState.value.isLoading) load()
            }
        }
    }

    fun selectDays(dayCount: Int) {
        require(dayCount == 7 || dayCount == 30)
        if (mutableState.value.dayCount == dayCount) return
        mutableState.value = mutableState.value.copy(dayCount = dayCount)
        load()
    }

    fun refreshPermission() {
        usagePermission.refresh()
        mutableState.value = mutableState.value.copy(
            usagePermissionGranted = usagePermission.hasPermission.value,
        )
        load()
    }

    fun export(uri: Uri) {
        viewModelScope.launch {
            val dayCount = mutableState.value.dayCount
            try {
                withContext(ioDispatcher) {
                    val csv = insightsCsv(getInsights.csvRows(dayCount))
                    val output = context.contentResolver.openOutputStream(uri)
                        ?: throw IOException("The selected document could not be opened.")
                    output.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(csv) }
                }
                mutableState.value = mutableState.value.copy(exportFailed = false)
            } catch (_: IOException) {
                mutableState.value = mutableState.value.copy(exportFailed = true)
            } catch (_: SecurityException) {
                mutableState.value = mutableState.value.copy(exportFailed = true)
            }
        }
    }

    fun usageAccessSettingsIntent() = usagePermission.settingsIntent()

    private fun load() {
        viewModelScope.launch {
            val current = mutableState.value
            mutableState.value = current.copy(isLoading = true, loadFailed = false)
            if (!usagePermission.hasPermission.value) {
                mutableState.value = mutableState.value.copy(report = null, isLoading = false)
                return@launch
            }
            val report = try {
                withContext(ioDispatcher) {
                    getInsights(current.dayCount, current.focusScoreEnabled)
                }
            } catch (_: SecurityException) {
                usagePermission.refresh()
                mutableState.value = mutableState.value.copy(
                    report = null,
                    isLoading = false,
                    loadFailed = true,
                    usagePermissionGranted = usagePermission.hasPermission.value,
                )
                return@launch
            } catch (_: SQLiteException) {
                mutableState.value = mutableState.value.copy(
                    report = null,
                    isLoading = false,
                    loadFailed = true,
                )
                return@launch
            }
            val assumedMinutes = settingsRepository.assumedMinutesPerOpen.first()
            val saved = report.days.lastOrNull()?.let {
                it.interceptsCancelled * assumedMinutes
            } ?: 0
            mutableState.value = mutableState.value.copy(
                report = report,
                isLoading = false,
                estimatedMinutesSaved = saved,
            )
        }
    }
}
