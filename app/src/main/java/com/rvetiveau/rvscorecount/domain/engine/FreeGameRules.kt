package com.rvetiveau.rvscorecount.domain.engine

import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.FreePartyStatus
import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.FreeStanding
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import java.util.UUID

/**
 * Counting of any game without its own engine (AD-9, beside [GameEngine]): pure and stateless. Its
 * rules come with each Partie: who wins ([WinDirection]) and how it ends ([EndRule], null by hand).
 */
interface FreeGameRules : Game {
    override val gameId: String
    val minPlayers: Int
    val maxPlayers: Int

    /** The bounds of one score in one Manche, negative included. */
    val roundScores: IntRange

    /** A Manche is valid when every one of [participants] has exactly one score within [roundScores]. */
    fun isValidRound(scores: Map<UUID, Int>, participants: List<Participant>): Boolean

    /**
     * Totals after [rounds], best first according to [direction]; ties share a rank, then follow the
     * table order. A score of a Participant outside [participants] is an inconsistency: it throws.
     */
    fun standings(participants: List<Participant>, rounds: List<FreeRound>, direction: WinDirection): List<FreeStanding>

    /**
     * Whether [rounds] reach [endRule]: a total at or beyond the target score, or the last Manche of
     * the count. Without a rule the Partie ends only by hand.
     */
    fun partyStatus(
        participants: List<Participant>,
        rounds: List<FreeRound>,
        direction: WinDirection,
        endRule: EndRule?
    ): FreePartyStatus

    /** Every Participant at the best total, whenever the Partie is ended (by its rule or by hand). */
    fun winners(participants: List<Participant>, rounds: List<FreeRound>, direction: WinDirection): List<Participant>
}
