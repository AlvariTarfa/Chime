package com.savatech.chimelauncher.feature.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.savatech.chimelauncher.R

@Composable
fun AccessibilityDisclosureScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var launchFailed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.accessibility_disclosure_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(R.string.accessibility_disclosure_prominent),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(stringResource(R.string.accessibility_disclosure_body))
        Text(stringResource(R.string.accessibility_disclosure_disable))
        if (launchFailed) {
            Text(
                stringResource(R.string.accessibility_settings_failed),
                color = MaterialTheme.colorScheme.error,
            )
        }
        TextButton(onClick = {
            try {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                launchFailed = false
            } catch (_: ActivityNotFoundException) {
                launchFailed = true
            } catch (_: SecurityException) {
                launchFailed = true
            }
        }) {
            Text(stringResource(R.string.accessibility_continue_to_settings))
        }
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.intercept_go_back))
        }
    }
}
