package com.savatech.chimelauncher.feature.digest

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.data.apps.AppInfo
import com.savatech.chimelauncher.service.work.DigestWorker
import java.time.LocalTime
import kotlinx.coroutines.launch

@Composable
fun DigestSettingsScreen(
    onOpenConsent: () -> Unit,
    onOpenAllowList: () -> Unit,
    onOpenItems: () -> Unit,
    viewModel: DigestViewModel = hiltViewModel(),
) {
    val enabled by viewModel.digestEnabled.collectAsStateWithLifecycle()
    val firstTime by viewModel.digestFirstTime.collectAsStateWithLifecycle()
    val secondTime by viewModel.digestSecondTime.collectAsStateWithLifecycle()
    val consentAccepted by viewModel.digestConsentAccepted.collectAsStateWithLifecycle()
    val listenerAccess by viewModel.listenerAccess.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        viewModel.refreshListenerAccess()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshListenerAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.digest_settings_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.digest_settings_explanation))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = enabled,
                onCheckedChange = { shouldEnable ->
                    if (!shouldEnable) {
                        viewModel.setDigestEnabled(false)
                    } else if (!consentAccepted) {
                        onOpenConsent()
                    } else {
                        viewModel.setDigestEnabled(true)
                        if (!listenerAccess) openListenerSettings(context)
                    }
                },
            )
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.digest_enable_title))
                Text(
                    stringResource(
                        if (listenerAccess) R.string.digest_access_granted
                        else R.string.digest_access_not_granted,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        TextButton(onClick = {
            if (consentAccepted) openListenerSettings(context) else onOpenConsent()
        }) {
            Text(stringResource(R.string.digest_manage_access))
        }
        Text(stringResource(R.string.digest_schedule_title), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = {
            showDigestTimePicker(context, firstTime) {
                viewModel.setDigestTime(DigestWorker.MIDDAY, it)
            }
        }) {
            Text(stringResource(R.string.digest_first_time, firstTime))
        }
        TextButton(onClick = {
            showDigestTimePicker(context, secondTime) {
                viewModel.setDigestTime(DigestWorker.EVENING, it)
            }
        }) {
            Text(stringResource(R.string.digest_second_time, secondTime))
        }
        TextButton(onClick = onOpenAllowList) {
            Text(stringResource(R.string.digest_allow_list_title))
        }
        TextButton(onClick = onOpenItems) {
            Text(stringResource(R.string.digest_view_items))
        }
        Text(stringResource(R.string.digest_irreversible_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun DigestConsentScreen(
    onBack: () -> Unit,
    viewModel: DigestViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.digest_consent_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.digest_consent_access_details))
        Text(stringResource(R.string.digest_consent_storage_details))
        Text(stringResource(R.string.digest_consent_turn_off_details))
        Text(stringResource(R.string.digest_irreversible_note))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
            TextButton(onClick = {
                scope.launch {
                    viewModel.acceptConsentAndEnableDigest()
                    openListenerSettings(context)
                    onBack()
                }
            }) {
                Text(stringResource(R.string.digest_consent_continue))
            }
        }
    }
}

@Composable
fun DigestAllowListScreen(viewModel: DigestViewModel = hiltViewModel()) {
    val apps by viewModel.availableApps.collectAsStateWithLifecycle()
    val allowList by viewModel.allowListedPackages.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.digest_allow_list_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.digest_allow_list_explanation))
        apps.distinctBy(AppInfo::packageName).sortedBy(AppInfo::label).forEach { app ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = app.packageName in allowList,
                    onCheckedChange = { viewModel.setAllowListed(app.packageName, it) },
                )
                Text(app.label, modifier = Modifier.weight(1f))
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun DigestItemsScreen(viewModel: DigestViewModel = hiltViewModel()) {
    val groups by viewModel.digestGroups.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.digest_items_title), style = MaterialTheme.typography.headlineMedium)
        TextButton(onClick = viewModel::clearAll) {
            Text(stringResource(R.string.digest_clear_all))
        }
        if (groups.isEmpty()) {
            Text(stringResource(R.string.digest_empty))
        }
        groups.forEach { group ->
            Text(group.label, style = MaterialTheme.typography.titleMedium)
            group.items.forEach { item ->
                Text(item.title.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                item.text?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            HorizontalDivider()
        }
    }
}

private fun openListenerSettings(context: android.content.Context) {
    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}

private fun showDigestTimePicker(
    context: android.content.Context,
    value: String,
    onSelected: (String) -> Unit,
) {
    val time = LocalTime.parse(value)
    TimePickerDialog(context, { _, hour, minute ->
        onSelected("%02d:%02d".format(hour, minute))
    }, time.hour, time.minute, true).show()
}
