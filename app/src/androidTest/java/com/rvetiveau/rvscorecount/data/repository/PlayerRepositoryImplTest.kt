package com.rvetiveau.rvscorecount.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The suggested names are the Joueurs (Story 10.1) of every Jeu, kept when their Parties are deleted. */
class PlayerRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var parties: PartyRepositoryImpl
    private lateinit var repository: PlayerRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        parties = PartyRepositoryImpl(
            appDatabase = database,
            partyDao = database.partyDao(),
            participantDao = database.participantDao(),
            roundDao = database.roundDao(),
            roundScoreDao = database.roundScoreDao(),
            playerDao = database.playerDao()
        )
        repository = PlayerRepositoryImpl(database, database.playerDao(), database.participantDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createParty(
        gameId: String,
        createdAt: Long,
        vararg names: String,
        status: PartyState = PartyState.FINISHED
    ): UUID {
        val party = Party(UUID.randomUUID(), gameId, status, 0, createdAt)
        parties.createPartyWithParticipants(
            party,
            names.mapIndexed { index, name -> Participant(UUID.randomUUID(), party.id, name, index) }
        )
        return party.id
    }

    @Test
    fun listsEachJoueurOnceWithTheirLatestPartie() = runTest {
        createParty("papayoo", createdAt = 10, "Alice", "Bob", "Chloé")
        createParty("belote", createdAt = 20, "alice", "David")

        val known = repository.observeKnownPlayers().first().sortedBy { it.name }

        assertEquals(
            listOf(KnownPlayer("Alice", 20), KnownPlayer("Bob", 10), KnownPlayer("Chloé", 10), KnownPlayer("David", 20)),
            known
        )
    }

    // --- « Joueurs » (Story 10.2) ---

    @Test
    fun summariesCountFinishedPartiesOnlyButDateTheLatestPartieOfAnyStatus() = runTest {
        createParty("papayoo", createdAt = 10, "Alice", "Bob")
        createParty("belote", createdAt = 20, "Alice", "Chloé")
        createParty("papayoo", createdAt = 30, "Alice", status = PartyState.IN_PROGRESS)
        val deleted = createParty("papayoo", createdAt = 40, "Denis")
        parties.deleteParty(deleted)

        val summaries = repository.observePlayers().first().associate { it.name to (it.finishedParties to it.lastPlayedAt) }

        assertEquals(
            mapOf("Alice" to (2 to 30L), "Bob" to (1 to 10L), "Chloé" to (1 to 20L), "Denis" to (0 to null)),
            summaries
        )
    }

    @Test
    fun renamingAJoueurRenamesEveryOneOfTheirSeats() = runTest {
        val first = createParty("papayoo", createdAt = 10, "Alice", "Bob")
        val second = createParty("papayoo", createdAt = 20, "alice", status = PartyState.IN_PROGRESS)
        val alice = repository.observePlayers().first().single { it.name == "Alice" }

        assertEquals(true, repository.renamePlayer(alice.id, "Alicia"))

        assertEquals(listOf("Alicia", "Bob"), parties.getParticipantsByPartyId(first).map { it.name })
        assertEquals(listOf("Alicia"), parties.getParticipantsByPartyId(second).map { it.name })
        assertEquals(setOf("Alicia", "Bob"), repository.observePlayers().first().map { it.name }.toSet())
        // The new name is the Joueur's key: a new seat named « alicia » is the same Joueur.
        val third = createParty("papayoo", createdAt = 30, "ALICIA")
        assertEquals(2, database.playerDao().getAll().size)
        assertEquals(alice.id.toString(), database.participantDao().getByPartyId(third.toString()).single().playerId)
    }

    // --- Profil (Story 10.3) ---

    @Test
    fun seatsAndTheirJoueursAreReadBack() = runTest {
        val first = createParty("papayoo", createdAt = 10, "Alice", "Bob")
        val second = createParty("belote", createdAt = 20, "alice", "Chloé")
        val alice = repository.observePlayers().first().single { it.name == "Alice" }

        val seats = repository.getSeats(alice.id)

        assertEquals(setOf(first, second), seats.map { it.partyId }.toSet())
        val players = repository.getPlayerIdsBySeat(second)
        assertEquals(alice.id, players.getValue(seats.single { it.partyId == second }.participantId))
        assertEquals(2, players.size)
    }

    @Test
    fun renamingAnUnknownJoueurChangesNothing() = runTest {
        createParty("papayoo", createdAt = 10, "Alice")

        assertEquals(false, repository.renamePlayer(UUID.randomUUID(), "Zoé"))
        assertEquals(listOf("Alice"), database.playerDao().getAll().map { it.name })
    }

    @Test
    fun aJoueurWithoutAnyPartieLeftIsStillKnownWithoutADate() = runTest {
        createParty("papayoo", createdAt = 10, "Alice")
        val deleted = createParty("papayoo", createdAt = 20, "Bob")

        parties.deleteParty(deleted)

        assertEquals(
            listOf(KnownPlayer("Alice", 10), KnownPlayer("Bob", null)),
            repository.observeKnownPlayers().first().sortedBy { it.name }
        )
    }
}
