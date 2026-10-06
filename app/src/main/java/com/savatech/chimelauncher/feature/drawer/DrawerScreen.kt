package com.savatech.chimelauncher.feature.drawer

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.apps.AppConfigResult
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.data.apps.IconCache
import com.savatech.chimelauncher.data.settings.DrawerMode
import com.savatech.chimelauncher.core.theme.LocalLayoutMetrics
import com.savatech.chimelauncher.core.launch.LaunchResult
import com.savatech.chimelauncher.core.launch.PauseLaunchArgs
import com.savatech.chimelauncher.feature.limits.DailyLimitWarningEffect
import com.savatech.chimelauncher.feature.focus.FocusModeDrawerBanner
import kotlinx.coroutines.launch

@Composable
fun DrawerScreen(
    onOpenHiddenApps: () -> Unit,
    onOpenGoals: () -> Unit,
    onNeedsPause: (PauseLaunchArgs) -> Unit,
    viewModel: DrawerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val layout = LocalLayoutMetrics.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val launchFailureMessage = stringResource(R.string.launch_failed)
    val pinLimitMessage = stringResource(R.string.pin_limit_reached)
    val appActionFailedMessage = stringResource(R.string.app_action_failed)
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    DailyLimitWarningEffect(snackbarHostState, viewModel::dailyLimitWarnings)

    fun launchApp(app: AppInfo) {
        coroutineScope.launch {
            when (val result = viewModel.requestLaunch(app)) {
                LaunchResult.Started -> Unit
                is LaunchResult.NeedsPause -> onNeedsPause(result.args)
                is LaunchResult.Failed -> snackbarHostState.showSnackbar(
                    result.error.localizedMessage ?: launchFailureMessage,
                )
            }
        }
    }

    fun togglePin(app: AppInfo, pinned: Boolean) {
        coroutineScope.launch {
            if (viewModel.setPinned(app.packageName, pinned) == AppConfigResult.PinLimitExceeded) {
                snackbarHostState.showSnackbar(pinLimitMessage)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(Modifier.fillMaxSize().padding(contentPadding)) {
            FocusModeDrawerBanner()
            TextButton(onClick = onOpenGoals) {
                Text(stringResource(R.string.goals_title))
            }
            TextField(
                value = uiState.query,
                onValueChange = { query ->
                    viewModel.updateQuery(query)
                    if (query == HIDDEN_APPS_QUERY) onOpenHiddenApps()
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_apps)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (uiState.apps.size == 1) launchApp(uiState.apps.single())
                }),
            )
            Box(Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                    uiState.apps.isEmpty() -> Text(
                        text = stringResource(R.string.no_apps_found),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    uiState.drawerMode == DrawerMode.GRID -> LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                    ) {
                        gridItems(uiState.apps, key = { it.key }) { app ->
                            GridAppItem(
                                app = app,
                                iconCache = viewModel.iconCache,
                                iconShape = uiState.iconShape,
                                iconPackPackage = uiState.iconPackPackage,
                                onClick = { launchApp(app) },
                                onLongClick = { selectedApp = app },
                            )
                        }
                    }
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(uiState.apps, key = { it.key }) { app ->
                            ListAppItem(
                                app = app,
                                showIcon = uiState.drawerMode == DrawerMode.ICONS,
                                iconShape = uiState.iconShape,
                                iconPackPackage = uiState.iconPackPackage,
                                rowHeight = layout.rowHeight.dp,
                                horizontalPadding = layout.horizontalPadding.dp,
                                verticalPadding = layout.verticalPadding.dp,
                                itemSpacing = layout.itemSpacing.dp,
                                viewModel = viewModel,
                                onClick = { launchApp(app) },
                                onLongClick = { selectedApp = app },
                            )
                        }
                    }
                }
                if (uiState.query.isBlank() && uiState.apps.isNotEmpty() &&
                    uiState.drawerMode != DrawerMode.GRID
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(end = 4.dp)
                            .pointerInput(uiState.apps, uiState.drawerMode) {
                                fun scrollToOffset(y: Float) {
                                    val entryIndex = (y / size.height * AlphabetIndex.entries.size)
                                        .toInt()
                                        .coerceIn(0, AlphabetIndex.entries.lastIndex)
                                    val entry = AlphabetIndex.entries[entryIndex]
                                    val appIndex = uiState.apps.indexOfFirst {
                                        AlphabetIndex.bucketFor(it.label) == entry
                                    }
                                    if (appIndex >= 0) {
                                        coroutineScope.launch { listState.scrollToItem(appIndex) }
                                    }
                                }
                                detectDragGestures(
                                    onDragStart = { scrollToOffset(it.y) },
                                ) { change, _ -> scrollToOffset(change.position.y) }
                            },
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        AlphabetIndex.entries.forEach { entry ->
                            Text(
                                text = entry,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    selectedApp?.let { app ->
        val config = uiState.configs[app.packageName]
        AppActionsSheet(
            app = app,
            config = config,
            iconCache = viewModel.iconCache,
            iconShape = uiState.iconShape,
            iconPackPackage = uiState.iconPackPackage,
            onDismiss = { selectedApp = null },
            onPin = { pinned -> togglePin(app, pinned) },
            onHide = {
                coroutineScope.launch { viewModel.hide(app.packageName) }
                selectedApp = null
            },
            onCategory = { category ->
                coroutineScope.launch { viewModel.setCategory(app.packageName, category) }
                selectedApp = null
            },
            onDailyLimit = { minutes ->
                coroutineScope.launch { viewModel.setDailyLimit(app.packageName, minutes) }
            },
            onSystemActionFailed = {
                coroutineScope.launch { snackbarHostState.showSnackbar(appActionFailedMessage) }
            },
        )
    }
}

@Composable
private fun ListAppItem(
    app: AppInfo,
    showIcon: Boolean,
    iconShape: com.savatech.chimelauncher.data.settings.IconShape,
    iconPackPackage: String?,
    rowHeight: androidx.compose.ui.unit.Dp,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    verticalPadding: androidx.compose.ui.unit.Dp,
    itemSpacing: androidx.compose.ui.unit.Dp,
    viewModel: DrawerViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = rowHeight)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showIcon) AppIcon(app, viewModel.iconCache, 40.dp, iconShape, iconPackPackage)
        Text(app.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (app.isWorkProfile) {
            Text(stringResource(R.string.work_profile), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GridAppItem(
    app: AppInfo,
    iconCache: IconCache,
    iconShape: com.savatech.chimelauncher.data.settings.IconShape,
    iconPackPackage: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val layout = LocalLayoutMetrics.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(
                horizontal = (layout.horizontalPadding / 4).dp,
                vertical = layout.verticalPadding.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(layout.itemSpacing.dp),
    ) {
        AppIcon(app, iconCache, 48.dp, iconShape, iconPackPackage)
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
        )
    }
}

private const val HIDDEN_APPS_QUERY = ":hidden"
