package com.rvetiveau.rvscorecount.ui.screens.history

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.domain.model.PartySummary
import java.util.UUID

/**
 * Historique des Parties terminées (Story 6.1). [Loaded.entries] are most recent first;
 * [Loaded.pendingDeletionId] is the entry awaiting the « Supprimer cette partie ? » confirmation.
 */
sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Empty : HistoryUiState
    data class Loaded(
        val entries: List<PartySummary>,
        val pendingDeletionId: UUID? = null,
        val isDeleting: Boolean = false,
        @StringRes val deleteErrorRes: Int? = null
    ) : HistoryUiState
    data object LoadError : HistoryUiState
}
