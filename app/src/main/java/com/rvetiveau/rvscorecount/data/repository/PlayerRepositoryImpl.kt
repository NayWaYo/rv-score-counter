package com.rvetiveau.rvscorecount.data.repository

import com.rvetiveau.rvscorecount.data.dao.PlayerDao
import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Joueurs outlive their Parties (Story 10.1): one without any Partie left is still suggested, last. */
class PlayerRepositoryImpl @Inject constructor(
    private val playerDao: PlayerDao
) : PlayerRepository {
    override fun observeKnownPlayers(): Flow<List<KnownPlayer>> =
        playerDao.observeKnownNames().map { rows ->
            rows.map { KnownPlayer(name = it.name, lastPlayedAt = it.lastPlayedAt) }
        }
}
