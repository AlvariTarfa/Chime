package com.savatech.chimelauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.activity.compose.BackHandler
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.savatech.chimelauncher.core.navigation.AppNavHost
import com.savatech.chimelauncher.core.navigation.Routes
import com.savatech.chimelauncher.core.theme.ChimeTheme
import com.savatech.chimelauncher.data.focus.FocusRepository
import com.savatech.chimelauncher.data.focus.FocusSessionRepository
import com.savatech.chimelauncher.data.settings.FontPreset
import com.savatech.chimelauncher.data.settings.LayoutDensityPreset
import com.savatech.chimelauncher.data.settings.SettingsRepository
import com.savatech.chimelauncher.data.settings.ThemeMode
import com.savatech.chimelauncher.service.SchedulerFacade
import com.savatech.chimelauncher.core.di.IoDispatcher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineDispatcher
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import android.os.Build
import android.graphics.ColorMatrixColorFilter

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var focusRepository: FocusRepository
    @Inject lateinit var focusSessionRepository: FocusSessionRepository
    @Inject lateinit var schedulerFacade: SchedulerFacade
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
    private val notificationRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptNotificationIntent(intent)
        lifecycleScope.launch(ioDispatcher) { focusRepository.seedBuiltInModesIfNeeded() }
        lifecycleScope.launch(ioDispatcher) { focusSessionRepository.reconcileCompletedSessions() }
        lifecycleScope.launch { schedulerFacade.rescheduleAll() }
        setContent {
            val activeMode by focusRepository.activeMode.collectAsStateWithLifecycle(initialValue = null)
            val bedtimeMode = activeMode?.id == "builtin-sleep"
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val dynamicColor by settingsRepository.useDynamicColor.collectAsStateWithLifecycle(true)
            val accentColorIndex by settingsRepository.accentColorIndex.collectAsStateWithLifecycle(0)
            val fontPreset by settingsRepository.fontPreset.collectAsStateWithLifecycle(FontPreset.DEFAULT)
            val densityPreset by settingsRepository.layoutDensity.collectAsStateWithLifecycle(
                LayoutDensityPreset.COMFORTABLE,
            )
            ChimeTheme(
                themeMode = themeMode,
                dynamicColorEnabled = dynamicColor,
                accentColorIndex = accentColorIndex,
                fontPreset = fontPreset,
                densityPreset = densityPreset,
                bedtimeMode = bedtimeMode,
            ) {
                val navController = rememberNavController()
                val route = notificationRoute.value
                LaunchedEffect(route) {
                    route?.let {
                        navController.navigate(it) { launchSingleTop = true }
                        notificationRoute.value = null
                    }
                }
                val backStackEntry = navController.currentBackStackEntryAsState()
                BackHandler(enabled = backStackEntry.value?.destination?.route == Routes.Home) {}
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    val grayscale = remember {
                        android.graphics.ColorMatrix().apply { setSaturation(0f) }
                    }
                    Box(
                        Modifier.fillMaxSize().then(
                            if (bedtimeMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.graphicsLayer {
                                    renderEffect = android.graphics.RenderEffect
                                        .createColorFilterEffect(ColorMatrixColorFilter(grayscale))
                                        .asComposeRenderEffect()
                                }
                            } else {
                                Modifier
                            },
                        ),
                    ) {
                        AppNavHost(navController = navController)
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(ioDispatcher) { focusSessionRepository.reconcileCompletedSessions() }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptNotificationIntent(intent)
    }

    private fun acceptNotificationIntent(intent: android.content.Intent?) {
        val route = intent?.getStringExtra(EXTRA_NOTIFICATION_ROUTE) ?: return
        notificationRoute.value = route
    }

    companion object {
        const val EXTRA_NOTIFICATION_ROUTE = "notification_route"
        const val EXTRA_CHECK_IN_TYPE = "check_in_type"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_GOAL_ID = "goal_id"
    }
}
