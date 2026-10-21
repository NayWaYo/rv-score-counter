package com.rvetiveau.rvscorecount.data.repository

import androidx.room.withTransaction
import com.rvetiveau.rvscorecount.data.dao.BeloteRoundDao
import com.rvetiveau.rvscorecount.data.dao.ParticipantDao
import com.rvetiveau.rvscorecount.data.dao.PartyDao
import com.rvetiveau.rvscorecount.data.dao.TeamDao
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.data.entity.toDomain
import com.rvetiveau.rvscorecount.data.entity.toEntity
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Team
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.repository.BeloteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeloteRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val partyDao: PartyDao,
    private val participantDao: ParticipantDao,
    private val teamDao: TeamDao,
    private val beloteRoundDao: BeloteRoundDao
) : BeloteRepository {

    override suspend fun createParty(party: Party, teams: List<Team>, participants: List<Participant>) {
        require(party.endRule != null) { "A Belote party has an end rule" }
        require(teams.map { it.side }.sorted() == TeamSide.entries) { "A Belote party has one team per side" }
        require(teams.all { it.partyId == party.id }) { "Every team must belong to the party" }
        require(participants.all { it.partyId == party.id && it.teamSide != null }) {
            "Every participant must belong to the party and to a team"
        }

        appDatabase.withTransaction {
            partyDao.insert(party.toEntity())
            teamDao.insertAll(teams.map { it.toEntity() })
            participantDao.insertAll(participants.map { it.toEntity() })
        }
    }

    override suspend fun getTeams(partyId: UUID): List<Team> =
        teamDao.getByPartyId(partyId.toString()).map { it.toDomain() }.sortedBy { it.side }

    override fun observeTeams(partyId: UUID): Flow<List<Team>> =
        teamDao.observeByPartyId(partyId.toString()).map { list -> list.map { it.toDomain() }.sortedBy { it.side } }

    // Transaction requise (AD-3) : une Manche sans la rotation du Donneur serait un état incohérent
    override suspend fun saveRound(round: BeloteRound, updatedParty: Party) {
        require(updatedParty.id == round.partyId) { "The updated party must be the round's party" }

        appDatabase.withTransaction {
            beloteRoundDao.insert(round.toEntity())
            partyDao.update(updatedParty.toEntity())
        }
    }

    override suspend fun correctRound(round: BeloteRound, updatedParty: Party) {
        require(updatedParty.id == round.partyId) { "The updated party must be the round's party" }

        appDatabase.withTransaction {
            val stored = requireNotNull(beloteRoundDao.getById(round.id.toString())) { "Round ${round.id} does not exist" }
            require(stored.partyId == round.partyId.toString() && stored.roundNumber == round.roundNumber) {
                "Only what was entered for a round can be corrected"
            }
            val party = checkNotNull(partyDao.getById(stored.partyId)) { "Round ${round.id} has no party" }
            require(party.status == PartyState.IN_PROGRESS.name) { "Only a round of a party in progress can be corrected" }

            val entity = round.toEntity()
            beloteRoundDao.updateInput(
                entity.id, entity.takerSide, entity.takerTrickPoints, entity.beloteSide, entity.capotSide, entity.takerPlayerId
            )
            partyDao.update(updatedParty.toEntity())
        }
    }

    override suspend fun getRounds(partyId: UUID): List<BeloteRound> =
        beloteRoundDao.getByPartyId(partyId.toString()).map { it.toDomain() }

    override fun observeRounds(partyId: UUID): Flow<List<BeloteRound>> =
        beloteRoundDao.observeByPartyId(partyId.toString()).map { list -> list.map { it.toDomain() } }
}
