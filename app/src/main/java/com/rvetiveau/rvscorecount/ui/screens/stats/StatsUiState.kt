package com.rvetiveau.rvscorecount.ui.screens.stats

import com.rvetiveau.rvscorecount.ui.components.RoundRecordUi
import com.rvetiveau.rvscorecount.ui.components.RoundTable
import com.rvetiveau.rvscorecount.ui.components.SevenChampionUi
import com.rvetiveau.rvscorecount.ui.components.StandingRow

/**
 * Statistiques d'une Partie, en cours ou terminée : Classement avec les statistiques dépliables
 * de chaque Participant, cartes de statistiques, courbe et tableau des Manches.
 * [Loaded.canCorrectRounds] : la Partie est en cours, une Manche du tableau peut être corrigée.
 */
sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Loaded(
        val standings: List<StandingRow>,
        val roundTable: RoundTable,
        val sevenChampion: SevenChampionUi? = null,
        val roundRecord: RoundRecordUi? = null,
        val canCorrectRounds: Boolean = false
    ) : StatsUiState
    data object LoadError : StatsUiState

    /** Party missing or with an inconsistent aggregate. */
    data object Unavailable : StatsUiState
}
