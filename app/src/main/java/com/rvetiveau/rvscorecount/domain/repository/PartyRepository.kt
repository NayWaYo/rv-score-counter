package com.rvetiveau.rvscorecount.domain.repository

import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface PartyRepository {
    suspend fun createParty(party: Party)
    suspend fun updateParty(party: Party)
    /** Atomic aggregate write (AC 1-3, 9): the Party (already IN_PROGRESS) and all its Participants
     * commit in a single Room transaction, or none of them persist. */
    suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>)
    suspend fun getPartyById(id: UUID): Party?
    fun observeParty(id: UUID): Flow<Party?>
    suspend fun getPartyInProgress(): Party?
    suspend fun addParticipant(participant: Participant)
    suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant>
    fun observeParticipants(partyId: UUID): Flow<List<Participant>>
    /** Atomic write (FR-8, FR-9): the Round, its RoundScores and [updatedParty] (next Donneur)
     * commit in a single Room transaction, or none of them persist. */
    suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party)
    suspend fun getRoundsByPartyId(partyId: UUID): List<Round>
    suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore>
}
