package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import com.rvetiveau.rvscorecount.domain.repository.PlayerRepository
import com.rvetiveau.rvscorecount.domain.repository.PlayerSeat
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/** In-memory Joueurs: [knownPlayers] for the suggestions, [players] for the « Joueurs » screen. */
class FakePlayerRepository(
    private val knownPlayers: List<KnownPlayer> = emptyList(),
    players: List<PlayerSummary> = emptyList()
) : PlayerRepository {
    val players = MutableStateFlow(players)
    var renameFailure: Throwable? = null

    /** Seats by Joueur, and the Joueur of each seat by Partie (Story 10.3). */
    val seats = mutableMapOf<UUID, List<PlayerSeat>>()
    val playerIdsBySeat = mutableMapOf<UUID, Map<UUID, UUID>>()

    /** [names] listed from the most to the least recently played. */
    constructor(vararg names: String) : this(names.mapIndexed { index, name -> KnownPlayer(name, (names.size - index).toLong()) })

    override fun observeKnownPlayers(): Flow<List<KnownPlayer>> = flowOf(knownPlayers)

    override fun observePlayers(): Flow<List<PlayerSummary>> = players

    override suspend fun renamePlayer(id: UUID, name: String): Boolean {
        renameFailure?.let { throw it }
        if (players.value.none { it.id == id }) return false
        players.value = players.value.map { if (it.id == id) it.copy(name = name) else it }
        return true
    }

    override suspend fun getSeats(playerId: UUID): List<PlayerSeat> = seats[playerId].orEmpty()

    override suspend fun getPlayerIdsBySeat(partyId: UUID): Map<UUID, UUID> = playerIdsBySeat[partyId].orEmpty()

    var writeFailure: Throwable? = null
    val merges = mutableListOf<Pair<UUID, UUID>>()

    override suspend fun mergePlayers(keptId: UUID, absorbedId: UUID) {
        writeFailure?.let { throw it }
        merges += keptId to absorbedId
        seats[keptId] = seats[keptId].orEmpty() + seats[absorbedId].orEmpty()
        seats.remove(absorbedId)
        players.value = players.value.filterNot { it.id == absorbedId }
    }

    override suspend fun deletePlayer(id: UUID): Boolean {
        writeFailure?.let { throw it }
        if (players.value.none { it.id == id }) return false
        players.value = players.value.filterNot { it.id == id }
        return true
    }
}
