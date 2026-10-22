package com.rvetiveau.rvscorecount.data.repository

import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Players are read from the Participants of the stored Parties: a deleted Partie forgets its names. */
class PlayerRepositoryImpl @Inject constructor(
    private val participantDao: ParticipantDao
) : PlayerRepository {
    override fun observeKnownPlayers(): Flow<List<KnownPlayer>> =
        participantDao.observeKnownNames().map { rows ->
            rows.map { KnownPlayer(name = it.name, lastPlayedAt = it.lastPlayedAt) }
        }
}
