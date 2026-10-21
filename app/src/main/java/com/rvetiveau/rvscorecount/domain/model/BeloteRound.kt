package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/**
 * What is entered for one Manche of Belote: the [taker]'s trick points, dix de der included (the
 * defense has the rest of the deck's points), the Équipe that announced the belote-rebelote and the
 * one that took every trick, if any.
 */
data class BeloteRoundInput(
    val taker: TeamSide,
    val takerTrickPoints: Int,
    val beloteTeam: TeamSide? = null,
    val capotTeam: TeamSide? = null
)

enum class BeloteOutcome {
    /** The taker's total, belote included, beats the defense's. */
    MADE,

    /** The taker's total is below the defense's: « dedans ». */
    DOWN,

    /** Equal totals: « litige », the taker's tricks go to the reserve. */
    DISPUTED,

    /** One Équipe took every trick. */
    CAPOT
}

/**
 * Score of one Manche. [points] includes the reserve won by the Manche's winner, [reserveWon]
 * (0 after a litige or when none was pending); [reserveAfter] is what the next Manche can win.
 */
data class BeloteRoundResult(
    val outcome: BeloteOutcome,
    val points: TeamPoints,
    val reserveWon: Int,
    val reserveAfter: Int
)

/**
 * A validated Manche of Belote: what was entered, its points being recomputed by the engine.
 * [takerPlayerId]: the player of the taking Équipe who took (Story 8.3); null for Manches played before.
 */
data class BeloteRound(
    val id: UUID,
    val partyId: UUID,
    val roundNumber: Int,
    val input: BeloteRoundInput,
    val takerPlayerId: UUID? = null
)
