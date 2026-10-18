package com.rvetiveau.rvscorecount.ui.screens.partydetail

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.ui.components.RoundHistoryItem
import com.rvetiveau.rvscorecount.ui.components.StandingRow

/**
 * Détail d'une Partie FINISHED (Story 6.2): final Classement sorted by Score Cumulé and the
 * Manches in chronological order, read-only.
 */
sealed interface PartyDetailUiState {
    data object Loading : PartyDetailUiState
    data class Loaded(
        val gameId: String,
        val createdAt: Long,
        val standings: List<StandingRow>,
        val rounds: List<RoundHistoryItem>,
        val isReplaying: Boolean = false,
        @StringRes val replayErrorRes: Int? = null
    ) : PartyDetailUiState {
        val winners: List<StandingRow> get() = if (rounds.isEmpty()) emptyList() else standings.filter { it.rank == 1 }
    }
    data object LoadError : PartyDetailUiState

    /** Party missing, not FINISHED, or with an inconsistent aggregate. */
    data object Unavailable : PartyDetailUiState
}
