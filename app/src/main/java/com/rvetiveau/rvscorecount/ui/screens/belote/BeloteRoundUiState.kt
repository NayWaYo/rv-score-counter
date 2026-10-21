package com.rvetiveau.rvscorecount.ui.screens.belote

import androidx.annotation.StringRes
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundResult
import com.rvetiveau.rvscorecount.domain.model.TeamSide

sealed interface BeloteRoundUiState {
    data object Loading : BeloteRoundUiState

    /** The Partie is not in progress any more, or the corrected Manche does not exist. */
    data object Unavailable : BeloteRoundUiState

    /**
     * Saisie of a Manche: [pointsInputs] by [TeamSide] ordinal, the other Équipe's points being
     * deduced on 162; [preview] is the engine's result, reserve included, once it can be computed.
     */
    data class Loaded(
        val roundNumber: Int,
        val isCorrection: Boolean,
        val teamNames: List<String>,
        val taker: TeamSide? = null,
        val pointsInputs: List<String> = listOf("", ""),
        val beloteTeam: TeamSide? = null,
        val capotTeam: TeamSide? = null,
        val pointsError: Boolean = false,
        val preview: BeloteRoundResult? = null,
        val isSaving: Boolean = false,
        @StringRes val errorRes: Int? = null
    ) : BeloteRoundUiState {
        val canValidate: Boolean get() = preview != null && !isSaving
    }
}
