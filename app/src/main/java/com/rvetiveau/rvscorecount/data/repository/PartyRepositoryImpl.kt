package com.rvetiveau.rvscorecount.data.repository

import androidx.room.withTransaction
import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.data.dao.PartyDao
import com.rvetiveau.rvscorecount.data.dao.PlayerDao
import com.rvetiveau.rvscorecount.data.dao.playerIdFor
import com.rvetiveau.rvscorecount.data.dao.RoundDao
import com.rvetiveau.rvscorecount.data.dao.RoundScoreDao
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.data.entity.toDomain
import com.rvetiveau.rvscorecount.data.entity.toEntity
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
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
    private val roundScoreDao: RoundScoreDao,
    private val playerDao: PlayerDao
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
            participantDao.insertAll(participants.map { it.toEntity(playerDao.playerIdFor(it.name)) })
        }
    }

    override suspend fun getPartyById(id: UUID): Party? =
        partyDao.getById(id.toString())?.toDomain()

    override fun observeParty(id: UUID): Flow<Party?> =
        partyDao.observeById(id.toString()).map { it?.toDomain() }

    override suspend fun getPartyInProgress(): Party? =
        partyDao.getPartyInProgress()?.toDomain()

    override fun observeFinishedParties(): Flow<List<Party>> =
        partyDao.observeByStatus(PartyState.FINISHED.name).map { list -> list.map { it.toDomain() } }

    override suspend fun deleteParty(id: UUID) {
        partyDao.deleteById(id.toString())
    }

    override suspend fun addParticipant(participant: Participant, updatedParty: Party) {
        require(updatedParty.id == participant.partyId) { "The updated party must be the participant's party" }

        appDatabase.withTransaction {
            participantDao.shiftPositionsFrom(participant.partyId.toString(), participant.listPosition)
            participantDao.insert(participant.toEntity(playerDao.playerIdFor(participant.name)))
            partyDao.update(updatedParty.toEntity())
        }
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
            require(round.sevenHolderId == null || round.sevenHolderId.toString() in participantIds) {
                "The 7 holder must belong to the round's party"
            }

            roundDao.insert(round.toEntity())
            val scoreEntities = scores.map { (participantId, points) ->
                RoundScore(roundId = round.id, participantId = participantId, points = points).toEntity()
            }
            roundScoreDao.insertAll(scoreEntities)
            partyDao.update(updatedParty.toEntity())
        }
    }

    // Transaction requise (AD-3) : une correction à moitié écrite fausserait la somme de la manche
    override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) {
        require(scores.isNotEmpty()) { "A round must contain at least one score" }

        appDatabase.withTransaction {
            val stored = requireNotNull(roundDao.getById(round.id.toString())) { "Round ${round.id} does not exist" }
            require(stored.copy(sevenHolderId = round.sevenHolderId?.toString()) == round.toEntity()) {
                "Only the 7 holder of a round can be corrected"
            }
            val playedBy = roundScoreDao.getByRoundId(stored.id).mapTo(mutableSetOf()) { it.participantId }
            require(scores.keys.mapTo(mutableSetOf()) { it.toString() } == playedBy) {
                "A correction must cover exactly the participants who played the round"
            }
            require(round.sevenHolderId == null || round.sevenHolderId.toString() in playedBy) {
                "The 7 holder must have played the round"
            }

            roundDao.updateSevenHolder(stored.id, round.sevenHolderId?.toString())
            scores.forEach { (participantId, points) ->
                roundScoreDao.updatePoints(stored.id, participantId.toString(), points)
            }
        }
    }

    override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> =
        roundDao.getByPartyId(partyId.toString()).map { it.toDomain() }

    override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> =
        roundScoreDao.getByRoundId(roundId.toString()).map { it.toDomain() }
}
