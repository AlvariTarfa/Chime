package com.savatech.chimelauncher.feature.limits

import android.database.sqlite.SQLiteException
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.savatech.chimelauncher.R
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun DailyLimitWarningEffect(
    snackbarHostState: SnackbarHostState,
    checkWarnings: suspend () -> List<String>,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val locale = LocalConfiguration.current.locales[0]
    val warningTemplate = stringResource(R.string.daily_limit_warning)
    val checkFailedMessage = stringResource(R.string.daily_limit_check_failed)
    LaunchedEffect(lifecycleOwner, snackbarHostState, checkWarnings) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                try {
                    checkWarnings().forEach { appLabel ->
                        snackbarHostState.showSnackbar(
                            String.format(locale, warningTemplate, appLabel),
                        )
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: IOException) {
                    snackbarHostState.showSnackbar(checkFailedMessage)
                    break
                } catch (exception: SQLiteException) {
                    snackbarHostState.showSnackbar(checkFailedMessage)
                    break
                } catch (exception: SecurityException) {
                    snackbarHostState.showSnackbar(checkFailedMessage)
                    break
                } catch (exception: IllegalStateException) {
                    snackbarHostState.showSnackbar(checkFailedMessage)
                    break
                }
                delay(CHECK_INTERVAL_MILLIS)
            }
        }
    }
}

private const val CHECK_INTERVAL_MILLIS = 60_000L
