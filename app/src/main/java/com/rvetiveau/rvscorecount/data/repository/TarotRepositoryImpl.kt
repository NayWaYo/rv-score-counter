package com.rvetiveau.rvscorecount.data.repository

import androidx.room.withTransaction
import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.data.dao.PartyDao
import com.rvetiveau.rvscorecount.data.dao.TarotRoundDao
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.data.entity.PartyEntity
import com.rvetiveau.rvscorecount.data.entity.toDomain
import com.rvetiveau.rvscorecount.data.entity.toEntity
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.TarotRound
import com.rvetiveau.rvscorecount.domain.repository.TarotRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class TarotRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val partyDao: PartyDao,
    private val participantDao: ParticipantDao,
    private val tarotRoundDao: TarotRoundDao
) : TarotRepository {

    // Transaction requise (AD-3) : une Manche sans la rotation du Donneur serait un état incohérent
    override suspend fun saveRound(round: TarotRound, updatedParty: Party) {
        require(updatedParty.id == round.partyId) { "The updated party must be the round's party" }
        appDatabase.withTransaction {
            requireSeats(tarotParty(round.partyId), round)
            tarotRoundDao.insert(round.toEntity())
            partyDao.update(updatedParty.toEntity())
        }
    }

    override suspend fun correctRound(round: TarotRound, updatedParty: Party) {
        require(updatedParty.id == round.partyId) { "The updated party must be the round's party" }
        appDatabase.withTransaction {
            val stored = requireNotNull(tarotRoundDao.getById(round.id.toString())) { "Round ${round.id} does not exist" }
            require(stored.partyId == round.partyId.toString() && stored.roundNumber == round.roundNumber) {
                "Only what was entered for a round can be corrected"
            }
            val party = tarotParty(round.partyId)
            require(party.status == PartyState.IN_PROGRESS.name) { "Only a round of a party in progress can be corrected" }
            requireSeats(party, round)
            tarotRoundDao.update(round.toEntity())
            partyDao.update(updatedParty.toEntity())
        }
    }

    override suspend fun getRounds(partyId: UUID): List<TarotRound> =
        tarotRoundDao.getByPartyId(partyId.toString()).map { it.toDomain() }

    override fun observeRounds(partyId: UUID): Flow<List<TarotRound>> =
        tarotRoundDao.observeByPartyId(partyId.toString()).map { list -> list.map { it.toDomain() } }

    private suspend fun tarotParty(partyId: UUID): PartyEntity {
        val party = requireNotNull(partyDao.getById(partyId.toString())) { "Party $partyId does not exist" }
        require(party.gameId == GameIds.TAROT) { "Party $partyId is not a Tarot party" }
        return party
    }

    private suspend fun requireSeats(party: PartyEntity, round: TarotRound) {
        val seats = participantDao.getByPartyId(party.id).mapTo(mutableSetOf()) { it.id }
        require(round.input.takerId.toString() in seats) { "The taker of round ${round.id} is not in its party" }
        round.input.partnerId?.let { partner ->
            require(partner.toString() in seats) { "The partner of round ${round.id} is not in its party" }
        }
    }
}
