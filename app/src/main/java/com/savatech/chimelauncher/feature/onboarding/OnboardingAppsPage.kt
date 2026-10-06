package com.savatech.chimelauncher.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.domain.model.AppCategory

@Composable
fun OnboardingAppsPage(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val visibleApps = state.apps.filter {
        state.searchQuery.isBlank() ||
            it.label.contains(state.searchQuery, ignoreCase = true) ||
            it.packageName.contains(state.searchQuery, ignoreCase = true)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.onboarding_classify_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_classify_body), style = MaterialTheme.typography.bodySmall)
        if (state.hasExistingClassifications) {
            Text(stringResource(R.string.onboarding_existing_classifications))
        }
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::updateSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboarding_search_apps)) },
            singleLine = true,
        )
        TextButton(onClick = viewModel::markAllNeutral) {
            Text(stringResource(R.string.onboarding_mark_all_neutral))
        }
        if (visibleApps.isEmpty()) {
            Text(stringResource(R.string.onboarding_no_apps_found))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(visibleApps, key = AppInfo::packageName) { app ->
                    AppClassificationRow(
                        app = app,
                        selected = state.classifications[app.packageName] ?: AppCategory.NEUTRAL,
                        onSelected = { viewModel.setCategory(app.packageName, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppClassificationRow(
    app: AppInfo,
    selected: AppCategory,
    onSelected: (AppCategory) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(app.label, style = MaterialTheme.typography.titleSmall)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppCategory.entries.forEach { category ->
                FilterChip(
                    selected = selected == category,
                    onClick = { onSelected(category) },
                    label = {
                        Text(
                            stringResource(
                                when (category) {
                                    AppCategory.PRODUCTIVE -> R.string.onboarding_category_productive
                                    AppCategory.NEUTRAL -> R.string.onboarding_category_neutral
                                    AppCategory.DISTRACTING -> R.string.onboarding_category_distracting
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}
