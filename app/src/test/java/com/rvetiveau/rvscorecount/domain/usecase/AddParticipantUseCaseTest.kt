package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AddParticipantUseCaseTest {

    private class FakePartyRepository(
        private val existingParticipants: List<Participant> = emptyList(),
        private val failure: Throwable? = null
    ) : PartyRepository {
        var added: Participant? = null

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) = error("not used in this test")
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? = error("not used in this test")
        override fun observeParty(id: UUID): Flow<Party?> = flowOf(null)
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override suspend fun addParticipant(participant: Participant) {
            failure?.let { throw it }
            added = participant
        }
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = existingParticipants
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = flowOf(emptyList())
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
    }

    @Test
    fun `adds a participant at the next continuous listPosition`() = runTest {
        val partyId = UUID.randomUUID()
        val existing = listOf(
            Participant(id = UUID.randomUUID(), partyId = partyId, name = "Alice", listPosition = 0),
            Participant(id = UUID.randomUUID(), partyId = partyId, name = "Bob", listPosition = 1)
        )
        val repository = FakePartyRepository(existingParticipants = existing)
        val useCase = AddParticipantUseCase(repository)

        val result = useCase(partyId, "  Charlie  ")

        assertTrue(result.isSuccess)
        val participant = result.getOrThrow()
        assertEquals("Charlie", participant.name)
        assertEquals(2, participant.listPosition)
        assertEquals(partyId, participant.partyId)
        assertEquals(participant, repository.added)
    }

    @Test
    fun `fails on a blank name without writing to the repository`() = runTest {
        val partyId = UUID.randomUUID()
        val repository = FakePartyRepository()
        val useCase = AddParticipantUseCase(repository)

        val result = useCase(partyId, "   ")

        assertTrue(result.isFailure)
        assertNull(repository.added)
    }

    @Test
    fun `propagates a persistence failure as a Result failure instead of throwing`() = runTest {
        val partyId = UUID.randomUUID()
        val failure = IllegalStateException("Room write failed")
        val repository = FakePartyRepository(failure = failure)
        val useCase = AddParticipantUseCase(repository)

        val result = useCase(partyId, "Alice")

        assertTrue(result.isFailure)
        assertSame(failure, result.exceptionOrNull())
    }
}
