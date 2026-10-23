package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * The Manches of the Tarot (AD-10). Its Partie and Participants are created and read through
 * [PartyRepository], like the Jeu libre.
 */
interface TarotRepository {
    /**
     * Atomic write: [round], whose taker and partner are Participants of its Partie of Tarot, and
     * [updatedParty] (next Donneur, FINISHED at the end) commit together, or nothing does.
     */
    suspend fun saveRound(round: TarotRound, updatedParty: Party)

    /**
     * Atomic write: replaces what was entered for the validated Manche [round] of a Partie in progress
     * (its number stays) and saves [updatedParty], FINISHED when the correction reaches the end.
     */
    suspend fun correctRound(round: TarotRound, updatedParty: Party)

    suspend fun getRounds(partyId: UUID): List<TarotRound>
    fun observeRounds(partyId: UUID): Flow<List<TarotRound>>
}
