package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/** [sevenHolderId]: Participant who took the 7 of [suit]; null for rounds recorded before it existed. */
data class Round(
    val id: UUID,
    val partyId: UUID,
    val roundNumber: Int,
    val suit: CardSuit,
    val sevenHolderId: UUID? = null
)
