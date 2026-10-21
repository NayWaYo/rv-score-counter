package com.rvetiveau.rvscorecount.game.belote

import com.rvetiveau.rvscorecount.domain.engine.BeloteRules
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.BeloteOutcome
import com.rvetiveau.rvscorecount.domain.model.BelotePartyStatus
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundResult
import com.rvetiveau.rvscorecount.domain.model.TeamPoints
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import javax.inject.Inject

class BeloteEngine @Inject constructor() : BeloteRules {
    override val gameId: String = GAME_ID
    override val totalTrickPoints: Int = TOTAL_TRICK_POINTS

    override fun otherTeamTrickPoints(trickPoints: Int): Int {
        requireTrickPoints(trickPoints)
        return TOTAL_TRICK_POINTS - trickPoints
    }

    override fun scoreRound(input: BeloteRoundInput, pendingReserve: Int): BeloteRoundResult {
        requireTrickPoints(input.takerTrickPoints)
        require(pendingReserve >= 0) { "The litige reserve cannot be negative" }
        val taker = input.taker
        val defense = taker.opponent
        if (input.capotTeam == taker) {
            require(input.takerTrickPoints == TOTAL_TRICK_POINTS) { "A capot taker has all the trick points" }
        }
        if (input.capotTeam == defense) {
            require(input.takerTrickPoints == 0) { "A taker capot by the defense has no trick points" }
        }

        fun belote(side: TeamSide) = if (input.beloteTeam == side) BELOTE_POINTS else 0
        val takerTricks = input.takerTrickPoints
        val takerTotal = takerTricks + belote(taker)
        val defenseTotal = TOTAL_TRICK_POINTS - takerTricks + belote(defense)

        val outcome = when {
            input.capotTeam != null -> BeloteOutcome.CAPOT
            takerTotal > defenseTotal -> BeloteOutcome.MADE
            takerTotal < defenseTotal -> BeloteOutcome.DOWN
            else -> BeloteOutcome.DISPUTED
        }
        val points = when (outcome) {
            BeloteOutcome.MADE -> TeamPoints.of(taker, takerTotal, defenseTotal)
            BeloteOutcome.DOWN -> TeamPoints.of(taker, belote(taker), TOTAL_TRICK_POINTS + belote(defense))
            BeloteOutcome.DISPUTED -> TeamPoints.of(taker, belote(taker), defenseTotal)
            BeloteOutcome.CAPOT -> {
                val capotTeam = checkNotNull(input.capotTeam)
                TeamPoints.of(capotTeam, CAPOT_POINTS + belote(capotTeam), belote(capotTeam.opponent))
            }
        }
        val winner = when (outcome) {
            BeloteOutcome.MADE -> taker
            BeloteOutcome.DOWN -> defense
            BeloteOutcome.CAPOT -> input.capotTeam
            BeloteOutcome.DISPUTED -> null
        }
        return if (winner == null) {
            BeloteRoundResult(outcome, points, reserveWon = 0, reserveAfter = pendingReserve + takerTricks)
        } else {
            BeloteRoundResult(
                outcome = outcome,
                points = points + TeamPoints.of(winner, pendingReserve, 0),
                reserveWon = pendingReserve,
                reserveAfter = 0
            )
        }
    }

    override fun scoreParty(rounds: List<BeloteRoundInput>): List<BeloteRoundResult> {
        var reserve = 0
        return rounds.map { round ->
            scoreRound(round, reserve).also { reserve = it.reserveAfter }
        }
    }

    override fun partyStatus(results: List<BeloteRoundResult>, endRule: BeloteEndRule): BelotePartyStatus {
        val totals = results.fold(TeamPoints.ZERO) { sum, result -> sum + result.points }
        val endReached = when (endRule) {
            is BeloteEndRule.TargetScore -> maxOf(totals.first, totals.second) >= endRule.points
            is BeloteEndRule.RoundCount -> results.size >= endRule.rounds
        }
        return when {
            !endReached || totals.first == totals.second -> BelotePartyStatus.InProgress
            totals.first > totals.second -> BelotePartyStatus.Won(TeamSide.FIRST)
            else -> BelotePartyStatus.Won(TeamSide.SECOND)
        }
    }

    private fun requireTrickPoints(trickPoints: Int) {
        require(trickPoints in 0..TOTAL_TRICK_POINTS) { "Trick points must be within 0..$TOTAL_TRICK_POINTS" }
    }

    companion object {
        const val GAME_ID = GameIds.BELOTE
        const val TOTAL_TRICK_POINTS = 162
        const val BELOTE_POINTS = 20
        const val CAPOT_POINTS = 252
    }
}
