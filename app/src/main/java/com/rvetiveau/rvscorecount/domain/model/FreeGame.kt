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

/**
 * A Partie of Jeu libre as played (Story 11.3): its Participants in table order, its Manches in
 * order and, recomputed by the engine, the [standings] and the [status] they lead to. A Partie
 * ended by hand is FINISHED while its [status] may still be in progress.
 */
data class FreePartyState(
    val party: Party,
    val participants: List<Participant>,
    val rounds: List<FreeRound>,
    val standings: List<FreeStanding>,
    val status: FreePartyStatus
) {
    val gameName: String get() = checkNotNull(party.gameName) { "A Partie of Jeu libre has a game name" }

    val winDirection: WinDirection get() = checkNotNull(party.winDirection) { "A Partie of Jeu libre has a win direction" }

    val dealer: Participant get() = participants[party.currentDealerIndex]

    /** Every Participant at the best total: the winners once the Partie is ended, by its rule or by hand. */
    val leaders: List<Participant> get() = standings.filter { it.rank == 1 }.map { it.participant }
}
