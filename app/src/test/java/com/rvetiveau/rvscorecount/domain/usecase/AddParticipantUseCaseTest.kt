package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.engine.GameEngine
import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import com.rvetiveau.rvscorecount.game.papayoo.PapayooEngine
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddParticipantUseCaseTest {

    private class FakePartyRepository(
        private val party: Party?,
        private val participants: List<Participant>,
        private val failure: Throwable? = null
    ) : PartyRepository {
        var added: Participant? = null
            private set
        var updatedParty: Party? = null
            private set

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? = party?.takeIf { it.id == id }
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) {
            failure?.let { throw it }
            added = participant
            this.updatedParty = updatedParty
        }
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
    }

    private val partyId = UUID.randomUUID()

    private fun aParty(dealerIndex: Int = 1, status: PartyState = PartyState.IN_PROGRESS) =
        Party(id = partyId, gameId = PapayooEngine.GAME_ID, status = status, currentDealerIndex = dealerIndex, createdAt = 0L)

    private fun players(count: Int) = List(count) { Participant(UUID.randomUUID(), partyId, "P$it", it) }

    private fun useCase(
        repository: PartyRepository,
        engines: Map<String, GameEngine> = mapOf(PapayooEngine.GAME_ID to PapayooEngine())
    ) = AddParticipantUseCase(repository, engines)

    // --- nextDealerIndexAfterInsertion (PRD FR-4) ---

    @Test
    fun `inserting just after the next dealer makes the new participant the next dealer`() {
        assertEquals(2, nextDealerIndexAfterInsertion(currentDealerIndex = 1, insertPosition = 2))
        assertEquals(4, nextDealerIndexAfterInsertion(currentDealerIndex = 3, insertPosition = 4))
    }

    @Test
    fun `inserting at or before the next dealer keeps that dealer, shifted by one seat`() {
        assertEquals(2, nextDealerIndexAfterInsertion(currentDealerIndex = 1, insertPosition = 1))
        assertEquals(2, nextDealerIndexAfterInsertion(currentDealerIndex = 1, insertPosition = 0))
    }

    @Test
    fun `inserting further after the next dealer keeps the dealer index`() {
        assertEquals(1, nextDealerIndexAfterInsertion(currentDealerIndex = 1, insertPosition = 3))
    }

    // --- AddParticipantUseCase ---

    @Test
    fun `inserts the trimmed participant at the chosen position and makes it dealer when right after the dealer`() = runTest {
        val repository = FakePartyRepository(aParty(dealerIndex = 1), players(3))

        val participant = useCase(repository)(partyId, "  Zoé  ", position = 2).getOrThrow()

        assertEquals("Zoé", participant.name)
        assertEquals(2, participant.listPosition)
        assertEquals(participant, repository.added)
        assertEquals(2, repository.updatedParty?.currentDealerIndex)
    }

    @Test
    fun `keeps the same next dealer for any other position`() = runTest {
        val repository = FakePartyRepository(aParty(dealerIndex = 1), players(4))

        useCase(repository)(partyId, "Zoé", position = 4).getOrThrow()

        assertEquals(1, repository.updatedParty?.currentDealerIndex)
    }

    @Test
    fun `rejects a blank name, a full party, a finished party and an out-of-range position without writing`() = runTest {
        val cases = listOf(
            Triple(FakePartyRepository(aParty(), players(3)), "   ", 1),
            Triple(FakePartyRepository(aParty(), players(8)), "Zoé", 1),
            Triple(FakePartyRepository(aParty(status = PartyState.FINISHED), players(3)), "Zoé", 1),
            Triple(FakePartyRepository(aParty(), players(3)), "Zoé", 4),
            Triple(FakePartyRepository(aParty(), players(3)), "Zoé", -1),
            Triple(FakePartyRepository(null, players(3)), "Zoé", 1)
        )

        cases.forEach { (repository, name, position) ->
            val result = useCase(repository)(partyId, name, position)
            assertTrue(result.exceptionOrNull() is InvalidParticipantAdditionException)
            assertNull(repository.added)
        }
    }

    @Test
    fun `rejects an inconsistent dealer index instead of guessing one`() = runTest {
        val repository = FakePartyRepository(aParty(dealerIndex = 5), players(3))

        val result = useCase(repository)(partyId, "Zoé", 1)

        assertTrue(result.exceptionOrNull() is InvalidParticipantAdditionException)
        assertNull(repository.added)
    }

    @Test
    fun `wraps a persistence failure`() = runTest {
        val repository = FakePartyRepository(aParty(), players(3), failure = IllegalStateException("Room write failed"))

        val result = useCase(repository)(partyId, "Zoé", 1)

        assertTrue(result.exceptionOrNull() is ParticipantPersistenceException)
    }
}
