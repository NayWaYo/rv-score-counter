package com.rvetiveau.rvscorecount.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.domain.engine.GameIds
import com.rvetiveau.rvscorecount.domain.model.EndRule
import com.rvetiveau.rvscorecount.domain.model.FreeRound
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.PlayedGameName
import com.rvetiveau.rvscorecount.domain.model.WinDirection
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Manches of the Jeu libre (Story 11.1) in a real Room database, beside a Partie of Papayoo. */
class FreeGameRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var parties: PartyRepositoryImpl
    private lateinit var repository: FreeGameRepositoryImpl

    private val party = Party(
        id = UUID.randomUUID(),
        gameId = GameIds.FREE,
        status = PartyState.IN_PROGRESS,
        currentDealerIndex = 0,
        createdAt = 10,
        endRule = EndRule.TargetScore(100),
        gameName = "Skyjo",
        winDirection = WinDirection.LOWEST_WINS
    )
    private val alice = Participant(UUID.randomUUID(), party.id, "Alice", 0)
    private val bob = Participant(UUID.randomUUID(), party.id, "Bob", 1)

    @Before
    fun setUp() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        parties = PartyRepositoryImpl(
            database, database.partyDao(), database.participantDao(), database.roundDao(), database.roundScoreDao(), database.playerDao()
        )
        repository = FreeGameRepositoryImpl(
            database, database.partyDao(), database.participantDao(), database.roundDao(), database.roundScoreDao()
        )
        parties.createPartyWithParticipants(party, listOf(alice, bob))
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun round(number: Int, aliceScore: Int, bobScore: Int) =
        FreeRound(UUID.randomUUID(), party.id, number, mapOf(alice.id to aliceScore, bob.id to bobScore))

    @Test
    fun theRulesOfTheJeuLibreTravelWithItsPartie() = runTest {
        assertEquals(party, parties.getPartyById(party.id))
        assertEquals(listOf("Alice", "Bob"), parties.getParticipantsByPartyId(party.id).map { it.name })
    }

    @Test
    fun aMancheIsSavedWithAllItsScoresNegativesIncludedAndTheNextDealer() = runTest {
        val first = round(1, aliceScore = -12, bobScore = 30)

        repository.saveRound(first, party.copy(currentDealerIndex = 1))

        assertEquals(listOf(first), repository.getRounds(party.id))
        assertEquals(listOf(first), repository.observeRounds(party.id).first())
        assertEquals(1, parties.getPartyById(party.id)?.currentDealerIndex)
    }

    @Test
    fun aMancheMissingAParticipantIsRefusedAndNothingIsWritten() = runTest {
        val partial = FreeRound(UUID.randomUUID(), party.id, 1, mapOf(alice.id to 5))

        val failure = runCatching { repository.saveRound(partial, party.copy(currentDealerIndex = 1)) }

        assertTrue(failure.isFailure)
        assertTrue(repository.getRounds(party.id).isEmpty())
        assertEquals(0, parties.getPartyById(party.id)?.currentDealerIndex)
    }

    @Test
    fun aMancheCannotBeSavedInAPartieOfAnotherJeu() = runTest {
        val papayoo = Party(UUID.randomUUID(), GameIds.PAPAYOO, PartyState.IN_PROGRESS, 0, 20)
        parties.createPartyWithParticipants(papayoo, listOf(Participant(UUID.randomUUID(), papayoo.id, "Chloé", 0)))

        val failure = runCatching { repository.saveRound(FreeRound(UUID.randomUUID(), papayoo.id, 1, emptyMap()), papayoo) }

        assertTrue(failure.isFailure)
    }

    @Test
    fun aValidatedMancheHasItsScoresCorrected() = runTest {
        val first = round(1, aliceScore = 10, bobScore = 20)
        repository.saveRound(first, party)

        repository.correctRound(first.copy(scores = mapOf(alice.id to -4, bob.id to 7)), party)

        assertEquals(mapOf(alice.id to -4, bob.id to 7), repository.getRounds(party.id).single().scores)
        val stranger = first.copy(scores = mapOf(alice.id to 1, UUID.randomUUID() to 2))
        assertTrue(runCatching { repository.correctRound(stranger, party) }.isFailure)
    }

    @Test
    fun aCorrectionAndTheEndItReachesCommitTogetherOrNotAtAll() = runTest {
        val first = round(1, aliceScore = 10, bobScore = 20)
        repository.saveRound(first, party)
        val finished = party.copy(status = PartyState.FINISHED)

        repository.correctRound(first.copy(scores = mapOf(alice.id to 100, bob.id to 20)), finished)

        assertEquals(PartyState.FINISHED, parties.getPartyById(party.id)?.status)
        assertEquals(mapOf(alice.id to 100, bob.id to 20), repository.getRounds(party.id).single().scores)

        parties.updateParty(party)
        val stranger = first.copy(scores = mapOf(alice.id to 1, UUID.randomUUID() to 2))
        assertTrue(runCatching { repository.correctRound(stranger, finished) }.isFailure)
        assertEquals(PartyState.IN_PROGRESS, parties.getPartyById(party.id)?.status)
    }

    @Test
    fun theGameNamesOfTheJeuLibreAreReadOncePerSpellingWithTheirLatestPartie() = runTest {
        parties.createPartyWithParticipants(party.copy(id = UUID.randomUUID(), createdAt = 30), emptyList())
        parties.createPartyWithParticipants(party.copy(id = UUID.randomUUID(), gameName = "Uno", createdAt = 20), emptyList())
        parties.createPartyWithParticipants(Party(UUID.randomUUID(), GameIds.PAPAYOO, PartyState.FINISHED, 0, 40), emptyList())

        val names = repository.observeGameNames().first().sortedBy { it.name }

        assertEquals(listOf(PlayedGameName("Skyjo", 30), PlayedGameName("Uno", 20)), names)
    }

    @Test
    fun deletingThePartieDeletesItsManches() = runTest {
        repository.saveRound(round(1, 1, 2), party)

        parties.deleteParty(party.id)

        assertTrue(repository.getRounds(party.id).isEmpty())
    }
}
