package com.rvetiveau.rvscorecount.domain.model

/**
 * A Partie of Belote as played: its two Équipes, its four players in table order, its Manches and
 * their [results] recomputed by the engine, in order, with the [status] they lead to.
 */
data class BelotePartyState(
    val party: Party,
    val teams: List<Team>,
    val players: List<Participant>,
    val rounds: List<BeloteRound>,
    val results: List<BeloteRoundResult>,
    val status: BelotePartyStatus
) {
    val endRule: BeloteEndRule get() = checkNotNull(party.endRule) { "A Belote party has an end rule" }

    val totals: TeamPoints get() = results.fold(TeamPoints.ZERO) { sum, result -> sum + result.points }

    /** Litige reserve the next Manche can win. */
    val pendingReserve: Int get() = results.lastOrNull()?.reserveAfter ?: 0

    val dealer: Participant get() = players[party.currentDealerIndex]

    fun team(side: TeamSide): Team = teams.first { it.side == side }

    fun playersOf(side: TeamSide): List<Participant> = players.filter { it.teamSide == side }
}
