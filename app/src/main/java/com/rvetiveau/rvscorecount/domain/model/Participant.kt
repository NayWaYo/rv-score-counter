package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

/** [teamSide]: the Participant's Équipe in a team game; null for the Papayoo. */
data class Participant(
    val id: UUID,
    val partyId: UUID,
    val name: String,
    val listPosition: Int,
    val teamSide: TeamSide? = null
)
