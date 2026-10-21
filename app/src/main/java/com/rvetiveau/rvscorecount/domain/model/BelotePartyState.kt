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

    /**
     * « 🗿 Chad de la game » (Story 8.3): every player with how many times they took and how many of
     * those contracts they made (made, or capot by the taker), most takes first, then most made, then
     * table order. Manches without a recorded taker do not count; empty when none has one.
     */
    fun takerRanking(): List<TakerStat> {
        val counted = rounds.zip(results).filter { (round, _) -> round.takerPlayerId != null }
        if (counted.isEmpty()) return emptyList()
        return players.map { player ->
            val taken = counted.filter { (round, _) -> round.takerPlayerId == player.id }
            TakerStat(
                player = player,
                takes = taken.size,
                made = taken.count { (round, result) ->
                    result.outcome == BeloteOutcome.MADE ||
                        (result.outcome == BeloteOutcome.CAPOT && round.input.capotTeam == round.input.taker)
                }
            )
        }.sortedWith(compareByDescending<TakerStat> { it.takes }.thenByDescending { it.made })
    }
}

data class TakerStat(val player: Participant, val takes: Int, val made: Int)
