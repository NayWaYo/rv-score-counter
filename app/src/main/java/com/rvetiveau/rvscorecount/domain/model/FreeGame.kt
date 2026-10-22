package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/** Who wins a Partie of Jeu libre (AD-9), chosen at its creation. Persisted by name. */
enum class WinDirection {
    /** Skyjo, Uno: the smallest total wins. */
    LOWEST_WINS,

    /** Yams, Rami: the largest total wins. */
    HIGHEST_WINS
}

/** A validated Manche of Jeu libre: one score per Participant, any integer, negative included. */
data class FreeRound(
    val id: UUID,
    val partyId: UUID,
    val roundNumber: Int,
    val scores: Map<UUID, Int>
)

/** A Participant's total so far and their rank, ties sharing a rank (1 is the best). */
data class FreeStanding(
    val participant: Participant,
    val total: Int,
    val rank: Int
)

/** A game name already played in Jeu libre, with the creation time of its latest Partie (Story 11.2). */
data class PlayedGameName(val name: String, val lastPlayedAt: Long)

/**
 * Game names to suggest at creation, the most recently played first, once each: names that differ
 * only by case or surrounding spaces are the same game, shown with the spelling of its latest Partie.
 */
fun List<PlayedGameName>.distinctGameNames(): List<String> =
    filter { it.name.isNotBlank() }
        .sortedByDescending { it.lastPlayedAt }
        .distinctBy { playerNameKey(it.name) }
        .map { it.name.trim() }

sealed interface FreePartyStatus {
    /** The end rule is not reached, or there is none: the Partie ends by hand. */
    data object InProgress : FreePartyStatus

    /** The end rule is reached: [winners] are every Participant at the best total. */
    data class Finished(val winners: List<Participant>) : FreePartyStatus
}
