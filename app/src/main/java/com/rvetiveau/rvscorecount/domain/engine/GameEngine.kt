package com.rvetiveau.rvscorecount.domain.engine

import com.rvetiveau.rvscorecount.domain.model.ParticipantId

/**
 * Contrat commun à tous les jeux : règles pures, sans persistance ni état.
 * Les appelants garantissent que les scores sont bornés aux valeurs métier du jeu.
 */
interface GameEngine : Game {
    override val gameId: String
    val minPlayers: Int
    val maxPlayers: Int
    val totalPointsPerRound: Int

    /**
     * Points of the 7 of the die's suit when the game records who took it each round (Papayoo : 40),
     * null otherwise. Its holder's round score is at least this value.
     */
    val suitSevenPoints: Int? get() = null

    /**
     * Numbers of the cards worth their own number (Papayoo : the Payoo 1 to 20), which a photo of a
     * player's cards can count (Story 9.1); null when the game has none.
     */
    val faceValueCards: IntRange? get() = null

    /** Round score of a player who took [cards] among [faceValueCards] and, when [tookSuitSeven], the 7. */
    fun scoreOfCards(cards: Set<Int>, tookSuitSeven: Boolean): Int {
        val range = requireNotNull(faceValueCards) { "$gameId has no face value cards" }
        require(cards.all { it in range }) { "Cards $cards are not all within $range" }
        return cards.sum() + if (tookSuitSeven) suitSevenPoints ?: 0 else 0
    }

    /** Dealing reminder for [playerCount] players, null when the game has none or the count is out of range. */
    fun dealingRules(playerCount: Int): DealingRules? = null

    fun precomputeLastScore(entered: Map<ParticipantId, Int>): Int

    fun validateRound(scores: Map<ParticipantId, Int>): RoundValidation
}
