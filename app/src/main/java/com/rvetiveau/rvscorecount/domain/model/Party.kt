package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/** [endRule]: how a Partie of Belote ends; null for the Papayoo. */
data class Party(
    val id: UUID,
    val gameId: String,
    val status: PartyState,
    val currentDealerIndex: Int,
    val createdAt: Long,
    val endRule: BeloteEndRule? = null
)
