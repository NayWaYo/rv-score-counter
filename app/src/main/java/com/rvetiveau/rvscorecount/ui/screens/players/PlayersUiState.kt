package com.rvetiveau.rvscorecount.ui.screens.players

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import java.util.UUID

/** « Joueurs » (Story 10.2): [Loaded.players] in the order of the suggestions. */
sealed interface PlayersUiState {
    data object Loading : PlayersUiState
    data object Empty : PlayersUiState
    data object LoadError : PlayersUiState
    data class Loaded(
        val players: List<PlayerSummary>,
        val renaming: PlayerRenaming? = null
    ) : PlayersUiState
}

/**
 * The rename dialog of [playerId], open until the new name is accepted or the dialog dismissed.
 * [errorRes] takes [takenBy] as its argument when the name belongs to another Joueur.
 */
data class PlayerRenaming(
    val playerId: UUID,
    val currentName: String,
    val input: String,
    @StringRes val errorRes: Int? = null,
    val takenBy: String? = null,
    val isSaving: Boolean = false
)
