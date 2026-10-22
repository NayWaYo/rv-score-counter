package com.rvetiveau.rvscorecount.data.repository

import androidx.room.withTransaction
import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.data.dao.PlayerDao
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.model.playerNameKey
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import com.rvetiveau.rvscorecount.domain.repository.PlayerSeat
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Joueurs outlive their Parties (Story 10.1): one without any Partie left is still suggested, last. */
class PlayerRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val playerDao: PlayerDao,
    private val participantDao: ParticipantDao
) : PlayerRepository {
    override fun observeKnownPlayers(): Flow<List<KnownPlayer>> =
        playerDao.observeKnownNames().map { rows ->
            rows.map { KnownPlayer(name = it.name, lastPlayedAt = it.lastPlayedAt) }
        }

    override fun observePlayers(): Flow<List<PlayerSummary>> =
        playerDao.observeSummaries().map { rows ->
            rows.map { PlayerSummary(UUID.fromString(it.id), it.name, it.finishedParties, it.lastPlayedAt) }
        }

    // The name lives on the Joueur and on each of their seats, which the existing screens read.
    override suspend fun renamePlayer(id: UUID, name: String): Boolean =
        appDatabase.withTransaction {
            val renamed = playerDao.rename(id.toString(), name, playerNameKey(name)) > 0
            if (renamed) participantDao.renamePlayer(id.toString(), name)
            renamed
        }

    override suspend fun getSeats(playerId: UUID): List<PlayerSeat> =
        playerDao.getSeats(playerId.toString()).map { PlayerSeat(UUID.fromString(it.partyId), UUID.fromString(it.participantId)) }

    override suspend fun getPlayerIdsBySeat(partyId: UUID): Map<UUID, UUID> =
        playerDao.getPlayersOfParty(partyId.toString()).associate { UUID.fromString(it.participantId) to UUID.fromString(it.playerId) }

    override suspend fun mergePlayers(keptId: UUID, absorbedId: UUID) {
        appDatabase.withTransaction {
            val kept = checkNotNull(playerDao.getById(keptId.toString())) { "No Joueur $keptId to keep" }
            participantDao.movePlayerSeats(absorbedId.toString(), keptId.toString(), kept.name)
            check(playerDao.deleteById(absorbedId.toString()) == 1) { "No Joueur $absorbedId to absorb" }
        }
    }

    override suspend fun deletePlayer(id: UUID): Boolean = playerDao.deleteById(id.toString()) > 0
}
