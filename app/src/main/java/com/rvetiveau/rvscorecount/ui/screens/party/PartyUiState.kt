package com.rvetiveau.rvscorecount.ui.screens.party

import java.util.UUID

/**
 * Room-backed state of the Partie en cours (Story 4.1). [Loaded.standings] is already sorted by
 * Score Cumulé; [Loaded.roundNumber] is the Manche about to be played.
 */
sealed interface PartyUiState {
    data object Loading : PartyUiState
    data class Loaded(
        val dealerName: String,
        val roundNumber: Int = 1,
        val standings: List<StandingRow> = emptyList(),
        val canStartRound: Boolean = true
    ) : PartyUiState
    data object LoadError : PartyUiState
    data object PartyNotFound : PartyUiState
    data object InconsistentParticipants : PartyUiState
}

data class StandingRow(
    val participantId: UUID,
    val rank: Int,
    val name: String,
    val cumulativeScore: Int,
    val isDealer: Boolean
)
