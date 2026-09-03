package com.rvetiveau.rvscorecount.ui.screens.party

/**
 * Room-backed state for the freshly created Partie (AC 5, 10). [Loaded] only carries the
 * minimal Manche 1 / Donneur display owned by this story; classement temps réel, "Nouvelle
 * manche" and later flows belong to Epics 3-5.
 */
sealed interface PartyUiState {
    data object Loading : PartyUiState
    data class Loaded(val dealerName: String) : PartyUiState
    data object LoadError : PartyUiState
    data object PartyNotFound : PartyUiState
    data object InconsistentParticipants : PartyUiState
}
