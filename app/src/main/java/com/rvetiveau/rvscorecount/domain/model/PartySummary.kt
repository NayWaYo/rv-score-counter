package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/**
 * Historique entry of a FINISHED Party. [winners] holds every Participant tied at the lowest Score
 * Cumulé (empty when no Manche was played); [isConsistent] is false when the stored aggregate
 * cannot produce a trustworthy ranking, in which case no winner is given. For a team game,
 * [teamScores] lists both Équipes and [winners] the winning Équipe, at the highest score.
 */
data class PartySummary(
    val partyId: UUID,
    val gameId: String,
    val createdAt: Long,
    val roundCount: Int,
    val winners: List<String>,
    val winningScore: Int?,
    val isConsistent: Boolean = true,
    val teamScores: List<TeamScore> = emptyList()
)

/** Final score of an Équipe, for a team game. */
data class TeamScore(val name: String, val points: Int)
