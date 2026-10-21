package com.rvetiveau.rvscorecount.domain.engine

import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundResult

/**
 * Scoring of classic Belote, by Équipe (AD-8, beside [GameEngine]): pure and stateless.
 * Rules: `planning-artifacts/games/belote.md`.
 */
interface BeloteRules {
    val gameId: String

    /** Points of all the tricks of a Manche, dix de der included. */
    val totalTrickPoints: Int

    /** Trick points of the other Équipe when one Équipe has [trickPoints]. */
    fun otherTeamTrickPoints(trickPoints: Int): Int

    /** Scores one Manche, the winner taking the litige reserve [pendingReserve] left by the previous ones. */
    fun scoreRound(input: BeloteRoundInput, pendingReserve: Int = 0): BeloteRoundResult

    /** Scores the Manches of a Partie in order, carrying the litige reserve from one to the next. */
    fun scoreParty(rounds: List<BeloteRoundInput>): List<BeloteRoundResult>

    fun partyStatus(results: List<BeloteRoundResult>, endRule: BeloteEndRule): BelotePartyStatus
}
