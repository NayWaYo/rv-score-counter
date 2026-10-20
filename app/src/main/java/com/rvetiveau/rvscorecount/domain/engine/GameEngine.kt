package com.rvetiveau.rvscorecount.domain.engine

import com.rvetiveau.rvscorecount.domain.model.ParticipantId

/**
 * Contrat commun à tous les jeux : règles pures, sans persistance ni état.
 * Les appelants garantissent que les scores sont bornés aux valeurs métier du jeu.
 */
interface GameEngine {
    val gameId: String
    val minPlayers: Int
    val maxPlayers: Int
    val totalPointsPerRound: Int

    /**
     * Points of the 7 of the die's suit when the game records who took it each round (Papayoo : 40),
     * null otherwise. Its holder's round score is at least this value.
     */
    val suitSevenPoints: Int? get() = null

    /** Dealing reminder for [playerCount] players, null when the game has none or the count is out of range. */
    fun dealingRules(playerCount: Int): DealingRules? = null

    fun precomputeLastScore(entered: Map<ParticipantId, Int>): Int

    fun validateRound(scores: Map<ParticipantId, Int>): RoundValidation
}
