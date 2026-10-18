package com.rvetiveau.rvscorecount.domain.usecase

import com.rvetiveau.rvscorecount.domain.model.Participant
import com.rvetiveau.rvscorecount.domain.model.Party
import com.rvetiveau.rvscorecount.domain.model.PartyState
import com.rvetiveau.rvscorecount.domain.model.Round
import com.rvetiveau.rvscorecount.domain.model.RoundScore
import com.rvetiveau.rvscorecount.domain.repository.PartyRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClosePartyUseCaseTest {

    private class FakePartyRepository(
        private val party: Party?,
        private val failure: Throwable? = null
    ) : PartyRepository {
        var updated: Party? = null
            private set

        override suspend fun createParty(party: Party) = error("not used in this test")
        override suspend fun updateParty(party: Party) {
            failure?.let { throw it }
            updated = party
        }
        override suspend fun createPartyWithParticipants(party: Party, participants: List<Participant>) = error("not used in this test")
        override suspend fun getPartyById(id: UUID): Party? = party?.takeIf { it.id == id }
        override fun observeParty(id: UUID): Flow<Party?> = error("not used in this test")
        override suspend fun getPartyInProgress(): Party? = error("not used in this test")
        override fun observeFinishedParties(): Flow<List<Party>> = error("not used in this test")
        override suspend fun deleteParty(id: UUID) = error("not used in this test")
        override suspend fun addParticipant(participant: Participant, updatedParty: Party) = error("not used in this test")
        override suspend fun getParticipantsByPartyId(partyId: UUID): List<Participant> = error("not used in this test")
        override fun observeParticipants(partyId: UUID): Flow<List<Participant>> = error("not used in this test")
        override suspend fun saveRound(round: Round, scores: Map<UUID, Int>, updatedParty: Party) = error("not used in this test")
        override suspend fun getRoundsByPartyId(partyId: UUID): List<Round> = error("not used in this test")
        override suspend fun getScoresByRoundId(roundId: UUID): List<RoundScore> = error("not used in this test")
    }

    private val partyId = UUID.randomUUID()

    private fun aParty(status: PartyState) =
        Party(id = partyId, gameId = "papayoo", status = status, currentDealerIndex = 2, createdAt = 42L)

    @Test
    fun `finishes an in-progress party and keeps every other field`() = runTest {
        val repository = FakePartyRepository(aParty(PartyState.IN_PROGRESS))

        val result = ClosePartyUseCase(repository)(partyId)

        assertTrue(result.isSuccess)
        assertEquals(aParty(PartyState.FINISHED), repository.updated)
    }

    @Test
    fun `rejects a missing or already finished party without writing`() = runTest {
        listOf(FakePartyRepository(null), FakePartyRepository(aParty(PartyState.FINISHED))).forEach { repository ->
            val result = ClosePartyUseCase(repository)(partyId)

            assertTrue(result.exceptionOrNull() is InvalidPartyClosureException)
            assertNull(repository.updated)
        }
    }

    @Test
    fun `wraps a persistence failure`() = runTest {
        val repository = FakePartyRepository(aParty(PartyState.IN_PROGRESS), IllegalStateException("Room write failed"))

        val result = ClosePartyUseCase(repository)(partyId)

        assertTrue(result.exceptionOrNull() is PartyClosurePersistenceException)
    }
}
