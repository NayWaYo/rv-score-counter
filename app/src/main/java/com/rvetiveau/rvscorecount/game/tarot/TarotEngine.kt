package com.rvetiveau.rvscorecount.game.tarot

import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.engine.TarotRules
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.TarotDealing
import com.rvetiveau.rvscorecount.domain.model.TarotInputError
import com.rvetiveau.rvscorecount.domain.model.TarotPartyStatus
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import com.rvetiveau.rvscorecount.domain.model.TarotRoundResult
import com.rvetiveau.rvscorecount.domain.model.TarotSide
import com.rvetiveau.rvscorecount.domain.model.TarotStanding
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/** FFT scoring (`games/tarot.md`): the attack wins S against each defender, the partner taking one share. */
class TarotEngine @Inject constructor() : TarotRules {
    override val gameId: String = GAME_ID
    override val minPlayers: Int = 3
    override val maxPlayers: Int = 5
    override val partnerPlayerCount: Int = 5
    override val totalPoints: Int = 91
    override val maxBouts: Int = 3

    override fun dealing(playerCount: Int): TarotDealing = when (playerCount) {
        3 -> TarotDealing(cardsPerPlayer = 24, dogSize = 6)
        4 -> TarotDealing(cardsPerPlayer = 18, dogSize = 6)
        5 -> TarotDealing(cardsPerPlayer = 15, dogSize = 3)
        else -> throw IllegalArgumentException("The Tarot is played by $minPlayers to $maxPlayers players, not $playerCount")
    }

    override fun requiredPoints(bouts: Int): Int {
        require(bouts in 0..maxBouts) { "A side has 0 to $maxBouts bouts, not $bouts" }
        return REQUIRED_POINTS[bouts]
    }

    override fun inputError(input: TarotRoundInput, participants: List<Participant>): TarotInputError? {
        val ids = participants.map { it.id }.toSet()
        return when {
            participants.size !in minPlayers..maxPlayers -> TarotInputError.PLAYER_COUNT
            input.takerId !in ids -> TarotInputError.UNKNOWN_TAKER
            input.attackBouts !in 0..maxBouts -> TarotInputError.BOUTS_OUT_OF_RANGE
            input.attackPoints !in 0..totalPoints -> TarotInputError.POINTS_OUT_OF_RANGE
            input.partnerId == null -> null
            participants.size != partnerPlayerCount -> TarotInputError.PARTNER_NOT_ALLOWED
            input.partnerId == input.takerId -> TarotInputError.PARTNER_IS_TAKER
            input.partnerId !in ids -> TarotInputError.UNKNOWN_PARTNER
            else -> null
        }
    }

    override fun scoreRound(input: TarotRoundInput, participants: List<Participant>): TarotRoundResult {
        inputError(input, participants)?.let { error -> throw IllegalArgumentException("Invalid Tarot Manche: $error") }

        val required = requiredPoints(input.attackBouts)
        val made = input.attackPoints >= required
        val multiplier = input.contract.multiplier
        val towardWinner = if (made) 1 else -1

        val contract = (25 + abs(input.attackPoints - required)) * multiplier * towardWinner
        val petitAuBout = when (input.petitAuBout) {
            TarotSide.ATTACK -> 10 * multiplier
            TarotSide.DEFENSE -> -10 * multiplier
            null -> 0
        }
        val poignees = ((input.attackPoignee?.points ?: 0) + (input.defensePoignee?.points ?: 0)) * towardWinner
        val perDefender = contract + petitAuBout + poignees + (input.chelem?.points ?: 0)

        val defenders = participants.filter { it.id != input.takerId && it.id != input.partnerId }
        val attackShare = perDefender * defenders.size
        val points = participants.associate { participant ->
            participant.id to when (participant.id) {
                input.takerId -> if (input.partnerId == null) attackShare else attackShare - perDefender
                input.partnerId -> perDefender
                else -> -perDefender
            }
        }
        return TarotRoundResult(required, made, perDefender, points)
    }

    override fun standings(participants: List<Participant>, rounds: List<TarotRound>): List<TarotStanding> {
        val totals = totals(participants, rounds)
        val ordered = participants.sortedWith(compareByDescending<Participant> { totals.getValue(it.id) }.thenBy { it.listPosition })
        return ordered.map { participant ->
            val total = totals.getValue(participant.id)
            // Ties share the rank of the first of them: one plus everyone strictly ahead.
            TarotStanding(participant, total, rank = 1 + ordered.count { totals.getValue(it.id) > total })
        }
    }

    override fun partyStatus(participants: List<Participant>, rounds: List<TarotRound>, endRule: EndRule?): TarotPartyStatus {
        val reached = when (endRule) {
            null -> false
            is EndRule.RoundCount -> rounds.size >= endRule.rounds
            is EndRule.TargetScore -> throw IllegalArgumentException("A Partie of Tarot has no target score")
        }
        if (!reached) return TarotPartyStatus.InProgress
        return TarotPartyStatus.Finished(standings(participants, rounds).filter { it.rank == 1 }.map { it.participant })
    }

    /** Every Participant's total, 0 before any Manche. */
    private fun totals(participants: List<Participant>, rounds: List<TarotRound>): Map<UUID, Int> {
        val totals = participants.associate { it.id to 0L }.toMutableMap()
        rounds.forEach { round ->
            scoreRound(round.input, participants).points.forEach { (participantId, points) ->
                totals[participantId] = totals.getValue(participantId) + points
            }
        }
        return totals.mapValues { (_, total) -> Math.toIntExact(total) }
    }

    companion object {
        const val GAME_ID = GameIds.TAROT

        /** Indexed by the attack's bouts. */
        private val REQUIRED_POINTS = listOf(56, 51, 41, 36)
    }
}
