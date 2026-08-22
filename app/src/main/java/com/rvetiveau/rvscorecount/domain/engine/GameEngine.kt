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

    fun precomputeLastScore(entered: Map<ParticipantId, Int>): Int

    fun validateRound(scores: Map<ParticipantId, Int>): RoundValidation
}
