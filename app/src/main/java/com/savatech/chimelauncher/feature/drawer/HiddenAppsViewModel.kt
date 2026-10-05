package com.savatech.chimelauncher.feature.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.apps.AppConfigRepository
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HiddenAppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class HiddenAppsViewModel @Inject constructor(
    private val repository: AppRepository,
    private val appConfigRepository: AppConfigRepository,
) : ViewModel() {
    val uiState: StateFlow<HiddenAppsUiState> = combine(
        repository.apps,
        repository.isLoaded,
        appConfigRepository.observeAll(),
    ) { apps, isLoaded, configs ->
        HiddenAppsUiState(
            apps = apps.filter { configs[it.packageName]?.hidden == true },
            isLoading = !isLoaded,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HiddenAppsUiState())

    fun unhide(packageName: String) {
        viewModelScope.launch { appConfigRepository.setHidden(packageName, false) }
    }
}
