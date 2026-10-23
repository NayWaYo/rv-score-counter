package com.rvetiveau.rvscorecount.ui.screens.backup

import com.rvetiveau.rvscorecount.domain.model.BackupContent
import java.time.LocalDate

/**
 * « Sauvegarde » (Epic 12): what a backup would hold, the [operation] running, if any, the outcome of
 * the last one and the restore waiting for confirmation.
 */
sealed interface BackupUiState {
    data object Loading : BackupUiState
    data object LoadError : BackupUiState
    data class Loaded(
        val content: BackupContent,
        val operation: BackupOperation? = null,
        val message: BackupMessage? = null,
        val pendingRestore: PendingRestore? = null
    ) : BackupUiState
}

/** One at a time: « Exporter » and « Restaurer » are off while one runs. */
enum class BackupOperation { EXPORTING, READING, RESTORING }

enum class BackupMessage(val isSuccess: Boolean) {
    EXPORTED(true),
    EXPORT_FAILED(false),
    RESTORED(true),
    RESTORE_FAILED(false),
    NOT_A_BACKUP(false),
    NEWER_FORMAT(false),
    DAMAGED(false),
    UNREADABLE(false)
}

/** A checked backup file [source], saved on [createdAt] with [content], waiting for « Remplacer » (Story 12.2). */
data class PendingRestore(val source: String, val createdAt: Long, val content: BackupContent)

/** The name the file picker proposes, such as `rv-score-counter-2026-10-23.json`. */
fun backupFileName(date: LocalDate): String = "rv-score-counter-$date.json"
