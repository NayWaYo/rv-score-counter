package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/** One of the two Équipes of a Partie, identified by its [side]. */
data class Team(
    val partyId: UUID,
    val side: TeamSide,
    val name: String
)
