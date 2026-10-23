package com.rvetiveau.rvscorecount.domain.engine

import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.TarotInputError
import com.rvetiveau.rvscorecount.domain.model.TarotPartyStatus
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.model.TarotRoundInput
import com.rvetiveau.rvscorecount.domain.model.TarotRoundResult
import com.rvetiveau.rvscorecount.domain.model.TarotStanding

/**
 * Scoring of Tarot by player (AD-10, beside [GameEngine]): pure and stateless.
 * Rules: `planning-artifacts/games/tarot.md`.
 */
interface TarotRules : Game {
    override val gameId: String
    val minPlayers: Int
    val maxPlayers: Int

    /** The players who may name a partner (the called king). */
    val partnerPlayerCount: Int

    /** Trick points of the whole deck. */
    val totalPoints: Int
    val maxBouts: Int

    /** Points the attack needs with [bouts] bouts. */
    fun requiredPoints(bouts: Int): Int

    /** Why [input] cannot be scored for [participants], or null when it can. */
    fun inputError(input: TarotRoundInput, participants: List<Participant>): TarotInputError?

    /** Scores one Manche; [input] must have no [inputError]. */
    fun scoreRound(input: TarotRoundInput, participants: List<Participant>): TarotRoundResult

    /** Totals after [rounds], highest first; ties share a rank, then follow the table order. */
    fun standings(participants: List<Participant>, rounds: List<TarotRound>): List<TarotStanding>

    /**
     * Finished once [endRule], a [EndRule.RoundCount], is reached; without a rule the Partie ends only
     * by hand. A [EndRule.TargetScore] is not a Tarot rule: it throws.
     */
    fun partyStatus(participants: List<Participant>, rounds: List<TarotRound>, endRule: EndRule?): TarotPartyStatus
}
