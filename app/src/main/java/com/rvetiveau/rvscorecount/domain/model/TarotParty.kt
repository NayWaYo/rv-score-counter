package com.rvetiveau.rvscorecount.domain.model

/**
 * A Partie of Tarot as played (Story 13.3): its Participants in table order, its Manches in order
 * with their [results], and, recomputed by the engine, the [standings] and the [status] they lead
 * to. A Partie ended by hand is FINISHED while its [status] may still be in progress.
 */
data class TarotPartyState(
    val party: Party,
    val participants: List<Participant>,
    val rounds: List<TarotRound>,
    val results: List<TarotRoundResult>,
    val standings: List<TarotStanding>,
    val status: TarotPartyStatus,
    val dealing: TarotDealing,
    /** The table is large enough for the taker to call a king: their partner, or nobody. */
    val callsPartner: Boolean
) {
    val dealer: Participant get() = participants[party.currentDealerIndex]

    /** Every Participant at the best total: the winners once the Partie is ended, by its rule or by hand. */
    val leaders: List<Participant> get() = standings.filter { it.rank == 1 }.map { it.participant }
}
