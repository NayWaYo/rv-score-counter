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
        repository = PlayerRepositoryImpl(database.playerDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createParty(gameId: String, createdAt: Long, vararg names: String): UUID {
        val party = Party(UUID.randomUUID(), gameId, PartyState.FINISHED, 0, createdAt)
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
