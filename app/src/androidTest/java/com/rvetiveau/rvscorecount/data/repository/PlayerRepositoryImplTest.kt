package com.rvetiveau.rvscorecount.data.repository

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.rvetiveau.rvscorecount.data.db.AppDatabase
import com.rvetiveau.rvscorecount.data.entity.ParticipantEntity
import com.rvetiveau.rvscorecount.data.entity.PartyEntity
import com.rvetiveau.rvscorecount.domain.model.KnownPlayer
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The known names query groups real Participant rows across Parties of every Jeu. */
class PlayerRepositoryImplTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PlayerRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = PlayerRepositoryImpl(database.participantDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertParty(gameId: String, createdAt: Long, vararg names: String): String {
        val partyId = UUID.randomUUID().toString()
        database.partyDao().insert(
            PartyEntity(id = partyId, gameId = gameId, status = "FINISHED", currentDealerIndex = 0, createdAt = createdAt)
        )
        database.participantDao().insertAll(
            names.mapIndexed { index, name ->
                ParticipantEntity(id = UUID.randomUUID().toString(), partyId = partyId, name = name, listPosition = index)
            }
        )
        return partyId
    }

    @Test
    fun listsEachExactNameOnceWithItsLatestParty() = runTest {
        insertParty("papayoo", createdAt = 10, "Alice", "Bob", "Chloé")
        insertParty("belote", createdAt = 20, "Alice", "David", "Émile", "alice")

        val known = repository.observeKnownPlayers().first().sortedBy { it.name }

        assertEquals(
            listOf(
                KnownPlayer("Alice", 20),
                KnownPlayer("Bob", 10),
                KnownPlayer("Chloé", 10),
                KnownPlayer("David", 20),
                KnownPlayer("alice", 20),
                KnownPlayer("Émile", 20)
            ),
            known
        )
    }

    @Test
    fun forgetsTheNamesOfADeletedParty() = runTest {
        insertParty("papayoo", createdAt = 10, "Alice")
        val deleted = insertParty("papayoo", createdAt = 20, "Bob")

        database.partyDao().deleteById(deleted)

        assertEquals(listOf(KnownPlayer("Alice", 10)), repository.observeKnownPlayers().first())
    }
}
