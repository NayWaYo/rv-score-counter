package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/** Points a Participant took in the Manche [roundNumber]. */
data class RoundPoints(
    val points: Int,
    val roundNumber: Int
)

/**
 * One Participant's figures over the Manches they played: [averagePoints] per Manche, Manches
 * finished with 0 point, the best (fewest points) and worst (most points) Manche, the earliest
 * one on a tie. [sevensTaken] is null when no Manche of the Partie records who took the 7.
 */
data class PlayerStats(
    val roundsPlayed: Int,
    val averagePoints: Double,
    val zeroRounds: Int,
    val sevensTaken: Int?,
    val best: RoundPoints,
    val worst: RoundPoints
)

/**
 * Statistics of every Participant who played at least one Manche, keyed by participant id. A
 * score or 7 holder outside [participants] is an aggregate inconsistency, never ignored.
 */
fun playerStats(participants: List<Participant>, scoresByRound: Map<Round, List<RoundScore>>): Map<UUID, PlayerStats> {
    val participantIds = participants.mapTo(mutableSetOf()) { it.id }
    val rounds = scoresByRound.keys.sortedBy { it.roundNumber }
    require(rounds.all { it.sevenHolderId == null || it.sevenHolderId in participantIds }) {
        "Every 7 holder must be one of the party's participants"
    }
    val tracksSevens = rounds.any { it.sevenHolderId != null }
    val played = rounds.flatMap { round ->
        scoresByRound.getValue(round).map { score ->
            require(score.participantId in participantIds) {
                "Every round score must belong to one of the party's participants"
            }
            score.participantId to RoundPoints(score.points, round.roundNumber)
        }
    }.groupBy({ it.first }, { it.second })

    return played.mapValues { (participantId, results) ->
        PlayerStats(
            roundsPlayed = results.size,
            averagePoints = results.map { it.points }.average(),
            zeroRounds = results.count { it.points == 0 },
            sevensTaken = if (tracksSevens) rounds.count { it.sevenHolderId == participantId } else null,
            best = results.minBy { it.points },
            worst = results.maxBy { it.points }
        )
    }
}
