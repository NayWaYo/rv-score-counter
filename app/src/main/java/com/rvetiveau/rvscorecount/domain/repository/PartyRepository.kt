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
    /** FINISHED parties, most recent first (Historique). */
    fun observeFinishedParties(): Flow<List<Party>>
    /** Definitive delete of a Party and, by cascade, its Participants, Rounds and RoundScores. */
    suspend fun deleteParty(id: UUID)
    /** Atomic write (FR-4): Participants at or after [participant]'s listPosition move one place
     * down, [participant] is inserted and [updatedParty] (next Donneur) is saved, or nothing is. */
    suspend fun addParticipant(participant: Participant, updatedParty: Party)
    suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant>
    fun observeParticipants(partyId: UUID): Flow<List<Participant>>
    /** Atomic write (FR-8, FR-9): the Round, its RoundScores and [updatedParty] (next Donneur)
     * commit in a single Room transaction, or none of them persist. */
    suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party)
    /** Atomic correction of a validated Round: its 7 holder and the points of the Participants
     * who played it are replaced together, or nothing changes. Number, suit, the set of scored
     * Participants and the Donneur stay as they were. */
    suspend fun correctRound(round: Round, scores: Map<UUID, Int>)
    suspend fun getRoundsByPartyId(partyId: UUID): List<Round>
    suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore>
}
