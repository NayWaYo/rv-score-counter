package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PlayedGameName
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * The Manches of the Jeu libre (AD-9). Its Partie and Participants are created and read through
 * [PartyRepository], like every Jeu.
 */
interface FreeGameRepository {
    /**
     * Atomic write: [round], with a score for every Participant of its Partie, and [updatedParty]
     * (next Donneur) commit together, or nothing does.
     */
    suspend fun saveRound(round: FreeRound, updatedParty: Party)

    /**
     * Atomic write: replaces the scores of the validated Manche [round], for the same Participants
     * only, with [updatedParty] (FINISHED when the correction reaches the end rule).
     */
    suspend fun correctRound(round: FreeRound, updatedParty: Party)

    /** The Manches of [partyId] in order; a Manche with a 7's suit is not a Jeu libre one and throws. */
    suspend fun getRounds(partyId: UUID): List<FreeRound>

    fun observeRounds(partyId: UUID): Flow<List<FreeRound>>

    /** The game name of every stored Partie of Jeu libre, once per exact spelling. */
    fun observeGameNames(): Flow<List<PlayedGameName>>
}
