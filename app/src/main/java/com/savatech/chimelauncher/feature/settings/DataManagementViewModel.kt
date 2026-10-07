package com.savatech.chimelauncher.feature.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savatech.chimelauncher.data.backup.BackupRepository
import com.savatech.chimelauncher.data.backup.PrivacyRepository
import com.savatech.chimelauncher.domain.backup.BackupError
import com.savatech.chimelauncher.domain.backup.BackupImportMode
import com.savatech.chimelauncher.domain.backup.ImportReport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DataManagementState(
    val isWorking: Boolean = false,
    val exported: Boolean = false,
    val mergePreview: ImportReport? = null,
    val replacePreview: ImportReport? = null,
    val report: ImportReport? = null,
    val backupError: BackupError? = null,
    val operationFailed: Boolean = false,
    val deleted: Boolean = false,
)

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val privacyRepository: PrivacyRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DataManagementState())
    val state = mutableState.asStateFlow()
    private var importSource: String? = null

    fun writeExport(uri: Uri) {
        viewModelScope.launch {
            mutableState.value = DataManagementState(isWorking = true)
            try {
                val content = backupRepository.export()
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri)
                        ?: throw IOException("Unable to open the selected export location.")
                    stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
                }
                mutableState.value = DataManagementState(exported = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Log.e(TAG, "Backup export failed.", exception)
                mutableState.value = DataManagementState(operationFailed = true)
            }
        }
    }

    fun readImport(uri: Uri) {
        viewModelScope.launch {
            mutableState.value = DataManagementState(isWorking = true)
            try {
                val source = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw IOException("Unable to open the selected backup.")
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                }
                val merge = backupRepository.previewImport(source, BackupImportMode.MERGE)
                val replace = backupRepository.previewImport(source, BackupImportMode.REPLACE)
                importSource = source
                mutableState.value = DataManagementState(
                    mergePreview = merge,
                    replacePreview = replace,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: BackupError) {
                mutableState.value = DataManagementState(backupError = error)
            } catch (exception: Exception) {
                Log.e(TAG, "Backup preview failed.", exception)
                mutableState.value = DataManagementState(operationFailed = true)
            }
        }
    }

    fun import(mode: BackupImportMode) {
        val source = importSource ?: return
        viewModelScope.launch {
            mutableState.value = DataManagementState(isWorking = true)
            try {
                val report = backupRepository.import(source, mode)
                importSource = null
                mutableState.value = DataManagementState(report = report)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: BackupError) {
                mutableState.value = DataManagementState(backupError = error)
            } catch (exception: Exception) {
                Log.e(TAG, "Backup import failed.", exception)
                mutableState.value = DataManagementState(operationFailed = true)
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            mutableState.value = DataManagementState(isWorking = true)
            try {
                privacyRepository.deleteAllData()
                mutableState.value = DataManagementState(deleted = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Log.e(TAG, "Deleting local data failed.", exception)
                mutableState.value = DataManagementState(operationFailed = true)
            }
        }
    }

    fun clearReport() {
        mutableState.value = DataManagementState()
    }

    fun clearError() {
        importSource = null
        mutableState.value = DataManagementState()
    }

    private companion object {
        const val TAG = "ChimeDataManagement"
    }
}
