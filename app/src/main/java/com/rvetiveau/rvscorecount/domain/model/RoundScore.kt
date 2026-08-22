package com.rvetiveau.rvscorecount.domain.model

import java.util.UUID

data class RoundScore(
    val roundId: UUID,
    val participantId: UUID,
    val points: Int
)
