package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/** A Tarot contract and the [multiplier] it applies to the contract and the petit au bout. Persisted by name. */
enum class TarotContract(val multiplier: Int) {
    PRISE(1),
    GARDE(2),
    GARDE_SANS(4),
    GARDE_CONTRE(6)
}

/** The taker and their partner, or the other players. Persisted by name. */
enum class TarotSide { ATTACK, DEFENSE }

/** A handful of trumps shown, worth [points] to the side that wins the contract. Persisted by name. */
enum class Poignee(val points: Int) {
    SIMPLE(20),
    DOUBLE(30),
    TRIPLE(40)
}

/** Every trick taken by one side, worth [points] to the attack. Persisted by name. */
enum class Chelem(val points: Int) {
    ANNOUNCED_MADE(400),
    UNANNOUNCED_MADE(200),
    ANNOUNCED_FAILED(-200),

    /** The defense took every trick. */
    DEFENSE_MADE(-200)
}

/**
 * What is entered for one Manche of Tarot: the taker's [contract], their [partnerId] (the holder of
 * the called king at 5 players; null when alone, or below 5 players), the attack's bouts and trick
 * points (the defense has the rest of the 91), and the bonuses. Ids are Participant ids.
 */
data class TarotRoundInput(
    val takerId: UUID,
    val contract: TarotContract,
    val attackBouts: Int,
    val attackPoints: Int,
    val partnerId: UUID? = null,
    val petitAuBout: TarotSide? = null,
    val attackPoignee: Poignee? = null,
    val defensePoignee: Poignee? = null,
    val chelem: Chelem? = null
)

/** Why a [TarotRoundInput] cannot be scored. */
enum class TarotInputError {
    PLAYER_COUNT,
    UNKNOWN_TAKER,
    BOUTS_OUT_OF_RANGE,
    POINTS_OUT_OF_RANGE,

    /** A partner below 5 players. */
    PARTNER_NOT_ALLOWED,
    PARTNER_IS_TAKER,
    UNKNOWN_PARTNER
}

/**
 * Score of one Manche: the [requiredPoints] the bouts set, whether the contract is [made], what the
 * attack wins against each defender ([perDefender], negative when it loses) and every Participant's
 * [points], which add up to zero.
 */
data class TarotRoundResult(
    val requiredPoints: Int,
    val made: Boolean,
    val perDefender: Int,
    val points: Map<UUID, Int>
)

/** A validated Manche of Tarot: what was entered, its points being recomputed by the engine. */
data class TarotRound(
    val id: UUID,
    val partyId: UUID,
    val roundNumber: Int,
    val input: TarotRoundInput
)

/** A Participant's total so far and their rank, ties sharing a rank (1 is the best). */
data class TarotStanding(
    val participant: Participant,
    val total: Int,
    val rank: Int
)

sealed interface TarotPartyStatus {
    /** The last Manche planned is not played, or there is no limit: the Partie ends by hand. */
    data object InProgress : TarotPartyStatus

    /** The last Manche planned is played: [winners] are every Participant at the best total. */
    data class Finished(val winners: List<Participant>) : TarotPartyStatus
}
