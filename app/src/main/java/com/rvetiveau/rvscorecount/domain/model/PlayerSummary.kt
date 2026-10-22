package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/**
 * A Joueur on the « Joueurs » screen (Story 10.2): their finished Parties, every Jeu together, and
 * the creation time of their latest Partie, finished or not; null once none is left.
 */
data class PlayerSummary(
    val id: UUID,
    val name: String,
    val finishedParties: Int,
    val lastPlayedAt: Long?
)

/** The order of the suggestions (Story 10.1): most recently played first, then those without any Partie by name. */
fun List<PlayerSummary>.inPlayOrder(): List<PlayerSummary> = sortedWith(lastPlayedFirst({ it.lastPlayedAt }, { it.name }))

internal fun <T> lastPlayedFirst(lastPlayedAt: (T) -> Long?, name: (T) -> String): Comparator<T> =
    compareBy(nullsLast(reverseOrder()), lastPlayedAt).thenBy { playerNameKey(name(it)) }
