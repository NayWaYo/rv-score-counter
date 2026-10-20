package com.rvetiveau.rvscorecount.domain.engine

/**
 * How to deal one round for a given number of players: [cardsPerPlayer], dealt by packets of
 * three then a smaller last packet, and [cardsPassed] given to the left neighbour before play.
 * [removedSuitRank] is the rank taken out of each of the four suits before dealing, null when the
 * whole deck is dealt.
 */
data class DealingRules(
    val cardsPerPlayer: Int,
    val cardsPassed: Int,
    val removedSuitRank: Int? = null
) {
    init {
        require(cardsPerPlayer > 0) { "cardsPerPlayer must be positive" }
        require(cardsPassed in 0..cardsPerPlayer) { "cardsPassed must be within the player's hand" }
    }

    val fullPackets: Int get() = cardsPerPlayer / PACKET_SIZE

    /** Cards of the last, smaller packet; 0 when the hand is made of full packets only. */
    val lastPacket: Int get() = cardsPerPlayer % PACKET_SIZE

    companion object {
        const val PACKET_SIZE = 3
    }
}
