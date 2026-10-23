package com.rvetiveau.rvscorecount.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rvetiveau.rvscorecount.domain.usecase.ExportBackupUseCase
import com.rvetiveau.rvscorecount.domain.usecase.ObserveBackupContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Announces what a backup holds and writes it to the file the user picked (Story 12.1). */
@HiltViewModel
class BackupViewModel @Inject constructor(
    observeBackupContentUseCase: ObserveBackupContentUseCase,
    private val exportBackupUseCase: ExportBackupUseCase
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
        val state = _uiState.value as? BackupUiState.Loaded ?: return
        if (destination == null || state.isExporting) return
        _uiState.value = state.copy(isExporting = true, exportResult = null)

        viewModelScope.launch {
            val result = try {
                exportBackupUseCase(destination)
                ExportResult.EXPORTED
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ExportResult.FAILED
            }
            _uiState.update { current ->
                if (current is BackupUiState.Loaded) current.copy(isExporting = false, exportResult = result) else current
            }
        }
    }
}
