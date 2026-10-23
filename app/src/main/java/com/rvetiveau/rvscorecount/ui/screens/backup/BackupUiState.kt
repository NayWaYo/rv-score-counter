package com.rvetiveau.rvscorecount.ui.screens.backup

import com.rvetiveau.rvscorecount.domain.model.BackupContent
import java.time.LocalDate

/** « Sauvegarde » (Story 12.1): what a backup would hold, and how the last export went. */
sealed interface BackupUiState {
    data object Loading : BackupUiState
    data object LoadError : BackupUiState
    data class Loaded(
        val content: BackupContent,
        val isExporting: Boolean = false,
        val exportResult: ExportResult? = null
    ) : BackupUiState
}

enum class ExportResult { EXPORTED, FAILED }

/** The name the file picker proposes, such as `rv-score-counter-2026-10-23.json`. */
fun backupFileName(date: LocalDate): String = "rv-score-counter-$date.json"
