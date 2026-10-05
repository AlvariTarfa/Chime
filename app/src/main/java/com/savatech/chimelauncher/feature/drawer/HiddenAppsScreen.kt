package com.savatech.chimelauncher.feature.drawer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R

@Composable
fun HiddenAppsScreen(viewModel: HiddenAppsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when {
        uiState.isLoading -> CircularProgressIndicator(Modifier.padding(24.dp))
        uiState.apps.isEmpty() -> Text(
            text = stringResource(R.string.no_hidden_apps),
            modifier = Modifier.padding(24.dp),
        )
        else -> LazyColumn(Modifier.fillMaxSize()) {
            items(uiState.apps, key = { it.key }) { app ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(app.label)
                    TextButton(onClick = { viewModel.unhide(app.packageName) }) {
                        Text(stringResource(R.string.unhide_app))
                    }
                }
            }
        }
    }
}
