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
import com.savatech.chimelauncher.core.di.IoDispatcher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineDispatcher
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.os.Build
import android.graphics.ColorMatrixColorFilter

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var focusRepository: FocusRepository
    @Inject lateinit var focusSessionRepository: FocusSessionRepository
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(ioDispatcher) { focusRepository.seedBuiltInModesIfNeeded() }
        lifecycleScope.launch(ioDispatcher) { focusSessionRepository.reconcileCompletedSessions() }
        setContent {
            val activeMode by focusRepository.activeMode.collectAsStateWithLifecycle(initialValue = null)
            val bedtimeMode = activeMode?.id == "builtin-sleep"
            ChimeTheme(bedtimeMode = bedtimeMode) {
                val navController = rememberNavController()
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
}
