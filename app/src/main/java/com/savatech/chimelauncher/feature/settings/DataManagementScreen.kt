package com.savatech.chimelauncher.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savatech.chimelauncher.R
import com.savatech.chimelauncher.domain.backup.BackupError
import com.savatech.chimelauncher.domain.backup.BackupImportMode
import com.savatech.chimelauncher.domain.backup.BackupIssueReason
import com.savatech.chimelauncher.domain.backup.BackupRecordType
import com.savatech.chimelauncher.domain.backup.ImportReport

@Composable
fun DataManagementScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(BackupImportMode.MERGE) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::writeExport) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::readImport) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    state.report?.let { report ->
        ImportResultScreen(report = report, onDone = viewModel::clearReport)
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.data_management_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.backup_description))
        Button(
            enabled = !state.isWorking,
            onClick = { exportLauncher.launch("chime-backup.json") },
        ) { Text(stringResource(R.string.backup_export)) }
        TextButton(
            enabled = !state.isWorking,
            onClick = { importLauncher.launch(arrayOf("application/json", "application/*")) },
        ) { Text(stringResource(R.string.backup_import)) }
        if (state.exported) Text(stringResource(R.string.backup_export_complete))
        if (state.isWorking) CircularProgressIndicator()
        state.backupError?.let { error ->
            Text(
                text = backupErrorText(error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.operationFailed) {
            Text(
                stringResource(R.string.data_operation_failed),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.privacy_stored_intro))
        Text(stringResource(R.string.privacy_stored_goals))
        Text(stringResource(R.string.privacy_stored_logs))
        Text(stringResource(R.string.privacy_stored_app_settings))
        Text(stringResource(R.string.privacy_stored_focus))
        Text(stringResource(R.string.privacy_stored_checkins))
        Text(stringResource(R.string.privacy_stored_usage))
        Text(stringResource(R.string.privacy_stored_notifications))
        Text(stringResource(R.string.privacy_not_sent))
        Text(stringResource(R.string.privacy_manual_backups))
        Text(stringResource(R.string.privacy_android_backup_excluded))
        Button(
            enabled = !state.isWorking,
            onClick = {
                deleteConfirmation = ""
                showDeleteConfirmation = true
            },
        ) { Text(stringResource(R.string.privacy_delete_all)) }
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }

    val mergePreview = state.mergePreview
    val replacePreview = state.replacePreview
    if (mergePreview != null && replacePreview != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text(stringResource(R.string.backup_import_confirmation_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.backup_import_confirmation_body))
                    Text(stringResource(R.string.backup_merge_explanation))
                    ModeChoice(
                        mode = BackupImportMode.MERGE,
                        selectedMode = selectedMode,
                        report = mergePreview,
                        onSelect = { selectedMode = it },
                    )
                    ModeChoice(
                        mode = BackupImportMode.REPLACE,
                        selectedMode = selectedMode,
                        report = replacePreview,
                        onSelect = { selectedMode = it },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.import(selectedMode) },
                    enabled = !state.isWorking,
                ) { Text(stringResource(R.string.backup_import_action)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.privacy_delete_confirmation_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.privacy_delete_confirmation_body))
                    OutlinedTextField(
                        value = deleteConfirmation,
                        onValueChange = { deleteConfirmation = it },
                        label = { Text(stringResource(R.string.privacy_delete_confirmation_hint)) },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = deleteConfirmation == "DELETE" && !state.isWorking,
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteAllData()
                    },
                ) { Text(stringResource(R.string.privacy_delete_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ModeChoice(
    mode: BackupImportMode,
    selectedMode: BackupImportMode,
    report: ImportReport,
    onSelect: (BackupImportMode) -> Unit,
) {
    val label = if (mode == BackupImportMode.MERGE) {
        R.string.backup_merge_label
    } else {
        R.string.backup_replace_label
    }
    Row(
        modifier = Modifier.fillMaxWidth().selectable(
            selected = mode == selectedMode,
            onClick = { onSelect(mode) },
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = mode == selectedMode, onClick = { onSelect(mode) })
        Column(Modifier.padding(start = 8.dp)) {
            Text(stringResource(label))
            Text(
                stringResource(
                    R.string.backup_preview_counts,
                    report.inserted,
                    report.updated,
                    report.skipped,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ImportResultScreen(report: ImportReport, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.backup_result_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.backup_result_inserted, report.inserted))
        Text(stringResource(R.string.backup_result_updated, report.updated))
        Text(stringResource(R.string.backup_result_skipped, report.skipped))
        report.errors.forEach { error ->
            Text(
                stringResource(
                    R.string.backup_import_row_error,
                    stringResource(error.recordType.labelResource()),
                    error.identifier,
                    stringResource(error.reason.labelResource()),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(onClick = onDone) { Text(stringResource(R.string.done)) }
    }
}

@Composable
private fun backupErrorText(error: BackupError): String = when (error) {
    is BackupError.NewerSchemaVersion -> stringResource(
        R.string.backup_newer_schema,
        error.found,
        error.supported,
    )
    is BackupError.InvalidSchemaVersion -> stringResource(R.string.backup_invalid_schema, error.found)
    is BackupError.MalformedJson -> stringResource(R.string.backup_malformed_json)
    is BackupError.InvalidSettings -> stringResource(R.string.backup_invalid_setting, error.key)
}

@StringRes
private fun BackupRecordType.labelResource(): Int = when (this) {
    BackupRecordType.GOAL -> R.string.backup_record_goal
    BackupRecordType.TASK -> R.string.backup_record_task
    BackupRecordType.TASK_LOG -> R.string.backup_record_task_log
    BackupRecordType.DAILY_PRIORITY -> R.string.backup_record_daily_priority
    BackupRecordType.APP_CONFIG -> R.string.backup_record_app_config
    BackupRecordType.FOCUS_MODE -> R.string.backup_record_focus_mode
    BackupRecordType.FOCUS_SESSION -> R.string.backup_record_focus_session
    BackupRecordType.CHECK_IN -> R.string.backup_record_check_in
    BackupRecordType.INTERCEPT_EVENT -> R.string.backup_record_intercept_event
}

@StringRes
private fun BackupIssueReason.labelResource(): Int = when (this) {
    BackupIssueReason.EMPTY_ID -> R.string.backup_issue_empty_id
    BackupIssueReason.EMPTY_PACKAGE_NAME -> R.string.backup_issue_empty_package
    BackupIssueReason.MISSING_GOAL -> R.string.backup_issue_missing_goal
    BackupIssueReason.MISSING_TASK -> R.string.backup_issue_missing_task
    BackupIssueReason.INVALID_LINK -> R.string.backup_issue_invalid_link
    BackupIssueReason.DUPLICATE_ROW -> R.string.backup_issue_duplicate_row
}
