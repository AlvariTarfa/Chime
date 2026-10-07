package com.savatech.chimelauncher.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.graphics.PixelFormat
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import java.io.IOException
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.savatech.chimelauncher.core.launch.InterceptCoordinator
import com.savatech.chimelauncher.core.launch.InterceptEvaluation
import com.savatech.chimelauncher.core.launch.PauseLaunchArgs
import com.savatech.chimelauncher.core.theme.ChimeTheme
import com.savatech.chimelauncher.data.intercept.InterceptRepository
import com.savatech.chimelauncher.domain.intercept.InterceptDecision
import com.savatech.chimelauncher.domain.model.InterceptOutcome
import com.savatech.chimelauncher.domain.intercept.PauseReason
import com.savatech.chimelauncher.domain.limits.isValidLimitOverrideReason
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.savatech.chimelauncher.core.di.IoDispatcher

@AndroidEntryPoint
class ForegroundWatcherService : AccessibilityService() {
    @Inject lateinit var coordinator: InterceptCoordinator
    @Inject lateinit var interceptRepository: InterceptRepository
    @Inject lateinit var grantExpiryScheduler: GrantExpiryScheduler
    @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

    private val debouncer = ForegroundPackageDebouncer()
    private var serviceScope: CoroutineScope? = null
    private var packageRules: ForegroundPackageRules? = null
    private var homePackages: Set<String> = emptySet()
    private var inputMethodPackages: Set<String> = emptySet()
    private var currentForegroundPackage: String? = null
    private var activePausePackage: String? = null
    private var decisionJob: Job? = null
    private var decisionGeneration = 0
    private var overlayView: ComposeView? = null
    private var overlayOwners: AccessibilityOverlayOwners? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Foreground watcher operation failed.", throwable)
        }
        serviceScope = CoroutineScope(SupervisorJob() + ioDispatcher + exceptionHandler)
        serviceScope?.launch {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val homePackage = packageManager.resolveActivity(
                homeIntent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
            )?.activityInfo?.packageName
            val imes = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_INPUT_METHODS,
            ).orEmpty().split(':')
                .mapNotNull(ComponentName::unflattenFromString)
                .map(ComponentName::getPackageName)
                .toSet()
            withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                homePackages = setOfNotNull(homePackage)
                inputMethodPackages = imes
                packageRules = ForegroundPackageRules(
                    appPackageName = packageName,
                    systemUiPackageName = SYSTEM_UI_PACKAGE,
                    homePackages = homePackages,
                    inputMethodPackages = inputMethodPackages,
                )
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val eventPackageName = event.packageName?.toString() ?: return
        if (eventPackageName == packageName) return

        if (eventPackageName in homePackages) {
            currentForegroundPackage = eventPackageName
            clearOverlay()
            return
        }
        if (
            eventPackageName == SYSTEM_UI_PACKAGE ||
            eventPackageName in inputMethodPackages ||
            packageRules?.shouldIgnore(eventPackageName) != false
        ) {
            return
        }

        if (currentForegroundPackage != eventPackageName) {
            currentForegroundPackage = eventPackageName
            clearOverlay()
        }
        if (!debouncer.shouldProcess(eventPackageName, SystemClock.elapsedRealtime())) return
        if (activePausePackage == eventPackageName) return

        decisionJob?.cancel()
        val generation = ++decisionGeneration
        decisionJob = serviceScope?.launch {
            val evaluation = coordinator.evaluate(eventPackageName)
            when (val decision = evaluation.decision) {
                InterceptDecision.Allow -> Unit
                is InterceptDecision.Pause -> {
                    withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                        if (generation == decisionGeneration &&
                            currentForegroundPackage == eventPackageName
                        ) {
                            showPause(eventPackageName, evaluation, decision)
                        }
                    }
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        clearOverlay()
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }

    private fun showPause(
        packageName: String,
        evaluation: InterceptEvaluation,
        decision: InterceptDecision.Pause,
    ) {
        clearOverlay()
        val args = PauseLaunchArgs(
            packageName = packageName,
            className = "",
            userSerial = 0L,
            appLabel = packageName,
            delaySeconds = decision.delaySeconds,
            reason = decision.reason,
            usedMillis = evaluation.usedMillis,
            limitMinutes = evaluation.limitMinutes.takeIf {
                decision.reason == PauseReason.LIMIT_REACHED
            },
        )
        val owners = AccessibilityOverlayOwners().also(AccessibilityOverlayOwners::resume)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owners)
            setViewTreeViewModelStoreOwner(owners)
            setViewTreeSavedStateRegistryOwner(owners)
            setContent {
                ChimeTheme {
                    ForegroundPauseOverlay(
                        args = args,
                        onGoBack = ::goHome,
                        onContinue = { grantMinutes, reason, onError ->
                            continueAfterPause(args, grantMinutes, reason, onError)
                        },
                        onShown = {
                            serviceScope?.launch {
                                interceptRepository.logEvent(
                                    args.packageName,
                                    InterceptOutcome.SHOWN,
                                    reason = args.reason.name,
                                )
                            }
                        },
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.CENTER }
        try {
            getSystemService(WindowManager::class.java).addView(view, params)
            overlayView = view
            overlayOwners = owners
            activePausePackage = packageName
        } catch (exception: SecurityException) {
            owners.destroy()
            Log.e(TAG, "Could not display accessibility pause overlay.", exception)
        } catch (exception: IllegalStateException) {
            owners.destroy()
            Log.e(TAG, "Could not display accessibility pause overlay.", exception)
        }
    }

    private fun goHome(args: PauseLaunchArgs) {
        serviceScope?.launch {
            interceptRepository.logEvent(
                args.packageName,
                InterceptOutcome.CANCELLED,
                reason = args.reason.name,
            )
        }
        clearOverlay()
        if (!performGlobalAction(GLOBAL_ACTION_HOME)) {
            Log.e(TAG, "Android did not perform the Home global action.")
        }
    }

    private fun continueAfterPause(
        args: PauseLaunchArgs,
        grantMinutes: Int?,
        overrideReason: String,
        onError: () -> Unit,
    ) {
        serviceScope?.launch {
            try {
                val limitOverride = args.reason == PauseReason.LIMIT_REACHED
                val normalizedReason = overrideReason.trim()
                if (limitOverride && !isValidLimitOverrideReason(normalizedReason)) return@launch
                interceptRepository.logEvent(
                    packageName = args.packageName,
                    outcome = if (limitOverride) {
                        InterceptOutcome.LIMIT_OVERRIDE
                    } else {
                        InterceptOutcome.OPENED
                    },
                    grantedMinutes = grantMinutes,
                    reason = if (limitOverride) normalizedReason else args.reason.name,
                )
                if (grantMinutes != null) {
                    val grant = interceptRepository.createGrant(args.packageName, grantMinutes)
                    grantExpiryScheduler.schedule(args.packageName, args.appLabel, grant.expiresAt)
                }
                withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                    if (activePausePackage == args.packageName) clearOverlay()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: IOException) {
                Log.e(TAG, "Could not continue from accessibility pause.", exception)
                withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { onError() }
            } catch (exception: SQLiteException) {
                Log.e(TAG, "Could not continue from accessibility pause.", exception)
                withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { onError() }
            } catch (exception: SecurityException) {
                Log.e(TAG, "Could not continue from accessibility pause.", exception)
                withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { onError() }
            } catch (exception: IllegalStateException) {
                Log.e(TAG, "Could not continue from accessibility pause.", exception)
                withContext(kotlinx.coroutines.Dispatchers.Main.immediate) { onError() }
            }
        }
    }

    private fun clearOverlay() {
        decisionGeneration++
        decisionJob?.cancel()
        decisionJob = null
        val view = overlayView
        if (view != null) {
            try {
                getSystemService(WindowManager::class.java).removeView(view)
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Pause overlay was already removed.", exception)
            }
        }
        overlayView = null
        overlayOwners?.destroy()
        overlayOwners = null
        activePausePackage = null
    }

    private companion object {
        const val TAG = "ForegroundWatcher"
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
    }
}
