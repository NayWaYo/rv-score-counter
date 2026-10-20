package com.rvetiveau.rvscorecount.ui.screens.partydetail

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.ui.components.StandingRow

/**
 * Détail d'une Partie FINISHED (Story 6.2): final Classement sorted by Score Cumulé, read-only.
 * Statistics and the Manches are on the Statistiques screen, reached once [Loaded.roundCount] > 0.
 */
sealed interface PartyDetailUiState {
    data object Loading : PartyDetailUiState
    data class Loaded(
        val gameId: String,
        val createdAt: Long,
        val standings: List<StandingRow>,
        val roundCount: Int,
        val isReplaying: Boolean = false,
        @StringRes val replayErrorRes: Int? = null
    ) : PartyDetailUiState {
        val winners: List<StandingRow> get() = if (roundCount == 0) emptyList() else standings.filter { it.rank == 1 }
    }
    data object LoadError : PartyDetailUiState

    /** Party missing, not FINISHED, or with an inconsistent aggregate. */
    data object Unavailable : PartyDetailUiState
}
