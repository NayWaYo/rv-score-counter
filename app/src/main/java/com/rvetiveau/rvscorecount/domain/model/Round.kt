package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

data class Round(
    val id: UUID,
    val partyId: UUID,
    val roundNumber: Int,
    val suit: CardSuit
)
