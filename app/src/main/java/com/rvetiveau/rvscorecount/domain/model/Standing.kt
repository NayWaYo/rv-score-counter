package com.rvetiveau.rvscorecount.domain.model

data class Standing(
    val participant: Participant,
    val cumulativeScore: Int
)

/**
 * Classement par Score Cumulé croissant, égalités départagées par listPosition. Un score dont le
 * Participant n'appartient pas à [participants] est une incohérence d'agrégat, jamais ignorée.
 */
fun computeStandings(participants: List<Participant>, scores: List<RoundScore>): List<Standing> {
    val participantIds = participants.mapTo(mutableSetOf()) { it.id }
    require(scores.all { it.participantId in participantIds }) {
        "Every round score must belong to one of the party's participants"
    }
    val totals = scores.groupBy { it.participantId }.mapValues { (_, list) -> list.sumOf { it.points } }
    return participants
        .map { Standing(it, totals[it.id] ?: 0) }
        .sortedWith(compareBy<Standing> { it.cumulativeScore }.thenBy { it.participant.listPosition })
}
