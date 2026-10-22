package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import com.rvetiveau.rvscorecount.domain.model.PlayerSummary
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/** A place a Joueur took at a Partie: the Participant [participantId] of [partyId]. */
data class PlayerSeat(val partyId: UUID, val participantId: UUID)

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

    /** Every seat of the Joueur [playerId], in every stored Partie (Story 10.3). */
    suspend fun getSeats(playerId: UUID): List<PlayerSeat>

    /** The Joueur of each Participant of [partyId], keyed by participant id. */
    suspend fun getPlayerIdsBySeat(partyId: UUID): Map<UUID, UUID>

    /**
     * Every seat of [absorbedId] moves to [keptId] under its name, then [absorbedId] is deleted,
     * together or not at all (Story 10.4). The caller has checked they never sat at the same Partie.
     */
    suspend fun mergePlayers(keptId: UUID, absorbedId: UUID)

    /** Deletes the Joueur [id], who has no seat left; false when no Joueur has this id. */
    suspend fun deletePlayer(id: UUID): Boolean
}
