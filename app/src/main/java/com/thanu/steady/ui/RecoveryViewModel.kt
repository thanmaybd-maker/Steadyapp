package com.thanu.steady.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanu.steady.R
import com.thanu.steady.data.RecoveryCodec
import com.thanu.steady.data.RecoveryRepository
import com.thanu.steady.data.RecoverySnapshot
import com.thanu.steady.data.PortableCodec
import com.thanu.steady.platform.ActivityAlarmAdapter
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
    val exportEnd: String = "",
    val categories: Set<String> = setOf("TASKS","HABITS","FOCUS"),
    val includeRoutes: Boolean = false,
    val backupCounts: List<Int>? = null,
    val markdownPreview: String? = null,
    val backupReady: Boolean = false,
    val restoreCounts: List<Int>? = null,
    val restoreRange: String = "",
    val importCompleted: Boolean = false,
    val legacyRestore: Boolean = false,
    val legacyPreview: String? = null,
    val legacyCounts: List<Int>? = null,
    val legacyZone: String = "",
    val legacyRange: String = "",
    val legacyResult: List<Int>? = null,
    val recordCounts: com.thanu.steady.data.OrganiserRecordCounts? = null
)

class SettingsViewModel(
    private val repository: RecoveryRepository,
    private val documentAdapter: DocumentAdapter,
    private val alarms: AlarmAdapter,
    private val notifications: NotificationAdapter,
    private val clock: Clock,
    private val deleteLocal: () -> Unit,
    private val activityAlarms: ActivityAlarmAdapter? = null,
    private val clearBootstrap: suspend () -> Unit = {}
) : ViewModel() {
    private val crypto = BackupService()
    private val exportPolicy = ExportPolicy()
    private val _uiState = MutableStateFlow(SettingsUiState(
        exportDate = LogicalDayPolicy().getLogicalDay(clock.instant()).toString()))
    val uiState = _uiState.asStateFlow()
    private var preparedBackup: ByteArray? = null
    private var preparedRestore: RecoverySnapshot? = null
    private var backupSnapshot: RecoverySnapshot? = null
    private var preparedLegacy: com.thanu.steady.data.LegacyOgImport? = null
    fun refreshRecordCounts() = work(R.string.record_counts_failed) {
        val counts=repository.recordCounts(); _uiState.update { it.copy(recordCounts=counts) }
    }
    fun copyMarkdownPreview() {
        val preview=_uiState.value.markdownPreview ?: return
        status(if(documentAdapter.copyMarkdownPreview(preview)) R.string.markdown_copied else R.string.markdown_copy_failed)
    }
    fun shareMarkdownPreview() {
        val preview=_uiState.value.markdownPreview ?: return
        status(if(documentAdapter.shareMarkdownPreview(preview)) R.string.markdown_share_opened else R.string.markdown_share_failed)
    }
    fun prepareLegacy(uri: Uri?) {
        if(uri == null) { status(R.string.file_cancelled); return }
        if(_uiState.value.isProcessing) return
        cancelLegacy()
        work(R.string.legacy_og_invalid) {
            val text = documentAdapter.readLegacyJsonFromUri(uri) ?: error("Unreadable legacy file")
            val prepared = com.thanu.steady.data.LegacyOgCodec.decode(text,repository.legacyZone(),clock.millis())
            preparedLegacy = prepared
            _uiState.update { it.copy(legacyPreview=prepared.preview,legacyCounts=prepared.counts,legacyZone=prepared.zone,
                legacyRange=listOfNotNull(prepared.firstDay,prepared.lastDay).distinct().joinToString(" – "),legacyResult=null) }
        }
    }
    fun cancelLegacy() { preparedLegacy=null; _uiState.update { it.copy(legacyPreview=null,legacyCounts=null) } }
    fun confirmLegacy() {
        val prepared = preparedLegacy ?: return
        work(R.string.legacy_og_failed) {
            val result = repository.importLegacy(prepared)
            val counts = try { repository.recordCounts() } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) { null }
            preparedLegacy=null
            _uiState.update { it.copy(legacyPreview=null,legacyCounts=null,legacyResult=listOf(result.added,result.skipped),recordCounts=counts,statusMessage=R.string.legacy_og_success) }
        }
    }
    fun setExportEnd(value: String) { _uiState.update { it.copy(exportEnd=value.take(10),markdownPreview=null) } }
    fun category(key: String,enabled: Boolean) { _uiState.update { it.copy(categories=if(enabled) it.categories+key else it.categories-key,markdownPreview=null) } }
    fun routes(enabled: Boolean) { backupSnapshot=null; _uiState.update { it.copy(includeRoutes=enabled,backupCounts=null) } }
    fun prepareBackupScope() = work(R.string.backup_failed) {
        val snapshot = repository.snapshot(_uiState.value.includeRoutes)
        backupSnapshot=snapshot
        val a=snapshot.expanded!!
        _uiState.update { it.copy(backupCounts=listOf(a.recordCount+snapshot.plans.size+snapshot.reviews.size+snapshot.timers.size,
            a.tasks.size,a.habits.size,a.sessions.size,a.water.size+a.sleep.size+a.meals.size+a.careLogs.size,a.routes.size)) }
    }
    fun cancelBackupScope() { backupSnapshot=null; _uiState.update { it.copy(backupCounts=null) } }
    fun scopeConfirmed() { _uiState.update { it.copy(backupCounts=null) } }

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
        val end=LocalDate.parse(state.exportEnd.ifBlank { state.exportDate })
        val markdown=repository.markdown(day,end,state.categories,state.includeEvidence,clock)
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
                val payload = PortableCodec.encode(requireNotNull(backupSnapshot))
                backupSnapshot=null
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
                val snapshot = PortableCodec.decode(payload)
                preparedRestore = snapshot
                _uiState.update { it.copy(
                    restoreCounts = listOf(snapshot.plans.size + (snapshot.expanded?.recordCount ?: 0), snapshot.reviews.size, snapshot.timers.size),
                    restoreRange = listOfNotNull(snapshot.firstDay, snapshot.lastDay).joinToString(" – "),legacyRestore = snapshot.expanded == null
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
            activityAlarms?.invalidatePending()
            val oldTimers = repository.replace(snapshot)
            oldTimers.forEach { alarms.cancelAlarm(it); activityAlarms?.cancel(it); activityAlarms?.cancelRest(it) }
            notifications.cancelAll()
            preparedRestore = null
            _uiState.update { it.copy(restoreCounts = null, importCompleted = true, statusMessage = R.string.restore_success) }
        }
    }
    fun importHandled() { _uiState.update { it.copy(importCompleted = false) } }
    fun clearLocalData() = work(R.string.delete_failed) {
        repository.cancelPending(alarms::cancelAlarm) { activityAlarms?.cancel(it); activityAlarms?.cancelRest(it) }
        deleteLocal()
        clearBootstrap()
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
        preparedBackup?.fill(0); preparedRestore = null; backupSnapshot=null; preparedLegacy=null
    }
}
