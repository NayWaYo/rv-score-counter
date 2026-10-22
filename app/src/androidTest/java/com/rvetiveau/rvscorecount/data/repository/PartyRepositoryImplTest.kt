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
import kotlinx.coroutines.flow.first
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
            roundScoreDao = database.roundScoreDao(),
            playerDao = database.playerDao()
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
        // The Joueurs created for this Partie roll back with it (Story 10.1).
        assertTrue(database.playerDao().getAll().isEmpty())
    }

    // --- Joueurs (Story 10.1) ---

    private suspend fun playerIdsOf(partyId: UUID): Map<String, String> =
        database.participantDao().getByPartyId(partyId.toString()).associate { it.name to it.playerId }

    @Test
    fun eachNameIsLinkedToOneJoueurReusedFromPartieToPartie() = runTest {
        val first = aParty()
        repository.createPartyWithParticipants(first, listOf(aParticipant(first.id, "Alice", 0), aParticipant(first.id, "Bob", 1)))
        val second = aParty()
        repository.createPartyWithParticipants(second, listOf(aParticipant(second.id, " alice ", 0), aParticipant(second.id, "Chloé", 1)))

        val players = database.playerDao().getAll()
        assertEquals(setOf("Alice", "Bob", "Chloé"), players.map { it.name }.toSet())
        assertEquals(playerIdsOf(first.id).getValue("Alice"), playerIdsOf(second.id).getValue(" alice "))
    }

    @Test
    fun aLateParticipantIsLinkedToTheirJoueur() = runTest {
        val earlier = aParty()
        repository.createPartyWithParticipants(earlier, listOf(aParticipant(earlier.id, "Denis", 0)))
        val party = aParty()
        repository.createPartyWithParticipants(party, listOf(aParticipant(party.id, "Alice", 0)))

        repository.addParticipant(aParticipant(party.id, "DENIS", 1), party)

        assertEquals(playerIdsOf(earlier.id).getValue("Denis"), playerIdsOf(party.id).getValue("DENIS"))
        assertEquals(2, database.playerDao().getAll().size)
    }

    @Test
    fun deletingAPartieKeepsItsJoueurs() = runTest {
        val party = aParty()
        repository.createPartyWithParticipants(party, listOf(aParticipant(party.id, "Alice", 0)))

        repository.deleteParty(party.id)

        assertEquals(listOf("Alice"), database.playerDao().getAll().map { it.name })
    }

    // --- Qui a eu le 40 ---

    @Test
    fun saveRoundPersistsTheSevenHolderAndRejectsAStranger() = runTest {
        val party = aParty()
        val alice = aParticipant(party.id, "Alice", 0)
        repository.createPartyWithParticipants(party, listOf(alice))
        val round = Round(UUID.randomUUID(), party.id, 1, CardSuit.HEART, sevenHolderId = alice.id)

        repository.saveRound(round, mapOf(alice.id to 250), party)

        assertEquals(alice.id, repository.getRoundsByPartyId(party.id).single().sevenHolderId)
        var thrown: Throwable? = null
        try {
            repository.saveRound(round.copy(id = UUID.randomUUID(), roundNumber = 2, sevenHolderId = UUID.randomUUID()), mapOf(alice.id to 250), party)
        } catch (t: Throwable) {
            thrown = t
        }
        assertTrue(thrown is IllegalArgumentException)
        assertEquals(1, repository.getRoundsByPartyId(party.id).size)
    }

    // --- Story 6.1 : historique et suppression ---

    @Test
    fun observeFinishedPartiesListsOnlyFinishedOnesMostRecentFirst() = runTest {
        val old = aParty().copy(status = PartyState.FINISHED, createdAt = 10L)
        val recent = aParty().copy(status = PartyState.FINISHED, createdAt = 20L)
        val inProgress = aParty().copy(createdAt = 30L)
        listOf(old, recent, inProgress).forEach { party ->
            repository.createPartyWithParticipants(party, listOf(aParticipant(party.id, "Alice", 0)))
        }

        assertEquals(listOf(recent.id, old.id), repository.observeFinishedParties().first().map { it.id })
    }

    @Test
    fun deletePartyCascadesToParticipantsRoundsAndScores() = runTest {
        val party = aParty().copy(status = PartyState.FINISHED)
        val alice = aParticipant(party.id, "Alice", 0)
        repository.createPartyWithParticipants(party, listOf(alice))
        val round = Round(UUID.randomUUID(), party.id, 1, CardSuit.CLUB)
        repository.saveRound(round, mapOf(alice.id to 250), party)

        repository.deleteParty(party.id)

        assertNull(repository.getPartyById(party.id))
        assertTrue(repository.getParticipantsByPartyId(party.id).isEmpty())
        assertTrue(repository.getRoundsByPartyId(party.id).isEmpty())
        assertTrue(repository.getScoresByRoundId(round.id).isEmpty())
    }

    // --- Story 4.2 : addParticipant (AC 3, 5) ---

    @Test
    fun addParticipantShiftsLaterSeatsInsertsAndSavesTheDealerTogether() = runTest {
        val party = aParty()
        val alice = aParticipant(party.id, "Alice", 0)
        val bob = aParticipant(party.id, "Bob", 1)
        val chloe = aParticipant(party.id, "Chloé", 2)
        repository.createPartyWithParticipants(party, listOf(alice, bob, chloe))
        val zoe = aParticipant(party.id, "Zoé", 1)

        repository.addParticipant(zoe, party.copy(currentDealerIndex = 1))

        assertEquals(
            listOf("Alice" to 0, "Zoé" to 1, "Bob" to 2, "Chloé" to 3),
            repository.getParticipantsByPartyId(party.id).map { it.name to it.listPosition }
        )
        assertEquals(1, repository.getPartyById(party.id)?.currentDealerIndex)
    }

    @Test
    fun addParticipantRollsBackTheShiftWhenTheInsertFails() = runTest {
        val party = aParty()
        val alice = aParticipant(party.id, "Alice", 0)
        val bob = aParticipant(party.id, "Bob", 1)
        repository.createPartyWithParticipants(party, listOf(alice, bob))
        val duplicate = aParticipant(party.id, "Clone", 0, id = alice.id)

        var thrown: Throwable? = null
        try {
            repository.addParticipant(duplicate, party.copy(currentDealerIndex = 1))
        } catch (t: Throwable) {
            thrown = t
        }

        assertTrue(thrown != null)
        assertEquals(
            listOf("Alice" to 0, "Bob" to 1),
            repository.getParticipantsByPartyId(party.id).map { it.name to it.listPosition }
        )
        assertEquals(0, repository.getPartyById(party.id)?.currentDealerIndex)
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

    // --- Correction d'une Manche validée ---

    @Test
    fun correctRoundRewritesOnlyThatRoundsScoresAndSevenHolder() = runTest {
        val party = aParty()
        val alice = aParticipant(party.id, "Alice", 0)
        val bob = aParticipant(party.id, "Bob", 1)
        repository.createPartyWithParticipants(party, listOf(alice, bob))
        val first = Round(UUID.randomUUID(), party.id, 1, CardSuit.HEART, sevenHolderId = alice.id)
        val second = Round(UUID.randomUUID(), party.id, 2, CardSuit.CLUB, sevenHolderId = bob.id)
        repository.saveRound(first, mapOf(alice.id to 200, bob.id to 50), party.copy(currentDealerIndex = 1))
        repository.saveRound(second, mapOf(alice.id to 100, bob.id to 150), party.copy(currentDealerIndex = 0))

        repository.correctRound(first.copy(sevenHolderId = bob.id), mapOf(alice.id to 20, bob.id to 230))

        assertEquals(listOf(first.copy(sevenHolderId = bob.id), second), repository.getRoundsByPartyId(party.id))
        assertEquals(
            mapOf(alice.id to 20, bob.id to 230),
            repository.getScoresByRoundId(first.id).associate { it.participantId to it.points }
        )
        assertEquals(
            mapOf(alice.id to 100, bob.id to 150),
            repository.getScoresByRoundId(second.id).associate { it.participantId to it.points }
        )
        assertEquals(0, repository.getPartyById(party.id)?.currentDealerIndex)
    }

    @Test
    fun correctRoundChangesNothingWhenItTargetsAPlayerWhoDidNotPlayTheRound() = runTest {
        val party = aParty()
        val alice = aParticipant(party.id, "Alice", 0)
        val bob = aParticipant(party.id, "Bob", 1)
        repository.createPartyWithParticipants(party, listOf(alice, bob))
        val round = Round(UUID.randomUUID(), party.id, 1, CardSuit.HEART, sevenHolderId = alice.id)
        repository.saveRound(round, mapOf(alice.id to 200, bob.id to 50), party.copy(currentDealerIndex = 1))
        val lateComer = aParticipant(party.id, "Zoé", 2)
        repository.addParticipant(lateComer, party.copy(currentDealerIndex = 1))

        val attempts = listOf<suspend () -> Unit>(
            { repository.correctRound(round.copy(sevenHolderId = lateComer.id), mapOf(alice.id to 200, bob.id to 50)) },
            { repository.correctRound(round, mapOf(alice.id to 150, bob.id to 50, lateComer.id to 50)) },
            { repository.correctRound(round.copy(suit = CardSuit.SPADE), mapOf(alice.id to 150, bob.id to 100)) }
        )
        attempts.forEach { attempt ->
            var thrown: Throwable? = null
            try {
                attempt()
            } catch (t: Throwable) {
                thrown = t
            }
            assertTrue(thrown is IllegalArgumentException)
        }

        assertEquals(listOf(round), repository.getRoundsByPartyId(party.id))
        assertEquals(
            mapOf(alice.id to 200, bob.id to 50),
            repository.getScoresByRoundId(round.id).associate { it.participantId to it.points }
        )
    }
}
