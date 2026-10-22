package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/**
 * [endRule]: how a Partie of Belote or of Jeu libre ends, null for the Papayoo and for a Jeu libre
 * ended by hand. [gameName] and [winDirection]: the rules of a Jeu libre (AD-9), null for the others.
 */
data class Party(
    val id: UUID,
    val gameId: String,
    val status: PartyState,
    val currentDealerIndex: Int,
    val createdAt: Long,
    val endRule: EndRule? = null,
    val gameName: String? = null,
    val winDirection: WinDirection? = null
)
