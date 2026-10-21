package com.rvetiveau.rvscorecount.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.data.entity.toDomain
import com.rvetiveau.rvscorecount.data.entity.toEntity
import com.rvetiveau.rvscorecount.domain.model.BeloteEndRule
import com.rvetiveau.rvscorecount.domain.model.BeloteRound
import com.rvetiveau.rvscorecount.domain.model.BeloteRoundInput
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Team
import com.rvetiveau.rvscorecount.domain.model.TeamSide.FIRST
import com.rvetiveau.rvscorecount.domain.model.TeamSide.SECOND
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Instrumented tests for [BeloteRepositoryImpl]: real Room transactions, cascades and constraints. */
class BeloteRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BeloteRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = BeloteRepositoryImpl(
            appDatabase = database,
            partyDao = database.partyDao(),
            participantDao = database.participantDao(),
            teamDao = database.teamDao(),
            beloteRoundDao = database.beloteRoundDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val party = Party(
        id = UUID.randomUUID(),
        gameId = "belote",
        status = PartyState.IN_PROGRESS,
        currentDealerIndex = 0,
        createdAt = 0L,
        endRule = BeloteEndRule.TargetScore(1000)
    )

    private val teams = listOf(Team(party.id, SECOND, "Eux"), Team(party.id, FIRST, "Nous"))

    private val participants = listOf(
        Participant(UUID.randomUUID(), party.id, "Alice", 0, FIRST),
        Participant(UUID.randomUUID(), party.id, "Bob", 1, SECOND),
        Participant(UUID.randomUUID(), party.id, "Chloé", 2, FIRST),
        Participant(UUID.randomUUID(), party.id, "David", 3, SECOND)
    )

    private fun aRound(number: Int, input: BeloteRoundInput = BeloteRoundInput(FIRST, 100)) =
        BeloteRound(UUID.randomUUID(), party.id, number, input)

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? =
        try {
            block()
            null
        } catch (t: Throwable) {
            t
        }

    @Test
    fun createsThePartyItsTeamsAndItsParticipantsTogether() = runTest {
        repository.createParty(party, teams, participants)

        assertEquals(BeloteEndRule.TargetScore(1000), database.partyDao().getById(party.id.toString())?.toDomain()?.endRule)
        assertEquals(listOf("Nous", "Eux"), repository.getTeams(party.id).map { it.name })
        assertEquals(listOf("Nous", "Eux"), repository.observeTeams(party.id).first().map { it.name })
        val stored = database.participantDao().getByPartyId(party.id.toString())
        assertEquals(listOf("FIRST", "SECOND", "FIRST", "SECOND"), stored.map { it.teamSide })
    }

    @Test
    fun rollsBackThePartyAndItsTeamsWhenAParticipantInsertFails() = runTest {
        val duplicated = participants + participants.first().copy(listPosition = 4)

        assertTrue(failureOf { repository.createParty(party, teams, duplicated) } != null)

        assertNull(database.partyDao().getById(party.id.toString()))
        assertEquals(emptyList<Team>(), repository.getTeams(party.id))
    }

    @Test
    fun refusesAPartyWithoutEndRuleTwoTeamsOrTeamMembership() = runTest {
        val noEndRule = failureOf { repository.createParty(party.copy(endRule = null), teams, participants) }
        val oneTeam = failureOf { repository.createParty(party, teams.take(1), participants) }
        val noTeam = failureOf { repository.createParty(party, teams, participants.map { it.copy(teamSide = null) }) }

        listOf(noEndRule, oneTeam, noTeam).forEach { assertTrue(it is IllegalArgumentException) }
        assertNull(database.partyDao().getById(party.id.toString()))
    }

    @Test
    fun savesARoundWithTheNextDealer() = runTest {
        repository.createParty(party, teams, participants)
        val round = aRound(1, BeloteRoundInput(SECOND, 71, beloteTeam = SECOND))

        repository.saveRound(round, party.copy(currentDealerIndex = 1))

        assertEquals(listOf(round), repository.getRounds(party.id))
        assertEquals(listOf(round), repository.observeRounds(party.id).first())
        assertEquals(1, database.partyDao().getById(party.id.toString())?.currentDealerIndex)
    }

    @Test
    fun saveRoundRollsBackTheDealerWhenTheRoundNumberIsTaken() = runTest {
        repository.createParty(party, teams, participants)
        repository.saveRound(aRound(1), party.copy(currentDealerIndex = 1))

        assertTrue(failureOf { repository.saveRound(aRound(1), party.copy(currentDealerIndex = 2)) } != null)

        assertEquals(1, repository.getRounds(party.id).size)
        assertEquals(1, database.partyDao().getById(party.id.toString())?.currentDealerIndex)
    }

    @Test
    fun correctRoundReplacesWhatWasEnteredAndSavesThePartyTogether() = runTest {
        repository.createParty(party, teams, participants)
        val round = aRound(1)
        repository.saveRound(round, party)
        val corrected = round.copy(input = BeloteRoundInput(SECOND, 0, beloteTeam = SECOND, capotTeam = FIRST))

        repository.correctRound(corrected, party.copy(status = PartyState.FINISHED))

        assertEquals(listOf(corrected), repository.getRounds(party.id))
        assertEquals("FINISHED", database.partyDao().getById(party.id.toString())?.status)
    }

    @Test
    fun correctRoundRefusesAnotherNumberAnUnknownRoundOrAFinishedParty() = runTest {
        repository.createParty(party, teams, participants)
        val round = aRound(1)
        repository.saveRound(round, party)

        val renumbered = failureOf { repository.correctRound(round.copy(roundNumber = 2), party) }
        val unknown = failureOf { repository.correctRound(aRound(1), party) }
        database.partyDao().update(party.copy(status = PartyState.FINISHED).toEntity())
        val finished = failureOf { repository.correctRound(round.copy(input = BeloteRoundInput(SECOND, 90)), party) }

        listOf(renumbered, unknown, finished).forEach { assertTrue(it is IllegalArgumentException) }
        assertEquals(listOf(round), repository.getRounds(party.id))
    }

    @Test
    fun deletingThePartyRemovesItsTeamsAndRounds() = runTest {
        repository.createParty(party, teams, participants)
        repository.saveRound(aRound(1), party)

        database.partyDao().deleteById(party.id.toString())

        assertEquals(emptyList<Team>(), repository.getTeams(party.id))
        assertEquals(emptyList<BeloteRound>(), repository.getRounds(party.id))
    }
}
