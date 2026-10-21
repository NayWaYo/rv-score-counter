package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.Team
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * What a Partie of Belote stores beyond [PartyRepository] (AD-8): its Équipes and its Manches.
 * The Partie itself (status, Donneur, deletion, resume) stays in [PartyRepository].
 */
interface BeloteRepository {
    /** Atomic aggregate write: the Partie, its two Équipes and its Participants, or none of them. */
    suspend fun createParty(party: Party, teams: List<Team>, participants: List<Participant>)

    suspend fun getTeams(partyId: UUID): List<Team>
    fun observeTeams(partyId: UUID): Flow<List<Team>>

    /** Atomic write: the Manche and [updatedParty] (next Donneur), or neither. */
    suspend fun saveRound(round: BeloteRound, updatedParty: Party)

    /** Replaces what was entered for an existing Manche of a Partie in progress; its number stays. */
    suspend fun correctRound(round: BeloteRound)

    suspend fun getRounds(partyId: UUID): List<BeloteRound>
    fun observeRounds(partyId: UUID): Flow<List<BeloteRound>>
}
