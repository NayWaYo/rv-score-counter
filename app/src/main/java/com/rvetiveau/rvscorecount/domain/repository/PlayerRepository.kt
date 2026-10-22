package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    /** Every Participant name of every stored Partie, once per exact spelling. */
    fun observeKnownPlayers(): Flow<List<KnownPlayer>>
}
