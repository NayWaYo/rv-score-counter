package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

data class Participant(
    val id: UUID,
    val partyId: UUID,
    val name: String,
    val listPosition: Int
)
