package com.rvetiveau.rvscorecount.ui.screens.results

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.ui.components.RoundRecordUi
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.SevenChampionUi
import com.rvetiveau.rvscorecount.ui.components.StandingRow

/**
 * Résultats finaux d'une Partie FINISHED (Story 5.1). [Loaded.standings] is sorted by Score
 * Cumulé; [Loaded.winners] are all the rank-1 rows (ties included). [Loaded.roundTable] is the
 * score sheet of the Partie.
 */
sealed interface ResultsUiState {
    data object Loading : ResultsUiState
    data class Loaded(
        val standings: List<StandingRow>,
        val sevenChampion: SevenChampionUi? = null,
        val roundRecord: RoundRecordUi? = null,
        val roundTable: RoundTable = RoundTable(),
        val isReplaying: Boolean = false,
        @StringRes val replayErrorRes: Int? = null
    ) : ResultsUiState {
        val winners: List<StandingRow> get() = standings.filter { it.rank == 1 }
    }
    data object LoadError : ResultsUiState

    /** Party missing, not FINISHED, or with an inconsistent aggregate: no results to show. */
    data object Unavailable : ResultsUiState
}
