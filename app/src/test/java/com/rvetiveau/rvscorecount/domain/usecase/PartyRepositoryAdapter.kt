package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PlayedGameName
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.repository.FreeGameRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.domain.repository.TarotRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A [PartyRepository] whose every method fails: a test overrides only what its subject uses. */
open class PartyRepositoryAdapter : PartyRepository {
    override suspend fun createParty(party: Party): Unit = error("not used in this test")
    override suspend fun updateParty(party: Party): Unit = error("not used in this test")
    override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>): Unit = error("not used in this test")
    override suspend fun getPartyById(id: UUID): Party? = error("not used in this test")
    override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
    override suspend fun getPartyInProgress(): Party? = error("not used in this test")
    override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
    override suspend fun deleteParty(id: UUID): Unit = error("not used in this test")
    override suspend fun addParticipant(participant: Participant, updatedParty: Party): Unit = error("not used in this test")
    override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = error("not used in this test")
    override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
    override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party): Unit = error("not used in this test")
    override suspend fun correctRound(round: Round, scores: Map<UUID, Int>): Unit = error("not used in this test")
    override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
    override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
}

/** In-memory Manches of the Jeu libre and the game names already played (Stories 11.1, 11.2). */
open class FakeFreeGameRepository(gameNames: List<PlayedGameName> = emptyList()) : FreeGameRepository {
    val rounds = MutableStateFlow<List<FreeRound>>(emptyList())
    val gameNames = MutableStateFlow(gameNames)

    /** The Partie as each write left it, last one last. */
    val updatedParties = mutableListOf<Party>()

    override suspend fun saveRound(round: FreeRound, updatedParty: Party) {
        rounds.value = rounds.value + round
        updatedParties += updatedParty
    }

    override suspend fun correctRound(round: FreeRound, updatedParty: Party) {
        rounds.value = rounds.value.map { if (it.id == round.id) round else it }
        updatedParties += updatedParty
    }

    override suspend fun getRounds(partyId: UUID): List<FreeRound> = rounds.value.filter { it.partyId == partyId }

    override fun observeRounds(partyId: UUID): Flow<List<FreeRound>> = rounds

    override fun observeGameNames(): Flow<List<PlayedGameName>> = gameNames
}

/** In-memory Manches of the Tarot (Story 13.3). */
open class FakeTarotRepository : TarotRepository {
    val rounds = MutableStateFlow<List<TarotRound>>(emptyList())

    /** The Partie as each write left it, last one last. */
    val updatedParties = mutableListOf<Party>()

    override suspend fun saveRound(round: TarotRound, updatedParty: Party) {
        rounds.value = rounds.value + round
        updatedParties += updatedParty
    }

    override suspend fun correctRound(round: TarotRound, updatedParty: Party) {
        rounds.value = rounds.value.map { if (it.id == round.id) round else it }
        updatedParties += updatedParty
    }

    override suspend fun getRounds(partyId: UUID): List<TarotRound> = rounds.value.filter { it.partyId == partyId }

    override fun observeRounds(partyId: UUID): Flow<List<TarotRound>> = rounds
}
