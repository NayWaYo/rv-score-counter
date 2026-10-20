package com.rvetiveau.rvscorecount.domain.model

/** One Participant's score in the Manche [roundNumber]. */
data class RoundRecordEntry(
    val participant: Participant,
    val roundNumber: Int
)

/** Highest score taken in a single Manche of a Partie, and everyone who took it. */
data class RoundRecord(
    val entries: List<RoundRecordEntry>,
    val points: Int
)

/**
 * « La fusée » : the highest score a Participant took in a single Manche, ties
 * included, by Manche then table order; null before any Manche. A score outside [participants]
 * is an aggregate inconsistency, never ignored.
 */
fun roundRecord(participants: List<Participant>, scoresByRound: Map<Round, List<RoundScore>>): RoundRecord? {
    val participantsById = participants.associateBy { it.id }
    val scored = scoresByRound.flatMap { (round, scores) ->
        scores.map { score ->
            val participant = requireNotNull(participantsById[score.participantId]) {
                "Every round score must belong to one of the party's participants"
            }
            Triple(round.roundNumber, participant, score.points)
        }
    }
    val best = scored.maxOfOrNull { it.third } ?: return null
    return RoundRecord(
        entries = scored.filter { it.third == best }
            .sortedWith(compareBy({ it.first }, { it.second.listPosition }))
            .map { (roundNumber, participant, _) -> RoundRecordEntry(participant, roundNumber) },
        points = best
    )
}
