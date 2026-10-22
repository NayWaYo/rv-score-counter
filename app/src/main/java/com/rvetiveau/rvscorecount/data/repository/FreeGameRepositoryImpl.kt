package com.rvetiveau.rvscorecount.data.repository

import androidx.room.withTransaction
import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.data.dao.PartyDao
import com.rvetiveau.rvscorecount.data.dao.RoundDao
import com.rvetiveau.rvscorecount.data.dao.RoundScoreDao
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.data.entity.RoundEntity
import com.rvetiveau.rvscorecount.data.entity.RoundScoreEntity
import com.rvetiveau.rvscorecount.data.entity.toEntity
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PlayedGameName
import com.rvetiveau.rvscorecount.domain.repository.FreeGameRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** Manches of the Jeu libre (AD-9) in `rounds` without a 7's suit, their scores in `round_scores`. */
@Singleton
class FreeGameRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val partyDao: PartyDao,
    private val participantDao: ParticipantDao,
    private val roundDao: RoundDao,
    private val roundScoreDao: RoundScoreDao
) : FreeGameRepository {

    // Transaction requise (AD-3) : une Manche sans tous ses scores ou sans le Donneur suivant serait incohérente.
    override suspend fun saveRound(round: FreeRound, updatedParty: Party) {
        require(updatedParty.id == round.partyId) { "The updated party must be the round's party" }
        appDatabase.withTransaction {
            val party = requireNotNull(partyDao.getById(round.partyId.toString())) { "Party ${round.partyId} does not exist" }
            require(party.gameId == GameIds.FREE) { "Party ${round.partyId} is not a Jeu libre party" }
            val seats = participantDao.getByPartyId(party.id).mapTo(mutableSetOf()) { it.id }
            require(round.scores.keys.mapTo(mutableSetOf()) { it.toString() } == seats) {
                "A Manche of Jeu libre scores every Participant of its Partie, and only them"
            }
            roundDao.insert(RoundEntity(round.id.toString(), party.id, round.roundNumber, suit = null))
            roundScoreDao.insertAll(round.scores.map { (participantId, points) ->
                RoundScoreEntity(round.id.toString(), participantId.toString(), points)
            })
            partyDao.update(updatedParty.toEntity())
        }
    }

    // Transaction requise (AD-3) : une correction à moitié écrite fausserait les totaux.
    override suspend fun correctRound(round: FreeRound) {
        appDatabase.withTransaction {
            val stored = requireNotNull(roundDao.getById(round.id.toString())) { "Round ${round.id} does not exist" }
            require(stored == RoundEntity(round.id.toString(), round.partyId.toString(), round.roundNumber, suit = null)) {
                "Only the scores of a Manche of Jeu libre can be corrected"
            }
            val playedBy = roundScoreDao.getByRoundId(stored.id).mapTo(mutableSetOf()) { it.participantId }
            require(round.scores.keys.mapTo(mutableSetOf()) { it.toString() } == playedBy) {
                "A correction must cover exactly the Participants who played the Manche"
            }
            round.scores.forEach { (participantId, points) ->
                roundScoreDao.updatePoints(stored.id, participantId.toString(), points)
            }
        }
    }

    override suspend fun getRounds(partyId: UUID): List<FreeRound> =
        assemble(roundDao.getByPartyId(partyId.toString()), roundScoreDao.getByPartyId(partyId.toString()))

    override fun observeRounds(partyId: UUID): Flow<List<FreeRound>> =
        combine(roundDao.observeByPartyId(partyId.toString()), roundScoreDao.observeByPartyId(partyId.toString())) { rounds, scores ->
            assemble(rounds, scores)
        }

    override fun observeGameNames(): Flow<List<PlayedGameName>> =
        partyDao.observeGameNames(GameIds.FREE).map { rows ->
            rows.map { PlayedGameName(it.name, checkNotNull(it.lastPlayedAt) { "A Partie always has a creation time" }) }
        }

    private fun assemble(rounds: List<RoundEntity>, scores: List<RoundScoreEntity>): List<FreeRound> {
        val scoresByRound = scores.groupBy { it.roundId }
        return rounds.map { round ->
            check(round.suit == null) { "Round ${round.id} has a 7's suit: it is not a Manche of Jeu libre" }
            FreeRound(
                id = UUID.fromString(round.id),
                partyId = UUID.fromString(round.partyId),
                roundNumber = round.roundNumber,
                scores = scoresByRound[round.id].orEmpty().associate { UUID.fromString(it.participantId) to it.points }
            )
        }
    }
}
