package com.rvetiveau.rvscorecount.data.repository

import androidx.room.withTransaction
import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.data.dao.PartyDao
import com.rvetiveau.rvscorecount.data.dao.RoundDao
import com.rvetiveau.rvscorecount.data.dao.RoundScoreDao
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.data.entity.toDomain
import com.rvetiveau.rvscorecount.data.entity.toEntity
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartyRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val partyDao: PartyDao,
    private val participantDao: ParticipantDao,
    private val roundDao: RoundDao,
    private val roundScoreDao: RoundScoreDao
) : PartyRepository {

    override suspend fun createParty(party: Party) {
        partyDao.insert(party.toEntity())
    }

    override suspend fun updateParty(party: Party) {
        partyDao.update(party.toEntity())
    }

    // Aggregate create (AC 1-3, 9) : a single transaction makes only the final IN_PROGRESS
    // Party + its Participants observable; any failure or cancellation leaves no partial row.
    override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
        appDatabase.withTransaction {
            partyDao.insert(party.toEntity())
            participantDao.insertAll(participants.map { it.toEntity() })
        }
    }

    override suspend fun getPartyById(id: UUID): Party? =
        partyDao.getById(id.toString())?.toDomain()

    override fun observeParty(id: UUID): Flow<Party?> =
        partyDao.observeById(id.toString()).map { it?.toDomain() }

    override suspend fun getPartyInProgress(): Party? =
        partyDao.getPartyInProgress()?.toDomain()

    override suspend fun addParticipant(participant: Participant) {
        participantDao.insert(participant.toEntity())
    }

    override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> =
        participantDao.getByPartyId(partyId.toString()).map { it.toDomain() }

    override fun observeParticipants(partyId: UUID): Flow<List<Participant>> =
        participantDao.observeByPartyId(partyId.toString()).map { list -> list.map { it.toDomain() } }

    // Transaction requise (AD-3) : un Round sans ses RoundScores, ou sans la rotation du Donneur,
    // serait un état incohérent
    override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) {
        require(scores.isNotEmpty()) { "A round must contain at least one score" }
        require(updatedParty.id == round.partyId) { "The updated party must be the round's party" }

        appDatabase.withTransaction {
            val participantIds = participantDao.getByPartyId(round.partyId.toString())
                .mapTo(mutableSetOf()) { it.id }
            require(scores.keys.all { it.toString() in participantIds }) {
                "Every scored participant must belong to the round's party"
            }

            roundDao.insert(round.toEntity())
            val scoreEntities = scores.map { (participantId, points) ->
                RoundScore(roundId = round.id, participantId = participantId, points = points).toEntity()
            }
            roundScoreDao.insertAll(scoreEntities)
            partyDao.update(updatedParty.toEntity())
        }
    }

    override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> =
        roundDao.getByPartyId(partyId.toString()).map { it.toDomain() }

    override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> =
        roundScoreDao.getByRoundId(roundId.toString()).map { it.toDomain() }
}
