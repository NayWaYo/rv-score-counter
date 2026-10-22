package com.rvetiveau.rvscorecount.game.free

import com.rvetiveau.rvscorecount.domain.engine.FreeGameRules
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.FreePartyStatus
import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.FreeStanding
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import java.util.UUID
import javax.inject.Inject

class FreeGameEngine @Inject constructor() : FreeGameRules {
    override val gameId: String = GAME_ID
    override val minPlayers: Int = 2
    override val maxPlayers: Int = 10
    override val roundScores: IntRange = -MAX_ROUND_SCORE..MAX_ROUND_SCORE

    override fun isValidRound(scores: Map<UUID, Int>, participants: List<Participant>): Boolean =
        scores.keys == participants.mapTo(mutableSetOf()) { it.id } && scores.values.all { it in roundScores }

    override fun standings(participants: List<Participant>, rounds: List<FreeRound>, direction: WinDirection): List<FreeStanding> {
        val totals = totals(participants, rounds)
        val ordered = participants.sortedWith(
            compareBy<Participant> { direction.sortKey(totals.getValue(it.id)) }.thenBy { it.listPosition }
        )
        return ordered.map { participant ->
            val total = totals.getValue(participant.id)
            // Ties share the rank of the first of them: one plus everyone strictly better.
            val rank = 1 + ordered.count { direction.sortKey(totals.getValue(it.id)) < direction.sortKey(total) }
            FreeStanding(participant, total, rank)
        }
    }

    override fun partyStatus(
        participants: List<Participant>,
        rounds: List<FreeRound>,
        direction: WinDirection,
        endRule: EndRule?
    ): FreePartyStatus {
        val reached = when (endRule) {
            null -> false
            is EndRule.TargetScore -> totals(participants, rounds).values.any { it >= endRule.points }
            is EndRule.RoundCount -> rounds.size >= endRule.rounds
        }
        return if (reached) FreePartyStatus.Finished(winners(participants, rounds, direction)) else FreePartyStatus.InProgress
    }

    override fun winners(participants: List<Participant>, rounds: List<FreeRound>, direction: WinDirection): List<Participant> =
        standings(participants, rounds, direction).filter { it.rank == 1 }.map { it.participant }

    /** Every Participant's total, 0 before any Manche; a score outside the Partie throws. */
    private fun totals(participants: List<Participant>, rounds: List<FreeRound>): Map<UUID, Int> {
        val totals = participants.associate { it.id to 0L }.toMutableMap()
        rounds.forEach { round ->
            round.scores.forEach { (participantId, points) ->
                val total = requireNotNull(totals[participantId]) {
                    "Round ${round.roundNumber} scores $participantId, who is not in the Partie"
                }
                totals[participantId] = total + points
            }
        }
        return totals.mapValues { (_, total) -> Math.toIntExact(total) }
    }

    /** Smaller is better once turned by the direction. */
    private fun WinDirection.sortKey(total: Int): Long = when (this) {
        WinDirection.LOWEST_WINS -> total.toLong()
        WinDirection.HIGHEST_WINS -> -total.toLong()
    }

    companion object {
        const val GAME_ID = GameIds.FREE
        const val MAX_ROUND_SCORE = 99_999
    }
}
