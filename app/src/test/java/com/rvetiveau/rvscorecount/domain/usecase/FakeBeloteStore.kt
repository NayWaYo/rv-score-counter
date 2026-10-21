package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.model.Team
import com.rvetiveau.rvscorecount.domain.model.TeamSide
import com.rvetiveau.rvscorecount.domain.repository.BeloteRepository
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * In-memory Room stand-in for the Belote tests: one store behind both repositories, observable,
 * with an optional [writeFailure] thrown by every write. Papayoo-only methods are not used.
 */
class FakeBeloteStore : PartyRepository, BeloteRepository {
    val parties = MutableStateFlow<Map<UUID, Party>>(emptyMap())
    val teams = MutableStateFlow<List<Team>>(emptyList())
    val participants = MutableStateFlow<List<Participant>>(emptyList())
    val rounds = MutableStateFlow<List<BeloteRound>>(emptyList())
    var writeFailure: Throwable? = null

    /** A Belote party in progress: « Nous » (Alice, Chloé) against « Eux » (Bob, David). */
    fun seedParty(
        endRule: BeloteEndRule = BeloteEndRule.TargetScore(1000),
        status: PartyState = PartyState.IN_PROGRESS,
        dealerIndex: Int = 0
    ): Party {
        val party = Party(UUID.randomUUID(), "belote", status, dealerIndex, 0L, endRule)
        parties.value += party.id to party
        teams.value += listOf(Team(party.id, TeamSide.FIRST, "Nous"), Team(party.id, TeamSide.SECOND, "Eux"))
        participants.value += listOf("Alice", "Bob", "Chloé", "David").mapIndexed { position, name ->
            Participant(UUID.randomUUID(), party.id, name, position, if (position % 2 == 0) TeamSide.FIRST else TeamSide.SECOND)
        }
        return party
    }

    private fun checkWritable() {
        writeFailure?.let { throw it }
    }

    // BeloteRepository

    override suspend fun createParty(party: Party, teams: List<Team>, participants: List<Participant>) {
        checkWritable()
        parties.value += party.id to party
        this.teams.value += teams
        this.participants.value += participants
    }

    override suspend fun getTeams(partyId: UUID): List<Team> = teams.value.filter { it.partyId == partyId }
    override fun observeTeams(partyId: UUID): Flow<List<Team>> = teams.map { list -> list.filter { it.partyId == partyId } }

    override suspend fun saveRound(round: BeloteRound, updatedParty: Party) {
        checkWritable()
        rounds.value += round
        parties.value += updatedParty.id to updatedParty
    }

    override suspend fun correctRound(round: BeloteRound, updatedParty: Party) {
        checkWritable()
        rounds.value = rounds.value.map { if (it.id == round.id) round else it }
        parties.value += updatedParty.id to updatedParty
    }

    override suspend fun getRounds(partyId: UUID): List<BeloteRound> =
        rounds.value.filter { it.partyId == partyId }.sortedBy { it.roundNumber }

    override fun observeRounds(partyId: UUID): Flow<List<BeloteRound>> =
        rounds.map { list -> list.filter { it.partyId == partyId }.sortedBy { it.roundNumber } }

    // PartyRepository

    override suspend fun getPartyById(id: UUID): Party? = parties.value[id]
    override fun observeParty(id: UUID): Flow<Party?> = parties.map { it[id] }
    override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> =
        participants.value.filter { it.partyId == partyId }.sortedBy { it.listPosition }
    override fun observeParticipants(partyId: UUID): Flow<List<Participant>> =
        participants.map { list -> list.filter { it.partyId == partyId }.sortedBy { it.listPosition } }

    override suspend fun createParty(party: Party) = error("not used by the Belote")
    override suspend fun updateParty(party: Party) = error("not used by the Belote")
    override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used by the Belote")
    override suspend fun getPartyInProgress(): Party? = error("not used by the Belote")
    override fun observeFinishedParties(): Flow<List<Party>> = error("not used by the Belote")
    override suspend fun deleteParty(id: UUID) = error("not used by the Belote")
    override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used by the Belote")
    override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used by the Belote")
    override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) = error("not used by the Belote")
    override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used by the Belote")
    override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used by the Belote")
}
