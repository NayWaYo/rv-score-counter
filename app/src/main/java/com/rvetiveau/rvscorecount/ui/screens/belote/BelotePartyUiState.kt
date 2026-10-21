package com.rvetiveau.rvscorecount.ui.screens.belote

import com.rvetiveau.rvscorecount.domain.model.BeloteOutcome
import com.rvetiveau.rvscorecount.domain.model.TeamSide

sealed interface BelotePartyUiState {
    data object Loading : BelotePartyUiState
    data object LoadError : BelotePartyUiState
    data object NotFound : BelotePartyUiState

    data class Loaded(
        val teams: List<BeloteTeamUiModel>,
        val progress: BeloteProgress,
        val dealerName: String,
        val pendingReserve: Int,
        val rounds: List<BeloteRoundRowUiModel>,
        val nextRoundNumber: Int
    ) : BelotePartyUiState
}

/** An Équipe with its players and cumulated score; [isLeader] when it is strictly ahead. */
data class BeloteTeamUiModel(
    val side: TeamSide,
    val name: String,
    val playerNames: List<String>,
    val total: Int,
    val isLeader: Boolean
)

sealed interface BeloteProgress {
    /** [leaderNames]: the Équipe(s) closest to the target, [remaining] points from it. */
    data class Target(val target: Int, val remaining: Int, val leaderNames: List<String>) : BeloteProgress

    data class Rounds(val current: Int, val total: Int) : BeloteProgress

    /** End reached with equal totals: one more Manche. */
    data object ExtraRound : BeloteProgress
}

data class BeloteRoundRowUiModel(
    val roundNumber: Int,
    val takerName: String,
    val outcome: BeloteOutcome,
    val firstPoints: Int,
    val secondPoints: Int
)
