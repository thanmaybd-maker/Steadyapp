package com.thanu.steady.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.R
import com.thanu.steady.data.RecoveryCodec
import com.thanu.steady.data.RecoveryRepository
import com.thanu.steady.data.RecoverySnapshot
import com.thanu.steady.domain.BackupService
import com.thanu.steady.domain.ExportPolicy
import com.thanu.steady.domain.LogicalDayPolicy
import com.thanu.steady.platform.AlarmAdapter
import com.thanu.steady.platform.DocumentAdapter
import com.thanu.steady.platform.NotificationAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate

data class SettingsUiState(
    val statusMessage: Int? = null,
    val isProcessing: Boolean = false,
    val includeSensitiveExport: Boolean = false,
    val includeEvidence: Boolean = false,
    val includeIndicators: Boolean = false,
    val exportDate: String = "",
    val markdownPreview: String? = null,
    val backupReady: Boolean = false,
    val restoreCounts: List<Int>? = null,
    val restoreRange: String = "",
    val importCompleted: Boolean = false
)

class SettingsViewModel(
    private val repository: RecoveryRepository,
    private val documentAdapter: DocumentAdapter,
    private val alarms: AlarmAdapter,
    private val notifications: NotificationAdapter,
    private val clock: Clock,
    private val deleteLocal: () -> Unit
) : ViewModel() {
    private val crypto = BackupService()
    private val exportPolicy = ExportPolicy()
    private val _uiState = MutableStateFlow(SettingsUiState(
        exportDate = LogicalDayPolicy().getLogicalDay(clock.instant()).toString()))
    val uiState = _uiState.asStateFlow()
    private var preparedBackup: ByteArray? = null
    private var preparedRestore: RecoverySnapshot? = null

    fun toggleSensitiveExport(enabled: Boolean) {
        _uiState.update { it.copy(includeSensitiveExport = enabled, markdownPreview = null) }
    }
    fun setExportDate(value: String) {
        _uiState.update { it.copy(exportDate = value.take(10), markdownPreview = null) }
    }
    fun toggleEvidence(enabled: Boolean) { _uiState.update { it.copy(includeEvidence = enabled, markdownPreview = null) } }
    fun toggleIndicators(enabled: Boolean) { _uiState.update { it.copy(includeIndicators = enabled, markdownPreview = null) } }
    fun clearPreview() { _uiState.update { it.copy(markdownPreview = null) } }
    fun prepareMarkdown() = work(R.string.export_failed) {
        val state = _uiState.value
        val day = LocalDate.parse(state.exportDate)
        val (plan, review) = repository.markdownRecords(day)
        val markdown = exportPolicy.generateMarkdown(day, plan, review, state.includeSensitiveExport,
            state.includeEvidence, state.includeIndicators)
        _uiState.update { it.copy(markdownPreview = markdown) }
    }
    fun exportMarkdown(uri: Uri?) {
        if (uri == null) { status(R.string.file_cancelled); return }
        val markdown = _uiState.value.markdownPreview ?: return
        work(R.string.export_failed) {
            check(documentAdapter.writeMarkdownToUri(uri, markdown))
            status(R.string.export_success)
            clearPreview()
        }
    }
    fun prepareBackup(passphrase: CharArray) {
        if (_uiState.value.isProcessing) { passphrase.fill('\u0000'); return }
        work(R.string.backup_failed) {
            try {
                val payload = RecoveryCodec.encode(repository.snapshot())
                preparedBackup?.fill(0)
                preparedBackup = crypto.createEncryptedBackup(payload, passphrase)
                _uiState.update { it.copy(backupReady = true) }
            } finally { passphrase.fill('\u0000') }
        }
    }
    fun backupPickerLaunched() { _uiState.update { it.copy(backupReady = false) } }
    fun writeBackup(uri: Uri?) {
        if (uri == null) {
            preparedBackup?.fill(0); preparedBackup = null
            status(R.string.file_cancelled); return
        }
        val bytes = preparedBackup ?: return
        work(R.string.backup_failed) {
            try {
                check(documentAdapter.writeBackupToUri(uri, bytes))
                status(R.string.backup_success)
            } finally { bytes.fill(0); preparedBackup = null }
        }
    }
    fun prepareRestore(uri: Uri, passphrase: CharArray) {
        if (_uiState.value.isProcessing) { passphrase.fill('\u0000'); return }
        work(R.string.restore_invalid) {
            try {
                val bytes = documentAdapter.readBackupFromUri(uri) ?: error("Unreadable archive")
                val payload = try { crypto.restoreEncryptedBackup(bytes, passphrase) } finally { bytes.fill(0) }
                    ?: error("Invalid archive")
                val snapshot = RecoveryCodec.decode(payload)
                preparedRestore = snapshot
                _uiState.update { it.copy(
                    restoreCounts = listOf(snapshot.plans.size, snapshot.reviews.size, snapshot.timers.size),
                    restoreRange = listOfNotNull(snapshot.firstDay, snapshot.lastDay).joinToString(" – ")
                ) }
            } finally { passphrase.fill('\u0000') }
        }
    }
    fun cancelRestore() {
        preparedRestore = null
        _uiState.update { it.copy(restoreCounts = null, restoreRange = "") }
    }
    fun confirmRestore() {
        val snapshot = preparedRestore ?: return
        work(R.string.restore_failed) {
            val oldTimers = repository.replace(snapshot)
            oldTimers.forEach(alarms::cancelAlarm)
            notifications.cancelAll()
            preparedRestore = null
            _uiState.update { it.copy(restoreCounts = null, importCompleted = true, statusMessage = R.string.restore_success) }
        }
    }
    fun importHandled() { _uiState.update { it.copy(importCompleted = false) } }
    fun clearLocalData() = work(R.string.delete_failed) {
        val timers = repository.snapshot().timers
        timers.forEach { alarms.cancelAlarm(it.id) }
        deleteLocal()
        _uiState.update { it.copy(importCompleted = true, statusMessage = R.string.delete_success) }
    }
    fun status(message: Int) { _uiState.update { it.copy(statusMessage = message) } }
    private fun work(failureMessage: Int, block: suspend () -> Unit) {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(isProcessing = true, statusMessage = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                status(failureMessage)
            } finally { _uiState.update { it.copy(isProcessing = false) } }
        }
    }
    override fun onCleared() {
        preparedBackup?.fill(0); preparedRestore = null
    }
}
