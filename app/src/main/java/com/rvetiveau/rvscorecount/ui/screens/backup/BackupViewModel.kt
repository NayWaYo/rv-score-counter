package com.rvetiveau.rvscorecount.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.model.BackupReading
import com.rvetiveau.rvscorecount.domain.usecase.ExportBackupUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveBackupContentUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ReadBackupUseCase
import com.rvetiveau.rvscorecount.domain.usecase.RestoreBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Announces what a backup holds, writes it to the file the user picked (Story 12.1) and restores one
 * after confirmation (Story 12.2).
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    observeBackupContentUseCase: ObserveBackupContentUseCase,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val readBackupUseCase: ReadBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Loading)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeBackupContentUseCase()
                .catch { _uiState.value = BackupUiState.LoadError }
                .collect { content ->
                    _uiState.update { state ->
                        if (state is BackupUiState.Loaded) state.copy(content = content) else BackupUiState.Loaded(content)
                    }
                }
        }
    }

    /** [destination] is the document the file picker returned; null when the user cancelled it. */
    fun onExportDestinationChosen(destination: String?) {
        if (destination == null || !start(BackupOperation.EXPORTING)) return
        viewModelScope.launch {
            val message = try {
                exportBackupUseCase(destination)
                BackupMessage.EXPORTED
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                BackupMessage.EXPORT_FAILED
            }
            finish(message)
        }
    }

    /** [source] is the backup the file picker returned; null when the user cancelled it. */
    fun onRestoreSourceChosen(source: String?) {
        if (source == null || !start(BackupOperation.READING)) return
        viewModelScope.launch {
            val reading = try {
                readBackupUseCase(source)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                BackupReading.Unreadable
            }
            if (reading is BackupReading.Valid) {
                updateLoaded { it.copy(operation = null, pendingRestore = PendingRestore(source, reading.createdAt, reading.content)) }
            } else {
                finish(reading.refusal())
            }
        }
    }

    fun onRestoreConfirmed() {
        val pending = (_uiState.value as? BackupUiState.Loaded)?.pendingRestore ?: return
        if (!start(BackupOperation.RESTORING)) return
        viewModelScope.launch {
            val message = try {
                when (val reading = restoreBackupUseCase(pending.source)) {
                    is BackupReading.Valid -> BackupMessage.RESTORED
                    else -> reading.refusal()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                BackupMessage.RESTORE_FAILED
            }
            finish(message)
        }
    }

    fun onRestoreDismissed() {
        updateLoaded { it.copy(pendingRestore = null) }
    }

    /** False when another operation runs or the screen is not loaded. */
    private fun start(operation: BackupOperation): Boolean {
        var started = false
        updateLoaded { state ->
            started = state.operation == null
            if (started) state.copy(operation = operation, message = null, pendingRestore = null) else state
        }
        return started
    }

    private fun finish(message: BackupMessage) {
        updateLoaded { it.copy(operation = null, message = message) }
    }

    private fun updateLoaded(transform: (BackupUiState.Loaded) -> BackupUiState.Loaded) {
        _uiState.update { state -> if (state is BackupUiState.Loaded) transform(state) else state }
    }

    private fun BackupReading.refusal(): BackupMessage = when (this) {
        BackupReading.NotABackup -> BackupMessage.NOT_A_BACKUP
        BackupReading.NewerFormat -> BackupMessage.NEWER_FORMAT
        BackupReading.Damaged -> BackupMessage.DAMAGED
        BackupReading.Unreadable, is BackupReading.Valid -> BackupMessage.UNREADABLE
    }
}
