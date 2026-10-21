package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundResult
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import java.util.UUID

sealed interface BeloteRoundUiState {
    data object Loading : BeloteRoundUiState

    /** The Partie is not in progress any more, or the corrected Manche does not exist. */
    data object Unavailable : BeloteRoundUiState

    /**
     * Saisie of a Manche: the [takerPlayerId] among [players], whose Équipe is the [taker] (Story 8.3);
     * [pointsInputs] by [TeamSide] ordinal, the other Équipe's points being deduced on 162; [preview]
     * is the engine's result, reserve included, once it can be computed.
     */
    data class Loaded(
        val roundNumber: Int,
        val isCorrection: Boolean,
        val teamNames: List<String>,
        val players: List<BeloteSeatUiModel> = emptyList(),
        val takerPlayerId: UUID? = null,
        val taker: TeamSide? = null,
        val pointsInputs: List<String> = listOf("", ""),
        val beloteTeam: TeamSide? = null,
        val capotTeam: TeamSide? = null,
        val pointsError: Boolean = false,
        val preview: BeloteRoundResult? = null,
        val isSaving: Boolean = false,
        @StringRes val errorRes: Int? = null
    ) : BeloteRoundUiState {
        /** A taker player is required, also to correct a Manche played before it was recorded (Story 8.3). */
        val canValidate: Boolean get() = preview != null && takerPlayerId != null && !isSaving
    }
}

/** A player offered as taker, in table order, with the Équipe it plays for. */
data class BeloteSeatUiModel(val id: UUID, val name: String, val side: TeamSide)
