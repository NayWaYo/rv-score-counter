package com.rvetiveau.rvscorecount.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.domain.model.CardSuit
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Instrumented tests for [PartyRepositoryImpl]'s multi-table writes (aggregate creation, round
 * validation): only a
 * real Room transaction, with a real conflicting insert, can prove the aggregate commits or
 * rolls back atomically — a fake repository cannot discriminate this. */
class PartyRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PartyRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = PartyRepositoryImpl(
            appDatabase = database,
            partyDao = database.partyDao(),
            participantDao = database.participantDao(),
            roundDao = database.roundDao(),
            roundScoreDao = database.roundScoreDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun aParty(id: UUID = UUID.randomUUID()) = Party(
        id = id,
        gameId = "papayoo",
        status = PartyState.IN_PROGRESS,
        currentDealerIndex = 0,
        createdAt = 0L
    )

    private fun aParticipant(partyId: UUID, name: String, position: Int, id: UUID = UUID.randomUUID()) = Participant(
        id = id,
        partyId = partyId,
        name = name,
        listPosition = position
    )

    @Test
    fun commitsThePartyAndAllParticipantsTogetherInASingleTransaction() = runTest {
        val party = aParty()
        val participants = listOf(
            aParticipant(party.id, "Alice", 0),
            aParticipant(party.id, "Bob", 1),
            aParticipant(party.id, "Charlie", 2)
        )

        repository.createPartyWithParticipants(party, participants)

        val persistedParty = repository.getPartyById(party.id)
        assertEquals(PartyState.IN_PROGRESS, persistedParty?.status)
        assertEquals(0, persistedParty?.currentDealerIndex)
        val persistedParticipants = repository.getParticipantsByPartyId(party.id)
        assertEquals(listOf("Alice", "Bob", "Charlie"), persistedParticipants.map { it.name })
        assertEquals(listOf(0, 1, 2), persistedParticipants.map { it.listPosition })
    }

    @Test
    fun rollsBackTheWholeAggregateWhenAParticipantInsertViolatesTheConflictStrategy() = runTest {
        val party = aParty()
        val sharedId = UUID.randomUUID()
        val participants = listOf(
            aParticipant(party.id, "Alice", 0, id = sharedId),
            aParticipant(party.id, "Bob", 1, id = sharedId)
        )

        var thrown: Throwable? = null
        try {
            repository.createPartyWithParticipants(party, participants)
        } catch (t: Throwable) {
            thrown = t
        }

        assertTrue(thrown != null)
        assertNull(repository.getPartyById(party.id))
        assertTrue(repository.getParticipantsByPartyId(party.id).isEmpty())
    }

    // --- Story 3.4 : saveRound (AC 1, 2, 9) ---

    @Test
    fun saveRoundCommitsTheRoundItsScoresAndTheNextDealerTogether() = runTest {
        val party = aParty()
        val alice = aParticipant(party.id, "Alice", 0)
        val bob = aParticipant(party.id, "Bob", 1)
        repository.createPartyWithParticipants(party, listOf(alice, bob))
        val round = Round(UUID.randomUUID(), party.id, 1, CardSuit.HEART)

        repository.saveRound(round, mapOf(alice.id to 200, bob.id to 50), party.copy(currentDealerIndex = 1))

        assertEquals(listOf(round), repository.getRoundsByPartyId(party.id))
        assertEquals(
            mapOf(alice.id to 200, bob.id to 50),
            repository.getScoresByRoundId(round.id).associate { it.participantId to it.points }
        )
        assertEquals(1, repository.getPartyById(party.id)?.currentDealerIndex)
    }

    @Test
    fun saveRoundRollsBackEverythingWhenTheRoundNumberIsAlreadyTaken() = runTest {
        val party = aParty()
        val alice = aParticipant(party.id, "Alice", 0)
        val bob = aParticipant(party.id, "Bob", 1)
        repository.createPartyWithParticipants(party, listOf(alice, bob))
        val first = Round(UUID.randomUUID(), party.id, 1, CardSuit.HEART)
        repository.saveRound(first, mapOf(alice.id to 200, bob.id to 50), party.copy(currentDealerIndex = 1))
        val duplicate = Round(UUID.randomUUID(), party.id, 1, CardSuit.SPADE)

        var thrown: Throwable? = null
        try {
            repository.saveRound(duplicate, mapOf(alice.id to 0, bob.id to 250), party.copy(currentDealerIndex = 0))
        } catch (t: Throwable) {
            thrown = t
        }

        assertTrue(thrown != null)
        assertEquals(listOf(first), repository.getRoundsByPartyId(party.id))
        assertTrue(repository.getScoresByRoundId(duplicate.id).isEmpty())
        assertEquals(1, repository.getPartyById(party.id)?.currentDealerIndex)
    }
}
