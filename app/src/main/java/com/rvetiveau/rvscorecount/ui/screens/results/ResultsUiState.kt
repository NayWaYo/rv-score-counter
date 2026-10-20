package com.rvetiveau.rvscorecount.ui.screens.results

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.ui.components.StandingRow

/**
 * Résultats finaux d'une Partie FINISHED (Story 5.1). [Loaded.standings] is sorted by Score
 * Cumulé; [Loaded.winners] are all the rank-1 rows (ties included), none before a Manche. Statistics and the Manches
 * are on the Statistiques screen, reached once [Loaded.roundCount] > 0.
 */
sealed interface ResultsUiState {
    data object Loading : ResultsUiState
    data class Loaded(
        val standings: List<StandingRow>,
        val roundCount: Int = 0,
        val isReplaying: Boolean = false,
        @StringRes val replayErrorRes: Int? = null
    ) : ResultsUiState {
        val winners: List<StandingRow> get() = if (roundCount == 0) emptyList() else standings.filter { it.rank == 1 }
    }
    data object LoadError : ResultsUiState

    /** Party missing, not FINISHED, or with an inconsistent aggregate: no results to show. */
    data object Unavailable : ResultsUiState
}
