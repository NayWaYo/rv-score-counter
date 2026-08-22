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
    suspend fun getPartyById(id: UUID): Party?
    fun observeParty(id: UUID): Flow<Party?>
    suspend fun getPartyInProgress(): Party?
    suspend fun addParticipant(participant: Participant)
    suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant>
    fun observeParticipants(partyId: UUID): Flow<List<Participant>>
    suspend fun saveRound(round: Round, scores: Map<UUID, Int>)
    suspend fun getRoundsByPartyId(partyId: UUID): List<Round>
    suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore>
}
