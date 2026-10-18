package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/**
 * Historique entry of a FINISHED Party. [winners] holds every Participant tied at the lowest Score
 * Cumulé (empty when no Manche was played); [isConsistent] is false when the stored aggregate
 * cannot produce a trustworthy ranking, in which case no winner is given.
 */
data class PartySummary(
    val partyId: UUID,
    val gameId: String,
    val createdAt: Long,
    val roundCount: Int,
    val winners: List<String>,
    val winningScore: Int?,
    val isConsistent: Boolean = true
)
