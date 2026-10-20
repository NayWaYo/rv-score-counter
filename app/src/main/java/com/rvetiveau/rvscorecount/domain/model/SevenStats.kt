package com.rvetiveau.rvscorecount.domain.model

/** Participants who took the suit seven the most often in a Partie, and how many times. */
data class SevenChampion(
    val participants: List<Participant>,
    val count: Int
)

/**
 * « Poissard ou Nul ? » : the Participant(s) who took the 7 of the die's suit the most often, ties
 * included, in table order. Rounds without a recorded holder are ignored; null when none has one.
 * A holder outside [participants] is an aggregate inconsistency, never ignored.
 */
fun sevenChampion(participants: List<Participant>, rounds: List<Round>): SevenChampion? {
    val participantsById = participants.associateBy { it.id }
    val counts = rounds.mapNotNull { it.sevenHolderId }
        .onEach { require(it in participantsById) { "Every 7 holder must be one of the party's participants" } }
        .groupingBy { it }
        .eachCount()
    val best = counts.values.maxOrNull() ?: return null
    return SevenChampion(
        participants = counts.filterValues { it == best }.keys
            .map { participantsById.getValue(it) }
            .sortedBy { it.listPosition },
        count = best
    )
}
