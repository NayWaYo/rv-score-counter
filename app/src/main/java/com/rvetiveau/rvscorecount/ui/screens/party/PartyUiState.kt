package com.rvetiveau.rvscorecount.ui.screens.party

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.ui.components.RoundHistoryItem
import com.rvetiveau.rvscorecount.ui.components.StandingRow

/**
 * Room-backed state of the Partie en cours (Story 4.1). [Loaded.standings] is already sorted by
 * Score Cumulé; [Loaded.roundNumber] is the Manche about to be played. [Loaded.addition] is the
 * in-memory draft of a late Participant (Story 4.2), null when no addition is in progress.
 * [Loaded.roundHistory] lists the validated Manches, most recent first (Story 4.3).
 * [Loaded.closure] tracks the « Fin de partie » confirmation (Story 5.1).
 */
sealed interface PartyUiState {
    data object Loading : PartyUiState
    data class Loaded(
        val dealerName: String,
        val roundNumber: Int = 1,
        val standings: List<StandingRow> = emptyList(),
        val canStartRound: Boolean = true,
        val maxPlayers: Int = 0,
        val addition: ParticipantAdditionUiState? = null,
        val roundHistory: List<RoundHistoryItem> = emptyList(),
        val isRoundHistoryOpen: Boolean = false,
        val closure: PartyClosureUiState = PartyClosureUiState()
    ) : PartyUiState {
        val canAddParticipant: Boolean get() = canStartRound && standings.size < maxPlayers
        val isMaxPlayersReached: Boolean get() = canStartRound && maxPlayers > 0 && standings.size >= maxPlayers
    }
    data object LoadError : PartyUiState
    data object PartyNotFound : PartyUiState
    data object InconsistentParticipants : PartyUiState
}

data class PartyClosureUiState(
    val isConfirmationOpen: Boolean = false,
    val isClosing: Boolean = false,
    @StringRes val errorRes: Int? = null
)

/** [insertionOptions] is non-empty only once a valid name was confirmed (position step). */
data class ParticipantAdditionUiState(
    val nameInput: String = "",
    val insertionOptions: List<InsertionOption> = emptyList(),
    val isSaving: Boolean = false,
    @StringRes val errorRes: Int? = null
) {
    val isChoosingPosition: Boolean get() = insertionOptions.isNotEmpty()
}

/** Seat right after [afterName] at the table, i.e. listPosition [position]. */
data class InsertionOption(
    val position: Int,
    val afterName: String,
    val becomesDealer: Boolean
)
