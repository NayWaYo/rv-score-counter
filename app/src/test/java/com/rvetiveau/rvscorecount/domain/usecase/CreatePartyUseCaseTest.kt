package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.engine.RoundValidation
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.ParticipantCreationData
import com.rvetiveau.rvscorecount.domain.model.ParticipantId
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatePartyUseCaseTest {

    private class FakeGameEngine(
        override val gameId: String,
        override val minPlayers: Int = 3,
        override val maxPlayers: Int = 8
    ) : GameEngine {
        override val totalPointsPerRound: Int = 250
        override fun precomputeLastScore(entered: Map<ParticipantId, Int>): Int = 0
        override fun validateRound(scores: Map<ParticipantId, Int>): RoundValidation = RoundValidation.Success
    }

    private class FakePartyRepository(
        private val failure: Throwable? = null,
        private val referenceParty: Party? = null,
        private val referenceParticipants: List<Participant> = emptyList()
    ) : PartyRepository {
        var createdParty: Party? = null
        var createdParticipants: List<Participant>? = null

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) {
            failure?.let { throw it }
            createdParty = party
            createdParticipants = participants
        }
        override suspend fun getPartyById(id: UUID): Party? = referenceParty?.takeIf { it.id == id }
        override fun observeParty(id: UUID): Flow<Party?> = flowOf(null)
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
        override suspend fun deleteParty(id: UUID) = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> =
            referenceParticipants.filter { it.partyId == partyId }
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = flowOf(emptyList())
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
    }

    private fun draft(name: String) = ParticipantCreationData(id = UUID.randomUUID(), name = name)

    // --- Story 5.2 : fromParty (relance rapide) ---

    private val referenceId = UUID.randomUUID()

    private fun referenceParty(status: PartyState = PartyState.FINISHED) =
        Party(id = referenceId, gameId = "papayoo", status = status, currentDealerIndex = 2, createdAt = 1L)

    // Stored out of order on purpose: the replay must follow listPosition, not storage order.
    private val referenceParticipants = listOf(
        Participant(UUID.randomUUID(), referenceId, "Chloé", 2),
        Participant(UUID.randomUUID(), referenceId, "Alice", 0),
        Participant(UUID.randomUUID(), referenceId, "Bob", 1)
    )

    @Test
    fun `fromParty recreates the same game with the same names in table order under new ids`() = runTest {
        val repository = FakePartyRepository(referenceParty = referenceParty(), referenceParticipants = referenceParticipants)
        val useCase = CreatePartyUseCase(repository, mapOf("papayoo" to FakeGameEngine("papayoo")))

        val newPartyId = useCase.fromParty(referenceId).getOrThrow()

        val party = repository.createdParty!!
        assertEquals(newPartyId, party.id)
        assertTrue(newPartyId != referenceId)
        assertEquals("papayoo", party.gameId)
        assertEquals(PartyState.IN_PROGRESS, party.status)
        assertEquals(0, party.currentDealerIndex)
        val created = repository.createdParticipants!!
        assertEquals(listOf("Alice", "Bob", "Chloé"), created.map { it.name })
        assertEquals(listOf(0, 1, 2), created.map { it.listPosition })
        assertTrue(created.none { participant -> referenceParticipants.any { it.id == participant.id } })
    }

    @Test
    fun `fromParty only accepts a finished reference party`() = runTest {
        listOf(
            FakePartyRepository(referenceParty = null, referenceParticipants = referenceParticipants),
            FakePartyRepository(referenceParty = referenceParty(PartyState.IN_PROGRESS), referenceParticipants = referenceParticipants),
            FakePartyRepository(referenceParty = referenceParty(), referenceParticipants = emptyList())
        ).forEach { repository ->
            val useCase = CreatePartyUseCase(repository, mapOf("papayoo" to FakeGameEngine("papayoo")))

            val result = useCase.fromParty(referenceId)

            assertTrue(result.exceptionOrNull() is InvalidPartyConfigurationException)
            assertEquals(null, repository.createdParty)
        }
    }

    @Test
    fun `creates the party with exact gameId, generated id, IN_PROGRESS status and dealer index 0`() = runTest {
        val repository = FakePartyRepository()
        val useCase = CreatePartyUseCase(repository, mapOf("papayoo" to FakeGameEngine("papayoo")))

        val result = useCase("papayoo", listOf(draft("Alice"), draft("Bob"), draft("Charlie")))

        assertTrue(result.isSuccess)
        val party = repository.createdParty!!
        assertEquals("papayoo", party.gameId)
        assertEquals(PartyState.IN_PROGRESS, party.status)
        assertEquals(0, party.currentDealerIndex)
        assertEquals(party.id, result.getOrThrow())
    }

    @Test
    fun `persists participants with continuous listPosition matching the input order`() = runTest {
        val repository = FakePartyRepository()
        val useCase = CreatePartyUseCase(repository, mapOf("papayoo" to FakeGameEngine("papayoo")))
        val alice = draft("Alice")
        val bob = draft("Bob")
        val charlie = draft("Charlie")

        useCase("papayoo", listOf(alice, bob, charlie))

        val persisted = repository.createdParticipants!!
        assertEquals(listOf(0, 1, 2), persisted.map { it.listPosition })
        assertEquals(listOf(alice.id, bob.id, charlie.id), persisted.map { it.id })
        assertEquals(listOf("Alice", "Bob", "Charlie"), persisted.map { it.name })
        assertTrue(persisted.all { it.partyId == repository.createdParty!!.id })
    }

    @Test
    fun `honors engine limits different from 3 and 8`() = runTest {
        val repository = FakePartyRepository()
        val engine = FakeGameEngine("belote", minPlayers = 2, maxPlayers = 4)
        val useCase = CreatePartyUseCase(repository, mapOf("belote" to engine))

        val result = useCase("belote", listOf(draft("Alice"), draft("Bob")))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `fails when the participant count is below the engine minimum`() = runTest {
        val repository = FakePartyRepository()
        val engine = FakeGameEngine("belote", minPlayers = 2, maxPlayers = 4)
        val useCase = CreatePartyUseCase(repository, mapOf("belote" to engine))

        val result = useCase("belote", listOf(draft("Alice")))

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when the participant count is above the engine maximum`() = runTest {
        val repository = FakePartyRepository()
        val engine = FakeGameEngine("belote", minPlayers = 2, maxPlayers = 3)
        val useCase = CreatePartyUseCase(repository, mapOf("belote" to engine))

        val result = useCase("belote", listOf(draft("Alice"), draft("Bob"), draft("Charlie"), draft("Dora")))

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails on a blank participant name`() = runTest {
        val repository = FakePartyRepository()
        val engine = FakeGameEngine("papayoo", minPlayers = 1, maxPlayers = 8)
        val useCase = CreatePartyUseCase(repository, mapOf("papayoo" to engine))

        val result = useCase("papayoo", listOf(draft("Alice"), ParticipantCreationData(id = UUID.randomUUID(), name = "   ")))

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when the requested gameId is not registered`() = runTest {
        val repository = FakePartyRepository()
        val useCase = CreatePartyUseCase(repository, mapOf("belote" to FakeGameEngine("belote")))

        val result = useCase("papayoo", listOf(draft("Alice"), draft("Bob"), draft("Charlie")))

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails on duplicate participant ids`() = runTest {
        val repository = FakePartyRepository()
        val engine = FakeGameEngine("papayoo", minPlayers = 1, maxPlayers = 8)
        val useCase = CreatePartyUseCase(repository, mapOf("papayoo" to engine))
        val sharedId = UUID.randomUUID()

        val result = useCase(
            "papayoo",
            listOf(ParticipantCreationData(id = sharedId, name = "Alice"), ParticipantCreationData(id = sharedId, name = "Bob"))
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `wraps a persistence failure distinctly from invalid input`() = runTest {
        val failure = IllegalStateException("Room write failed")
        val repository = FakePartyRepository(failure = failure)
        val useCase = CreatePartyUseCase(repository, mapOf("papayoo" to FakeGameEngine("papayoo")))

        val result = useCase("papayoo", listOf(draft("Alice"), draft("Bob"), draft("Charlie")))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is PartyCreationPersistenceException)
        assertSame(failure, exception?.cause)
    }
}
