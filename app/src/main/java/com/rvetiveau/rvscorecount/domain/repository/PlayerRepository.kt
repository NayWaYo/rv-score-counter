package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    /** Every Joueur, with the date of their latest Partie when they still have one. */
    fun observeKnownPlayers(): Flow<List<KnownPlayer>>
}
