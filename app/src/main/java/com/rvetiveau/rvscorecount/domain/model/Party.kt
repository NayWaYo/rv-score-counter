package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

data class Party(
    val id: UUID,
    val gameId: String,
    val status: PartyState,
    val currentDealerIndex: Int,
    val createdAt: Long
)
