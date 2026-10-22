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

sealed interface FreePartyStatus {
    /** The end rule is not reached, or there is none: the Partie ends by hand. */
    data object InProgress : FreePartyStatus

    /** The end rule is reached: [winners] are every Participant at the best total. */
    data class Finished(val winners: List<Participant>) : FreePartyStatus
}
