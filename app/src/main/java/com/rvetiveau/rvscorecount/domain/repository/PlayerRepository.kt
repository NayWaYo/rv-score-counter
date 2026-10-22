package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import java.util.UUID
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    /** Every Joueur, with the date of their latest Partie when they still have one. */
    fun observeKnownPlayers(): Flow<List<KnownPlayer>>

    /** Every Joueur with their finished Parties and latest Partie, in no particular order. */
    fun observePlayers(): Flow<List<PlayerSummary>>

    /**
     * Renames the Joueur [id] and every one of their Participants together, or nothing changes;
     * false when no Joueur has this id. [name] is already trimmed and free (see RenamePlayerUseCase).
     */
    suspend fun renamePlayer(id: UUID, name: String): Boolean
}
