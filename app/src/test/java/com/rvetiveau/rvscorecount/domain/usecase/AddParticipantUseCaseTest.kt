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
        override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
        override suspend fun deleteParty(id: UUID) = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) {
            failure?.let { throw it }
            added = participant
            this.updatedParty = updatedParty
        }
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = participants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun correctRound(round: Round, scores: Map<UUID, Int>) = error("not used in this test")
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
    fun `seated just before the next dealer, the newcomer deals next and the planned dealer right after`() {
        // Table P0 P1 P2 P3, P1 deals next: seat 1 is between P0 (last dealer) and P1.
        assertEquals(1, nextDealerIndexAfterInsertion(currentDealerIndex = 1, insertPosition = 1, participantCount = 4))
        assertEquals(3, nextDealerIndexAfterInsertion(currentDealerIndex = 3, insertPosition = 3, participantCount = 4))
    }

    @Test
    fun `the seat between the last and the first participant comes just before a first-seat dealer`() {
        // P0 deals next: the seat before P0 is after P3 (position 4), or position 0, the same seat at table.
        assertEquals(4, nextDealerIndexAfterInsertion(currentDealerIndex = 0, insertPosition = 4, participantCount = 4))
        assertEquals(0, nextDealerIndexAfterInsertion(currentDealerIndex = 0, insertPosition = 0, participantCount = 4))
    }

    @Test
    fun `any other seat keeps the planned dealer, shifted when seated before him`() {
        // Seated right after the next dealer: the planned dealer keeps the turn (the former rule skipped it).
        assertEquals(1, nextDealerIndexAfterInsertion(currentDealerIndex = 1, insertPosition = 2, participantCount = 4))
        assertEquals(1, nextDealerIndexAfterInsertion(currentDealerIndex = 1, insertPosition = 4, participantCount = 4))
        assertEquals(3, nextDealerIndexAfterInsertion(currentDealerIndex = 2, insertPosition = 1, participantCount = 4))
        assertEquals(0, nextDealerIndexAfterInsertion(currentDealerIndex = 0, insertPosition = 2, participantCount = 4))
    }

    // --- AddParticipantUseCase ---

    @Test
    fun `inserts the trimmed participant at the chosen position and makes it dealer just before the planned one`() = runTest {
        val repository = FakePartyRepository(aParty(dealerIndex = 1), players(3))

        val participant = useCase(repository)(partyId, "  Zoé  ", position = 1).getOrThrow()

        assertEquals("Zoé", participant.name)
        assertEquals(1, participant.listPosition)
        assertEquals(participant, repository.added)
        assertEquals(1, repository.updatedParty?.currentDealerIndex)
    }

    @Test
    fun `keeps the same next dealer for any other position`() = runTest {
        val repository = FakePartyRepository(aParty(dealerIndex = 1), players(4))

        useCase(repository)(partyId, "Zoé", position = 2).getOrThrow()

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
