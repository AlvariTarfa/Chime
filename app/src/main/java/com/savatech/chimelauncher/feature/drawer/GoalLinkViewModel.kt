package com.savatech.chimelauncher.feature.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.apps.AppConfigRepository
import com.savatech.chimelauncher.data.goals.GoalRepository
import com.savatech.chimelauncher.domain.model.GoalModel
import com.savatech.chimelauncher.domain.model.GoalStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GoalLinkUiState(
    val activeGoals: List<GoalModel> = emptyList(),
    val failure: Boolean = false,
)

@HiltViewModel
class GoalLinkViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val appConfigRepository: AppConfigRepository,
) : ViewModel() {
    private val failure = MutableStateFlow(false)
    val uiState: StateFlow<GoalLinkUiState> = combine(
        goalRepository.observeGoals(GoalStatus.ACTIVE),
        failure,
    ) { goals, hasFailed -> GoalLinkUiState(goals, hasFailed) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalLinkUiState())

    suspend fun setLinkedGoal(packageName: String, goalId: String?): Boolean {
        failure.value = false
        try {
            appConfigRepository.setLinkedGoal(packageName, goalId)
            return true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failure.value = true
            return false
        }
    }
}
